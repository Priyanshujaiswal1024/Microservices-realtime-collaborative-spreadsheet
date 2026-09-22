package com.collab.spreadsheet.user.dto;

import com.collab.spreadsheet.common.dto.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * User DTO (response)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    
    private String id;
    
    private String email;
    
    private String username;
    
    private String fullName;
    
    private UserRole role;
    
    private Boolean enabled;
    
    private Boolean emailVerified;
    
    private String colorHex;
    
    private Instant createdAt;
    
    private Instant lastLoginAt;
}
