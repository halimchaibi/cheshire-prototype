package io.cheshire.security.auth;

import io.cheshire.security.exception.SecurityException;

/**
 * Exception thrown when authentication fails.
 */
public class AuthenticationException extends SecurityException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

