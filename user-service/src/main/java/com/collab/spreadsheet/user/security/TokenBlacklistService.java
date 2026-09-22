package com.collab.spreadsheet.user.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    /**
     * Blacklist an access token in Redis until its natural expiration
     */
    public void blacklistToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            String jti = jwtUtil.extractJti(token);
            Date expiration = jwtUtil.extractExpiration(token);
            long remainingMillis = expiration.getTime() - System.currentTimeMillis();

            if (remainingMillis > 0 && jti != null) {
                String key = BLACKLIST_PREFIX + jti;
                redisTemplate.opsForValue().set(key, "1", Duration.ofMillis(remainingMillis));
                log.info("Blacklisted JWT JTI '{}' for {} ms in Redis", jti, remainingMillis);
            }
        } catch (Exception e) {
            log.warn("Failed to blacklist JWT token: {}", e.getMessage());
        }
    }

    /**
     * Check if a token's JTI has been blacklisted
     */
    public boolean isBlacklisted(String token) {
        try {
            String jti = jwtUtil.extractJti(token);
            if (jti == null) {
                return false;
            }
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
        } catch (Exception e) {
            return false;
        }
    }
}
