package com.hrchatbot.exception;

/**
 * Exception thrown when user tries to access a resource they don't have permission for
 */
public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
    
    public UnauthorizedAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
