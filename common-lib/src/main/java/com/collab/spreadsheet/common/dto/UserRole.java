package com.collab.spreadsheet.common.dto;

/**
 * System-wide user roles
 */
public enum UserRole {
    /**
     * Regular user
     */
    USER,
    
    /**
     * Administrator with elevated privileges
     */
    ADMIN,
    
    /**
     * Service account for inter-service communication
     */
    SERVICE
}
