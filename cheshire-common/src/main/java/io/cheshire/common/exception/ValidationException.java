package io.cheshire.common.exception;

/**
 * The type Validation exception.
 */
public class ValidationException extends CheshireException {
    /**
     * Instantiates a new Validation exception.
     *
     * @param message the message
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Validation exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
