package com.collab.spreadsheet.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.Instant;

/**
 * Refresh Token stored in Redis
 * 
 * TTL is automatically managed by Redis based on @TimeToLive
 */
@RedisHash("refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    
    @Id
    private String id;  // Token ID (UUID)
    
    @Indexed
    private String userId;
    
    @Indexed
    private String tokenHash;  // SHA-256 hash of the actual token
    
    private Instant issuedAt;
    
    private Instant expiresAt;
    
    @TimeToLive
    private Long ttlSeconds;  // Automatically expires from Redis
    
    private String userAgent;
    
    private String ipAddress;
    
    @Builder.Default
    private Boolean revoked = false;
}
