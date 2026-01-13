package io.cheshire.security.exception;

/**
 * Base exception for all security-related errors.
 */
public class SecurityException extends RuntimeException {

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}

