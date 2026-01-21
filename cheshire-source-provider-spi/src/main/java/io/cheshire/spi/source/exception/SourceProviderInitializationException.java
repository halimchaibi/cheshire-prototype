/*-
 * #%L
 * Cheshire :: Source Provider :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.source.exception;

/**
 * Exception thrown when a source provider fails to initialize.
 *
 * <p>Initialization failures typically occur during resource allocation or setup operations before
 * the provider is ready to execute queries. Common causes include:
 *
 * <ul>
 *   <li>Missing or incompatible driver/client libraries
 *   <li>Resource allocation failures (memory, threads, pools)
 *   <li>Invalid state or preconditions
 *   <li>System resource limits exceeded
 * </ul>
 *
 * <h2>Error Code</h2>
 *
 * <p>{@code UNKNOWN} - This error is <b>not retryable</b> as it typically indicates a system or
 * configuration issue.
 *
 * @since 1.0
 */
public final class SourceProviderInitializationException extends SourceProviderException {

  /**
   * Constructs an initialization exception with the specified message.
   *
   * @param message the detail message
   */
  public SourceProviderInitializationException(String message) {
    super(message);
  }

  /**
   * Constructs an initialization exception for the specified source.
   *
   * @param message the detail message
   * @param sourceName the name of the source provider
   */
  public SourceProviderInitializationException(String message, String sourceName) {
    super(message, sourceName);
  }

  /**
   * Constructs an initialization exception with the specified message and cause.
   *
   * @param message the detail message
   * @param cause the underlying cause
   */
  public SourceProviderInitializationException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs an initialization exception with full context.
   *
   * @param message the detail message
   * @param sourceName the name of the source provider
   * @param cause the underlying cause
   */
  public SourceProviderInitializationException(String message, String sourceName, Throwable cause) {
    super(message, sourceName, cause);
  }
}
