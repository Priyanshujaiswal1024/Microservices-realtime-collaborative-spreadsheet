package com.collab.spreadsheet.common.exception;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends BaseException {
    
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(
            String.format("%s with ID '%s' not found", resourceType, resourceId),
            "RESOURCE_NOT_FOUND",
            404
        );
    }

    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", 404);
    }
}
