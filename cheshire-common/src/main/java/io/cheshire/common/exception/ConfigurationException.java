/*-
 * #%L
 * Cheshire :: Common Utils
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.common.exception;

/**
 * The type Configuration exception.
 */
public class ConfigurationException extends CheshireException {
    /**
     * Instantiates a new Configuration exception.
     *
     * @param message
     *            the message
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Configuration exception.
     *
     * @param message
     *            the message
     * @param cause
     *            the cause
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
