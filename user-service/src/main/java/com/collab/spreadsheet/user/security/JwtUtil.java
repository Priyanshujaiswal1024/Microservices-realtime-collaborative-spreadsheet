package com.collab.spreadsheet.user.security;

import com.collab.spreadsheet.common.dto.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT utility for token generation and validation with JTI support
 */
@Component
@Slf4j
public class JwtUtil {
    
    private final SecretKey secretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;
    
    public JwtUtil(
            @Value("${jwt.secret:collab-spreadsheet-super-secret-jwt-key-minimum-256-bits-long-key-string}") String secret,
            @Value("${jwt.access-token-validity:900000}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity:604800000}") long refreshTokenValidity) {
        
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }
    
    /**
     * Generate access token with unique JTI ID
     */
    public String generateAccessToken(String userId, String username, String email, UserRole role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("email", email);
        claims.put("role", role.name());
        
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(accessTokenValidity);
        String jti = UUID.randomUUID().toString();
        
        return Jwts.builder()
                .claims(claims)
                .id(jti)
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }
    
    /**
     * Generate refresh token
     */
    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(refreshTokenValidity);
        
        return Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }
    
    /**
     * Extract unique token identifier (JTI)
     */
    public String extractJti(String token) {
        return extractClaims(token).getId();
    }

    /**
     * Extract user ID from token
     */
    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }
    
    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        return extractClaims(token).get("username", String.class);
    }
    
    /**
     * Extract email from token
     */
    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }
    
    /**
     * Extract role from token
     */
    public UserRole extractRole(String token) {
        String roleStr = extractClaims(token).get("role", String.class);
        return roleStr != null ? UserRole.valueOf(roleStr) : UserRole.USER;
    }
    
    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }
    
    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if token is expired
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * Extract all claims from token
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    public long getAccessTokenValiditySeconds() {
        return accessTokenValidity / 1000;
    }
    
    public long getRefreshTokenValiditySeconds() {
        return refreshTokenValidity / 1000;
    }
}
