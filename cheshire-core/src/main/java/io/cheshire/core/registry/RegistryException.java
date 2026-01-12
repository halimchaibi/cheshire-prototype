package io.cheshire.core.registry;

/**
 * Exception thrown when registry operations fail.
 */
public class RegistryException extends RuntimeException {
    public RegistryException(String message) {
        super(message);
    }

    public RegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
