/*-
 * #%L
 * Cheshire :: Query Engine :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.query.exception;

import io.cheshire.spi.query.request.LogicalQuery;
import io.cheshire.spi.query.result.QueryEngineResult;
import java.util.Optional;

public sealed class QueryExecutionException extends QueryEngineException
    permits QueryTimeoutException {

  private final LogicalQuery failedQuery;
  private final QueryEngineResult partialResult;

  public QueryExecutionException(String message) {
    super(message);
    this.failedQuery = null;
    this.partialResult = null;
  }

  public QueryExecutionException(String message, Throwable cause) {
    super(message, cause);
    this.failedQuery = null;
    this.partialResult = null;
  }

  public QueryExecutionException(
      String message, LogicalQuery failedQuery, QueryEngineResult partialResult, Throwable cause) {
    super(message, cause);
    this.failedQuery = failedQuery;
    this.partialResult = partialResult;
  }

  public LogicalQuery getFailedQuery() {
    return failedQuery;
  }

  public Optional<QueryEngineResult> getPartialResult() {
    return Optional.ofNullable(partialResult);
  }

  @Override
  public String getErrorCode() {
    return "EXECUTION_FAILED";
  }
}
