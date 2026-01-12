package io.cheshire.source.jdbc;

import io.cheshire.spi.source.SourceProviderException;

public class JdbcDataSourceException extends SourceProviderException {
    /**
     * Constructs a SourceProviderException with the specified detail message.
     *
     * @param message the detail message
     */
    public JdbcDataSourceException(String message) {
        super(message);
    }

    /**
     * Constructs a SourceProviderException with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause of this exception
     */
    public JdbcDataSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
