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
 * Base runtime exception for all Cheshire framework errors.
 *
 * <p><strong>Exception Hierarchy:</strong>
 *
 * <pre>
 * CheshireException (base)
 *   ├─ ConfigurationException - Config loading/validation errors
 *   ├─ ValidationException - Input/schema validation errors
 *   ├─ ExecutionException - Runtime execution errors
 *   ├─ QueryEngineException - Query engine errors
 *   ├─ SourceProviderException - Data source errors
 *   └─ CheshireRuntimeError - Fatal runtime errors
 * </pre>
 *
 * <p><strong>Design Philosophy:</strong>
 *
 * <p>Extends {@link RuntimeException} to avoid checked exception proliferation. Framework uses this
 * as base for all domain-specific exceptions, providing a consistent error handling approach.
 *
 * <p><strong>Usage Pattern:</strong>
 *
 * <pre>{@code
 * try {
 *     // Framework operation
 * } catch (CheshireException e) {
 *     // Handle framework errors
 *     log.error("Framework error: {}", e.getMessage(), e);
 * }
 * }</pre>
 *
 * @see ConfigurationException
 * @see ValidationException
 * @see ExecutionException
 * @since 1.0.0
 */
public class CheshireException extends RuntimeException {
  /**
   * Instantiates a new Cheshire exception.
   *
   * @param message the message
   */
  public CheshireException(String message) {
    super(message);
  }

  /**
   * Instantiates a new Cheshire exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public CheshireException(String message, Throwable cause) {
    super(message, cause);
  }
}
