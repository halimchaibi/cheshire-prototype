package io.cheshire.common.exception;

/**
 * The type Configuration exception.
 */
public class ConfigurationException extends CheshireException {
    /**
     * Instantiates a new Configuration exception.
     *
     * @param message the message
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Configuration exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
