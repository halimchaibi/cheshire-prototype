package io.cheshire.spi.source;

/**
 * Represents the semantic result of a query execution.
 * May contain rows, update counts, or metadata.
 */
public interface QueryResult extends AutoCloseable {
    @Override
    void close() throws SourceProviderException;
}