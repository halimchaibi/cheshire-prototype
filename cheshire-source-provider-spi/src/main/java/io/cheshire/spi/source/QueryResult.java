/*-
 * #%L
 * Cheshire :: Source Provider :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.source;

/**
 * Represents the semantic result of a query execution. May contain rows, update counts, or metadata.
 */
public interface QueryResult extends AutoCloseable {
    @Override
    void close() throws SourceProviderException;
}
