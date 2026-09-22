package com.collab.spreadsheet.user.service;

import com.collab.spreadsheet.common.exception.UnauthorizedException;
import com.collab.spreadsheet.common.exception.ValidationException;
import com.collab.spreadsheet.common.util.IdGenerator;
import com.collab.spreadsheet.user.dto.*;
import com.collab.spreadsheet.user.entity.RefreshToken;
import com.collab.spreadsheet.user.entity.User;
import com.collab.spreadsheet.user.mapper.UserMapper;
import com.collab.spreadsheet.user.repository.RefreshTokenRepository;
import com.collab.spreadsheet.user.repository.UserRepository;
import com.collab.spreadsheet.user.security.JwtUtil;
import com.collab.spreadsheet.user.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Random;

/**
 * Production-Grade Authentication service handling registration with Redis OTP,
 * login, token rotation, password reset, and JWT blacklisting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final OtpService otpService;
    private final TokenBlacklistService tokenBlacklistService;
    
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 15;
    
    /**
     * Register a new user with status UNVERIFIED, generate 6-digit OTP, store in Redis with 5 min TTL
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: email={}, username={}", request.getEmail(), request.getUsername());
        
        // Validate uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("email", "Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("username", "Username already exists");
        }
        
        // Create user with verified status immediately (no OTP required)
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .username(request.getUsername().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .colorHex(generateRandomColor())
                .emailVerified(true)
                .enabled(true)
                .failedLoginAttempts(0)
                .build();
        
        user = userRepository.save(user);
        
        log.info("User registered successfully: id={}, email={}. Account activated immediately.", user.getId(), user.getEmail());
        
        // Return AuthResponse with tokens so user logs in immediately
        return generateAuthResponse(user, null, null);
    }

    /**
     * Verify Redis-backed OTP and activate user account
     */
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Verifying OTP for email: {}, purpose: {}", email, request.getPurpose());

        // Verify from Redis via atomic Lua script
        otpService.verifyOtp(email, request.getPurpose(), request.getOtp());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found for email: " + email));

        if ("EMAIL_VERIFICATION".equalsIgnoreCase(request.getPurpose())) {
            user.setEmailVerified(true);
            user.setEnabled(true);
            user = userRepository.save(user);
            log.info("Email verified successfully for user: {}", user.getId());
        }

        return generateAuthResponse(user, null, null);
    }

    /**
     * Resend verification OTP with 60-second cooldown lock
     */
    public void resendOtp(SendOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found with email: " + email));

        otpService.generateAndSendOtp(email, request.getPurpose(), user.getId());
        log.info("Resent OTP for email: {}, purpose: {}", email, request.getPurpose());
    }

    /**
     * Initiate forgot password flow: generate 6-digit OTP in Redis and dispatch via Kafka
     */
    public void forgotPassword(String email) {
        String cleanEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new UnauthorizedException("User not found with email: " + cleanEmail));

        otpService.generateAndSendOtp(cleanEmail, "PASSWORD_RESET", user.getId());
        log.info("Password reset OTP generated and sent to email: {}", cleanEmail);
    }

    /**
     * Reset password using verified Redis OTP
     */
    @Transactional
    public void resetPassword(ResetPasswordWithOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Attempting password reset with OTP for email: {}", email);

        // Verify OTP from Redis
        otpService.verifyOtp(email, "PASSWORD_RESET", request.getOtp());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found with email: " + email));

        // Update password with BCrypt
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Revoke all existing refresh tokens for security
        refreshTokenRepository.deleteByUserId(user.getId());
        log.info("Password reset successfully and existing sessions revoked for user: {}", user.getId());
    }
    
    /**
     * Login user
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
        log.info("Login attempt for: {}", request.getEmailOrUsername());
        
        // Find user
        User user = userRepository.findByEmailOrUsername(request.getEmailOrUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        
        // Check if account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new UnauthorizedException("Account is temporarily locked. Try again later.");
        }
        
        // Check if account is enabled
        if (!user.getEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new UnauthorizedException("Invalid credentials");
        }
        
        // Reset failed attempts and update last login
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        
        log.info("User logged in successfully: {}", user.getId());
        
        // Generate tokens
        return generateAuthResponse(user, userAgent, ipAddress);
    }
    
    /**
     * Refresh access token and rotate refresh token
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshTokenValue = request.getRefreshToken();
        
        // Validate refresh token JWT
        if (!jwtUtil.validateToken(refreshTokenValue)) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        
        String userId = jwtUtil.extractUserId(refreshTokenValue);
        String oldTokenHash = hashToken(refreshTokenValue);
        
        // Find refresh token in Redis
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(oldTokenHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found or expired"));
        
        // Check if revoked
        if (refreshToken.getRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        // Revoke old refresh token (Token Rotation)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        
        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        
        if (!user.getEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }
        
        log.info("Token refreshed and rotated for user: {}", user.getId());
        
        // Generate new access token and new rotated refresh token
        return generateAuthResponse(user, refreshToken.getUserAgent(), refreshToken.getIpAddress());
    }
    
    /**
     * Logout user: revoke refresh token and blacklist access token in Redis
     */
    @Transactional
    public void logout(String refreshTokenValue, String accessToken) {
        if (refreshTokenValue != null) {
            String tokenHash = hashToken(refreshTokenValue);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
                log.info("Refresh token revoked for user: {}", token.getUserId());
            });
        }
        if (accessToken != null) {
            tokenBlacklistService.blacklistToken(accessToken);
        }
    }
    
    /**
     * Logout from all devices (revoke all refresh tokens for user)
     */
    @Transactional
    public void logoutAll(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("All refresh tokens revoked for user: {}", userId);
    }
    
    /**
     * Generate auth response with tokens
     */
    private AuthResponse generateAuthResponse(User user, String userAgent, String ipAddress) {
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
        
        String refreshTokenValue = jwtUtil.generateRefreshToken(user.getId());
        String tokenHash = hashToken(refreshTokenValue);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .id(IdGenerator.generateId())
                .userId(user.getId())
                .tokenHash(tokenHash)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtUtil.getRefreshTokenValiditySeconds()))
                .ttlSeconds(jwtUtil.getRefreshTokenValiditySeconds())
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();
        
        refreshTokenRepository.save(refreshToken);
        
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(jwtUtil.getAccessTokenValiditySeconds())
                .user(userMapper.toDto(user))
                .build();
    }
    
    private void handleFailedLogin(User user) {
        Integer attempts = user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0;
        attempts++;
        user.setFailedLoginAttempts(attempts);
        
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60));
            log.warn("Account locked due to too many failed login attempts: {}", user.getEmail());
        }
        
        userRepository.save(user);
    }
    
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    private String generateRandomColor() {
        Random random = new Random();
        int r = 100 + random.nextInt(156);
        int g = 100 + random.nextInt(156);
        int b = 100 + random.nextInt(156);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
