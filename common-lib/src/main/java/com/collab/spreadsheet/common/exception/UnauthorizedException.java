package com.collab.spreadsheet.common.exception;

/**
 * Exception thrown when user is not authenticated
 */
public class UnauthorizedException extends BaseException {
    
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", 401);
    }

    public UnauthorizedException() {
        super("Authentication required", "UNAUTHORIZED", 401);
    }
}
