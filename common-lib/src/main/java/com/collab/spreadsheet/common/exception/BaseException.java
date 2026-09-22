package com.collab.spreadsheet.common.exception;

import lombok.Getter;

/**
 * Base exception for all custom exceptions in the system
 */
@Getter
public abstract class BaseException extends RuntimeException {
    
    private final String errorCode;
    private final int httpStatus;

    protected BaseException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected BaseException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
