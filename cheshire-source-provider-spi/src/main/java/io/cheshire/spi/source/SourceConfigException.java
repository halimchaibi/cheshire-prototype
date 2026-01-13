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
 * Exception indicating a problem with a SourceConfigT.
 *
 * <p>
 * Thrown when a SourceConfigT is invalid, incomplete, or otherwise unsuitable for creating a SourceProvider.
 * </p>
 */
public class SourceConfigException extends Exception {

    public SourceConfigException(String message) {
        super(message);
    }

    public SourceConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
