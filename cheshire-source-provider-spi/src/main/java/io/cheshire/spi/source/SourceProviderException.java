package io.cheshire.spi.source;

/**
 * Exception type used by {@link SourceProvider} implementations.
 *
 * <p>This exception signals errors related to the manager or operation
 * of a source, such as failures during initialization, query execution,
 * or closure. It provides a uniform way for the Cheshire query engine
 * to handle source-related errors.</p>
 */
public class SourceProviderException extends Exception {


    /**
     * Constructs a SourceProviderException with the specified detail message.
     *
     * @param message the detail message
     */
    public SourceProviderException(String message) {
        super(message);
    }

    /**
     * Constructs a SourceProviderException with the specified detail message
     * and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause of this exception
     */
    public SourceProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
