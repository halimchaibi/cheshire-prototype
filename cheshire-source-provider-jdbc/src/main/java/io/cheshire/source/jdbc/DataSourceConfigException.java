package io.cheshire.source.jdbc;

import io.cheshire.common.exception.ConfigurationException;

public class DataSourceConfigException extends ConfigurationException {
    /**
     * Instantiates a new Configuration exception.
     *
     * @param message the message
     */
    public DataSourceConfigException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Configuration exception.
     *
     * @param message the message
     * @param cause   the cause
     */
    public DataSourceConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
