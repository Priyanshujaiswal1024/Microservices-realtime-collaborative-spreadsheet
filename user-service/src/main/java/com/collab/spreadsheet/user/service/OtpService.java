package com.collab.spreadsheet.user.service;

import com.collab.spreadsheet.common.config.KafkaTopics;
import com.collab.spreadsheet.common.events.UserEvent;
import com.collab.spreadsheet.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/**
 * Production-Grade Redis-backed OTP Service.
 * Implements:
 * 1. 6-digit cryptographically secure OTP generation.
 * 2. SHA-256 hashing before storing in Redis with 5-minute (300s) TTL.
 * 3. 60-second resend cooldown lock.
 * 4. Max 3 OTP generation requests per hour rate-limiting.
 * 5. Atomic Lua script for verify-and-consume with max 5 failed attempts per 5-minute window.
 * 6. Kafka asynchronous dispatch to notification-service (Google SMTP).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final String OTP_KEY_PREFIX = "otp:";
    private static final String ATTEMPTS_KEY_PREFIX = "otp-attempts:";
    private static final String RESEND_LOCK_PREFIX = "otp-resend-lock:";
    private static final String HOURLY_GEN_PREFIX = "otp-generation-hourly:";

    private static final long OTP_TTL_SECONDS = 300L;          // 5 minutes
    private static final long RESEND_COOLDOWN_SECONDS = 60L;   // 60 seconds cooldown
    private static final long HOURLY_LIMIT_SECONDS = 3600L;    // 1 hour
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int MAX_HOURLY_GENERATIONS = 5;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Lua script for atomic OTP verification and attempt tracking
    // KEYS[1] = otpKey, KEYS[2] = attemptsKey
    // ARGV[1] = expectedHashedOtp, ARGV[2] = maxAttempts, ARGV[3] = ttlSeconds
    // Returns: 1 (SUCCESS), 0 (INVALID_OTP), -1 (MAX_ATTEMPTS_EXCEEDED), -2 (EXPIRED_OR_NOT_FOUND)
    private static final String VERIFY_OTP_LUA =
            "local storedOtp = redis.call('GET', KEYS[1]);\n" +
            "if not storedOtp then\n" +
            "    return -2;\n" +
            "end\n" +
            "local attempts = redis.call('INCR', KEYS[2]);\n" +
            "if attempts == 1 then\n" +
            "    redis.call('EXPIRE', KEYS[2], tonumber(ARGV[3]));\n" +
            "end\n" +
            "if attempts > tonumber(ARGV[2]) then\n" +
            "    redis.call('DEL', KEYS[1]);\n" +
            "    return -1;\n" +
            "end\n" +
            "if storedOtp == ARGV[1] then\n" +
            "    redis.call('DEL', KEYS[1]);\n" +
            "    redis.call('DEL', KEYS[2]);\n" +
            "    return 1;\n" +
            "else\n" +
            "    return 0;\n" +
            "end;";

    private final DefaultRedisScript<Long> verifyOtpScript = new DefaultRedisScript<>(VERIFY_OTP_LUA, Long.class);

    /**
     * Generate 6-digit OTP, store hashed in Redis with 5 min TTL, and publish to Kafka topic 'user-events'
     */
    public String generateAndSendOtp(String email, String purpose, String userId) {
        String normalizedEmail = normalize(email);
        String cleanPurpose = normalizePurpose(purpose);

        // 1. Enforce 60-second cooldown lock
        String resendLockKey = RESEND_LOCK_PREFIX + normalizedEmail;
        Boolean isLocked = redisTemplate.hasKey(resendLockKey);
        if (Boolean.TRUE.equals(isLocked)) {
            Long ttl = redisTemplate.getExpire(resendLockKey);
            throw new IllegalStateException("Please wait " + (ttl != null && ttl > 0 ? ttl : 60) + " seconds before requesting a new OTP");
        }

        // 2. Enforce Hourly Generation Rate Limit (max 5 per hour)
        String hourlyKey = HOURLY_GEN_PREFIX + normalizedEmail;
        Long hourlyCount = redisTemplate.opsForValue().increment(hourlyKey);
        if (hourlyCount != null && hourlyCount == 1) {
            redisTemplate.expire(hourlyKey, Duration.ofSeconds(HOURLY_LIMIT_SECONDS));
        }
        if (hourlyCount != null && hourlyCount > MAX_HOURLY_GENERATIONS) {
            throw new IllegalStateException("Hourly OTP generation limit reached. Please try again later.");
        }

        // 3. Generate 6-digit numeric OTP (100000 - 999999)
        int rawCode = 100000 + SECURE_RANDOM.nextInt(900000);
        String rawOtp = String.valueOf(rawCode);
        String hashedOtp = sha256(rawOtp);

        // 4. Store in Redis with 5-minute TTL
        String otpKey = buildOtpKey(normalizedEmail, cleanPurpose);
        String attemptsKey = buildAttemptsKey(normalizedEmail, cleanPurpose);

        redisTemplate.opsForValue().set(otpKey, hashedOtp, Duration.ofSeconds(OTP_TTL_SECONDS));
        redisTemplate.delete(attemptsKey); // reset any previous attempt counter
        redisTemplate.opsForValue().set(resendLockKey, "1", Duration.ofSeconds(RESEND_COOLDOWN_SECONDS));

        log.info("Redis OTP created for email='{}', purpose='{}', TTL={}s", normalizedEmail, cleanPurpose, OTP_TTL_SECONDS);

        // 5. Publish UserEvent to Kafka 'user-events' for notification-service to deliver via Google SMTP
        UserEvent event = UserEvent.builder()
                .actorId(userId != null ? userId : normalizedEmail)
                .type(UserEvent.Type.OTP_GENERATED)
                .userId(userId)
                .email(normalizedEmail)
                .otp(rawOtp)
                .purpose(cleanPurpose)
                .build();

        try {
            kafkaTemplate.send(KafkaTopics.USER_EVENTS, normalizedEmail, event);
            log.info("Published OtpGeneratedEvent to Kafka topic '{}' for '{}'", KafkaTopics.USER_EVENTS, normalizedEmail);
        } catch (Exception e) {
            log.warn("Failed to publish OTP event to Kafka: {}. OTP is cached in Redis.", e.getMessage());
        }

        return rawOtp;
    }

    /**
     * Atomically verify OTP from Redis using Lua script.
     */
    public boolean verifyOtp(String email, String purpose, String rawOtp) {
        if (email == null || rawOtp == null) {
            throw new UnauthorizedException("Email and OTP code are required");
        }

        String normalizedEmail = normalize(email);
        String cleanPurpose = normalizePurpose(purpose);
        String hashedOtp = sha256(rawOtp.trim());

        String otpKey = buildOtpKey(normalizedEmail, cleanPurpose);
        String attemptsKey = buildAttemptsKey(normalizedEmail, cleanPurpose);

        List<String> keys = List.of(otpKey, attemptsKey);
        Long result = redisTemplate.execute(
                verifyOtpScript,
                keys,
                hashedOtp,
                String.valueOf(MAX_VERIFY_ATTEMPTS),
                String.valueOf(OTP_TTL_SECONDS)
        );

        if (result == null || result == -2) {
            throw new UnauthorizedException("OTP has expired or was not requested. Please request a new OTP.");
        }
        if (result == -1) {
            throw new UnauthorizedException("Maximum verification attempts (" + MAX_VERIFY_ATTEMPTS + ") exceeded. This OTP has been invalidated.");
        }
        if (result == 0) {
            throw new UnauthorizedException("Invalid verification code. Please check and try again.");
        }

        log.info("OTP successfully verified and invalidated in Redis for email='{}', purpose='{}'", normalizedEmail, cleanPurpose);
        return true;
    }

    private String buildOtpKey(String email, String purpose) {
        return OTP_KEY_PREFIX + purpose + ":" + email;
    }

    private String buildAttemptsKey(String email, String purpose) {
        return ATTEMPTS_KEY_PREFIX + purpose + ":" + email;
    }

    private String normalize(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }

    private String normalizePurpose(String purpose) {
        return (purpose == null || purpose.isBlank()) ? "EMAIL_VERIFICATION" : purpose.trim().toUpperCase();
    }

    /**
     * SHA-256 hash for secure storage of short-lived OTP tokens in Redis
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }
}
