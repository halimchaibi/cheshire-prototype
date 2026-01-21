/*-
 * #%L
 * Cheshire :: Query Engine :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.query.request;

import java.util.Map;

/**
 * Base interface for logical queries submitted to query engines.
 *
 * <p>A logical query represents the query in a format independent of physical execution. Query
 * engines transform logical queries into physical queries for execution against specific data
 * sources.
 *
 * <h2>Example Implementation</h2>
 *
 * <pre>{@code
 * public record SqlLogicalQuery(String sql, Map<String, Object> params)
 *         implements LogicalQuery {
 *
 *     @Override
 *     public Object query() {
 *         return sql;
 *     }
 *
 *     @Override
 *     public Map<String, Object> parameters() {
 *         return params;
 *     }
 * }
 * }</pre>
 *
 * @see io.cheshire.spi.query.engine.QueryEngine#execute(LogicalQuery, QueryEngineContext)
 * @since 1.0
 */
public interface LogicalQuery {

  /**
   * Returns the query parameters.
   *
   * @return a map of parameter names to values, or an empty map; never {@code null}
   */
  Map<String, Object> parameters();

  /**
   * Returns the query object.
   *
   * <p>The type depends on the implementation (SQL string, DSL object, etc.).
   *
   * @return the query object, never {@code null}
   */
  Object query();

  /**
   * Checks whether this query has parameters.
   *
   * @return {@code true} if parameters are present, {@code false} otherwise
   */
  default boolean hasParameters() {
    return parameters() != null && !parameters().isEmpty();
  }
}
