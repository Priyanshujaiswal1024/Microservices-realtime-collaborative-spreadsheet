package com.collab.spreadsheet.common.exception;

/**
 * Exception thrown for validation errors
 */
public class ValidationException extends BaseException {
    
    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR", 400);
    }

    public ValidationException(String field, String reason) {
        super(
            String.format("Validation failed for field '%s': %s", field, reason),
            "VALIDATION_ERROR",
            400
        );
    }
}
