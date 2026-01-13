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
 * The type Execution exception.
 */
public class ExecutionException extends CheshireException {
    /**
     * Instantiates a new Execution exception.
     *
     * @param message
     *            the message
     */
    public ExecutionException(String message) {
        super(message);
    }

    /**
     * Instantiates a new Execution exception.
     *
     * @param message
     *            the message
     * @param cause
     *            the cause
     */
    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
