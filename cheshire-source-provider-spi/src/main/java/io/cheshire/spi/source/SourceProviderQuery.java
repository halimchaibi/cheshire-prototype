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

import java.util.Map;

/**
 * Base interface for queries executed against source providers.
 *
 * <p>A {@code SourceProviderQuery} encapsulates the query representation and associated parameters
 * for execution against a {@link SourceProvider}. Different source types will have specific query
 * implementations (SQL, NoSQL, REST, etc.).
 *
 * <h2>Query Components</h2>
 *
 * <ul>
 *   <li><b>Query Object</b>: The actual query (SQL string, DSL object, HTTP request, etc.)
 *   <li><b>Parameters</b>: Named or positional parameters for safe query execution
 * </ul>
 *
 * <h2>Example Implementations</h2>
 *
 * <h3>SQL Query</h3>
 *
 * <pre>{@code
 * public record SqlQuery(String sql, Map<String, Object> params)
 *         implements SourceProviderQuery {
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
 *
 * // Usage
 * SqlQuery query = new SqlQuery(
 *     "SELECT * FROM users WHERE age > :minAge",
 *     Map.of("minAge", 18)
 * );
 * }</pre>
 *
 * <h3>REST Query</h3>
 *
 * <pre>{@code
 * public record RestQuery(String endpoint, String method,
 *                        Map<String, Object> queryParams)
 *         implements SourceProviderQuery {
 *
 *     @Override
 *     public Object query() {
 *         return endpoint;  // URL endpoint
 *     }
 *
 *     @Override
 *     public Map<String, Object> parameters() {
 *         return queryParams;
 *     }
 * }
 * }</pre>
 *
 * <h2>Parameter Security</h2>
 *
 * <p>Always use parameterized queries to prevent injection attacks. Implementations should validate
 * and sanitize parameters appropriately for their query type.
 *
 * @see SourceProvider#execute(SourceProviderQuery)
 * @see SourceProviderQueryResult
 * @since 1.0
 */
public interface SourceProviderQuery {

  /**
   * Returns the query parameters as a map.
   *
   * <p>Parameters enable safe, reusable queries by separating data from the query structure. The
   * map keys are parameter names, and values are the parameter values.
   *
   * <h3>Parameter Types</h3>
   *
   * <p>Common parameter value types:
   *
   * <ul>
   *   <li>Primitives: {@code Integer}, {@code Long}, {@code Double}, {@code Boolean}
   *   <li>Strings: {@code String}
   *   <li>Temporal: {@code LocalDate}, {@code LocalDateTime}, {@code Instant}
   *   <li>Collections: {@code List}, {@code Set} (for IN clauses)
   * </ul>
   *
   * @return a map of parameter names to values, or an empty map if no parameters; never {@code
   *     null}
   */
  Map<String, Object> parameters();

  /**
   * Returns the query object.
   *
   * <p>The type and structure of the query object depends on the source provider implementation:
   *
   * <ul>
   *   <li><b>SQL</b>: {@code String} containing SQL statement
   *   <li><b>NoSQL</b>: Document query object or query DSL
   *   <li><b>REST</b>: URL endpoint or request object
   *   <li><b>File</b>: File path or file pattern
   * </ul>
   *
   * @return the query object, type depends on implementation; never {@code null}
   */
  Object query();

  /**
   * Checks whether this query has parameters.
   *
   * <p>This is a convenience method equivalent to {@code !parameters().isEmpty()}.
   *
   * @return {@code true} if the query has one or more parameters, {@code false} otherwise
   */
  default boolean hasParameters() {
    return parameters() != null && !parameters().isEmpty();
  }
}
