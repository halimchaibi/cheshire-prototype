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

import java.time.Duration;

/**
 * Exception thrown when query execution exceeds the configured timeout.
 *
 * <p>Timeout exceptions indicate that a query took longer than the allowed duration. This may occur
 * due to:
 *
 * <ul>
 *   <li>Long-running queries (inefficient SQL, large datasets)
 *   <li>Resource contention (locks, CPU, I/O)
 *   <li>Network latency
 *   <li>Database performance issues
 * </ul>
 *
 * <h2>Error Code</h2>
 *
 * <p>{@code QUERY_TIMEOUT} - This error is <b>retryable</b> as timeouts may be transient.
 *
 * <h2>Timing Information</h2>
 *
 * <p>The exception provides both the configured timeout and the actual elapsed time, which can be
 * used for metrics, logging, and debugging.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * long start = System.currentTimeMillis();
 * try {
 *     result = executeWithTimeout(query, timeout);
 * } catch (TimeoutException e) {
 *     Duration elapsed = Duration.ofMillis(System.currentTimeMillis() - start);
 *     throw new SourceProviderTimeoutException(
 *         "Query exceeded timeout",
 *         sourceName,
 *         elapsed,
 *         timeout,
 *         e);
 * }
 * }</pre>
 *
 * @since 1.0
 */
public final class SourceProviderTimeoutException extends SourceProviderExecutionException {

  private final Duration elapsedTime;
  private final Duration timeout;

  /**
   * Constructs a timeout exception with timing information.
   *
   * @param message the detail message
   * @param sourceName the name of the source provider
   * @param elapsedTime the actual time elapsed before timeout
   * @param timeout the configured timeout duration
   */
  public SourceProviderTimeoutException(
      String message, String sourceName, Duration elapsedTime, Duration timeout) {
    super(message, sourceName);
    this.elapsedTime = elapsedTime;
    this.timeout = timeout;
  }

  /**
   * Constructs a timeout exception with timing information and cause.
   *
   * @param message the detail message
   * @param sourceName the name of the source provider
   * @param elapsedTime the actual time elapsed before timeout
   * @param timeout the configured timeout duration
   * @param cause the underlying cause
   */
  public SourceProviderTimeoutException(
      String message, String sourceName, Duration elapsedTime, Duration timeout, Throwable cause) {
    super(message, sourceName, cause);
    this.elapsedTime = elapsedTime;
    this.timeout = timeout;
  }

  /**
   * Returns the actual time elapsed before the timeout occurred.
   *
   * @return the elapsed time, never {@code null}
   */
  public Duration getElapsedTime() {
    return elapsedTime;
  }

  /**
   * Returns the configured timeout duration.
   *
   * @return the timeout, never {@code null}
   */
  public Duration getTimeout() {
    return timeout;
  }

  /**
   * Returns {@code "QUERY_TIMEOUT"}.
   *
   * @return the error code
   */
  @Override
  public String getErrorCode() {
    return "QUERY_TIMEOUT";
  }

  /**
   * Returns {@code true} as query timeouts are typically transient and retryable.
   *
   * @return {@code true}
   */
  @Override
  public boolean isRetryable() {
    return true;
  }
}
