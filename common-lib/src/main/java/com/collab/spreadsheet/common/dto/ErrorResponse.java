package com.collab.spreadsheet.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard error response DTO for REST APIs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    
    /**
     * HTTP status code
     */
    private int status;
    
    /**
     * Error code (for client-side handling)
     */
    private String code;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * Request path that caused the error
     */
    private String path;
    
    /**
     * Timestamp when error occurred
     */
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    /**
     * Optional trace ID for debugging
     */
    private String traceId;
}
