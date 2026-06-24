/*-
 * #%L
 * Cheshire :: Query Engine :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.query.engine;

import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.request.LogicalQuery;
import io.cheshire.spi.query.request.QueryEngineContext;
import io.cheshire.spi.query.result.QueryEngineResult;

public interface QueryEngine<Q extends LogicalQuery> extends AutoCloseable {

  String name();

  /**
   * Opens and initializes the query engine.
   *
   * <p>This method allocates resources and prepares the engine for query execution. It should be
   * called before {@link #execute(LogicalQuery, QueryEngineContext)}. Calling {@code open()} on an
   * already open engine should be idempotent.
   *
   * @throws io.cheshire.spi.query.exception.QueryEngineInitializationException if initialization
   *     fails
   * @throws QueryEngineException if any other error occurs
   */
  void open() throws QueryEngineException;

  /**
   * Executes a logical query against the sources provided in the context.
   *
   * <p>The engine transforms the logical query into physical queries for the available sources,
   * executes them, and merges the results into a unified {@link QueryEngineResult}.
   *
   * <h3>Execution Flow</h3>
   *
   * <ol>
   *   <li>Validate the query via {@link #validate(LogicalQuery)}
   *   <li>Extract sources from {@link QueryEngineContext#sources()}
   *   <li>Plan query execution (optimization, source selection)
   *   <li>Execute physical queries against sources
   *   <li>Merge and transform results
   *   <li>Check deadline from {@link QueryEngineContext#deadline()}
   * </ol>
   *
   * @param query the logical query to execute, must not be {@code null}
   * @param ctx the execution context with sources and metadata, must not be {@code null}
   * @return the query result with columns and rows, never {@code null}
   * @throws io.cheshire.spi.query.exception.QueryExecutionException if execution fails
   * @throws io.cheshire.spi.query.exception.QueryValidationException if the query is invalid
   * @throws io.cheshire.spi.query.exception.QueryTimeoutException if execution times out
   * @throws QueryEngineException if any other error occurs
   */
  QueryEngineResult execute(Q query, QueryEngineContext ctx) throws QueryEngineException;

  /**
   * Returns an explanation of how the query would be executed.
   *
   * <p>The explanation typically includes:
   *
   * <ul>
   *   <li>Query plan (logical and physical)
   *   <li>Source selection and pushdown strategies
   *   <li>Join strategies and ordering
   *   <li>Estimated costs (if available)
   * </ul>
   *
   * @param query the query to explain, must not be {@code null}
   * @return a human-readable execution plan, never {@code null}
   * @throws QueryEngineException if plan generation fails
   */
  String explain(Q query) throws QueryEngineException;

  /**
   * Validates the query without executing it.
   *
   * <p>Validation checks include:
   *
   * <ul>
   *   <li>Query syntax correctness
   *   <li>Semantic validity (referenced tables/columns exist)
   *   <li>Type compatibility
   * </ul>
   *
   * @param query the query to validate, must not be {@code null}
   * @return {@code true} if the query is valid, {@code false} otherwise
   * @throws QueryEngineException if validation fails with an error
   */
  boolean validate(Q query) throws QueryEngineException;

  /**
   * Indicates whether this engine supports streaming result sets.
   *
   * <p>Streaming engines can process large result sets without loading all data into memory.
   *
   * @return {@code true} if streaming is supported, {@code false} otherwise
   */
  boolean supportsStreaming();

  /**
   * Checks whether this query engine is currently open and ready for execution.
   *
   * @return {@code true} if the engine is open, {@code false} otherwise
   */
  boolean isOpen();

  /**
   * Closes the query engine and releases all associated resources.
   *
   * <p>After closing, the engine should not be used for query execution. This method should be
   * idempotent (safe to call multiple times).
   *
   * @throws QueryEngineException if an error occurs during cleanup
   */
  @Override
  void close() throws QueryEngineException;
}
