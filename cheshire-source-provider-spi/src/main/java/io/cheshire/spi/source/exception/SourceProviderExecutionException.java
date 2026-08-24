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

import io.cheshire.spi.source.SourceProviderQuery;
import io.cheshire.spi.source.SourceProviderQueryResult;
import java.util.Optional;

/**
 * Sealed exception thrown when query execution fails.
 *
 * <p>This exception indicates failures during query execution such as:
 *
 * <ul>
 *   <li>Query syntax errors
 *   <li>Constraint violations
 *   <li>Data type mismatches
 *   <li>Runtime errors in the data source
 *   <li>Resource exhaustion during execution
 * </ul>
 *
 * <h2>Sealed Hierarchy</h2>
 *
 * <p>This class is sealed and permits {@link SourceProviderTimeoutException} as the only subtype.
 *
 * <h2>Error Code</h2>
 *
 * <p>{@code EXECUTION_FAILED} - This error is typically <b>not retryable</b> unless caused by
 * transient issues.
 *
 * <h2>Partial Results</h2>
 *
 * <p>In some cases, execution may fail after producing partial results. These can be retrieved via
 * {@link #getPartialResult()} for logging or recovery purposes.
 *
 * @see SourceProviderTimeoutException
 * @since 1.0
 */
public sealed class SourceProviderExecutionException extends SourceProviderException
    permits SourceProviderTimeoutException {

  private final SourceProviderQuery failedQuery;
  private final SourceProviderQueryResult partialResult;

  /**
   * Constructs an execution exception for the specified source.
   *
   * @param message the detail message
   * @param sourceName the name of the source provider
   */
  public SourceProviderExecutionException(String message, String sourceName) {
    super(message, sourceName);
    this.failedQuery = null;
    this.partialResult = null;
  }

  /**
   * Constructs an execution exception with the specified message, source, and cause.
   *
   * @param message the detail message
   * @param sourceName the name of the source provider
   * @param cause the underlying cause
   */
  public SourceProviderExecutionException(String message, String sourceName, Throwable cause) {
    super(message, sourceName, cause);
    this.failedQuery = null;
    this.partialResult = null;
  }

  /**
   * Constructs an execution exception with full context including the failed query and partial
   * results.
   *
   * @param message the detail message
   * @param sourceName the name of the source provider
   * @param failedQuery the query that failed
   * @param partialResult partial results if available (may be {@code null})
   * @param cause the underlying cause
   */
  public SourceProviderExecutionException(
      String message,
      String sourceName,
      SourceProviderQuery failedQuery,
      SourceProviderQueryResult partialResult,
      Throwable cause) {
    super(message, sourceName, cause);
    this.failedQuery = failedQuery;
    this.partialResult = partialResult;
  }

  /**
   * Returns the query that failed execution.
   *
   * @return the failed query, or {@code null} if not available
   */
  public SourceProviderQuery getFailedQuery() {
    return failedQuery;
  }

  /**
   * Returns partial results produced before the failure, if any.
   *
   * <p>Partial results may be available for queries that fail midway through execution (e.g., due
   * to connection loss or timeout after fetching some rows).
   *
   * @return an {@link Optional} containing partial results, or empty if none available
   */
  public Optional<SourceProviderQueryResult> getPartialResult() {
    return Optional.ofNullable(partialResult);
  }

  /**
   * Returns {@code "EXECUTION_FAILED"}.
   *
   * @return the error code
   */
  @Override
  public String getErrorCode() {
    return "EXECUTION_FAILED";
  }
}
