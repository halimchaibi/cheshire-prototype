package io.cheshire.security.authz;

import io.cheshire.security.exception.SecurityException;

/**
 * Exception thrown when authorization fails (access denied).
 */
public class AuthorizationException extends SecurityException {

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}

