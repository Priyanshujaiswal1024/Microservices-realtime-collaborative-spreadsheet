package com.collab.spreadsheet.common.exception;

/**
 * Exception thrown when user lacks permission to perform an action
 */
public class ForbiddenException extends BaseException {
    
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", 403);
    }

    public ForbiddenException(String resource, String action) {
        super(
            String.format("You don't have permission to %s %s", action, resource),
            "FORBIDDEN",
            403
        );
    }

    public ForbiddenException() {
        super("Access denied", "FORBIDDEN", 403);
    }
}
