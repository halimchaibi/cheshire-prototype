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

import io.cheshire.spi.source.exception.SourceProviderException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Result set interface for query execution results.
 *
 * <p>A {@code SourceProviderQueryResult} contains the rows returned from executing a query against
 * a source provider. It provides multiple access patterns for consuming results and implements
 * {@link AutoCloseable} for proper resource management.
 *
 * <h2>Data Model</h2>
 *
 * <p>Results are represented as a list of rows, where each row is a {@code Map<String, Object>}
 * with column names as keys and column values as values. This flexible structure supports
 * heterogeneous data sources without requiring schema knowledge at the SPI level.
 *
 * <h2>Access Patterns</h2>
 *
 * <p>The interface supports multiple ways to consume results:
 *
 * <ul>
 *   <li><b>List</b>: {@link #rows()} - All rows in memory (best for small result sets)
 *   <li><b>Iterator</b>: {@link #iterator()} - Sequential access
 *   <li><b>Stream</b>: {@link #stream()} - Functional processing with potential for lazy evaluation
 * </ul>
 *
 * <h2>Resource Management</h2>
 *
 * <p>Results may hold resources (database cursors, network connections, file handles) and should be
 * closed after use. Use try-with-resources for automatic cleanup:
 *
 * <pre>{@code
 * try (SourceProviderQueryResult result = provider.execute(query)) {
 *     result.rows().forEach(row -> {
 *         String name = (String) row.get("name");
 *         Integer age = (Integer) row.get("age");
 *         // Process row
 *     });
 * }
 * }</pre>
 *
 * <h2>Example Usage</h2>
 *
 * <h3>List Access</h3>
 *
 * <pre>{@code
 * List<Map<String, Object>> allRows = result.rows();
 * for (Map<String, Object> row : allRows) {
 *     System.out.println("User: " + row.get("name"));
 * }
 * }</pre>
 *
 * <h3>Stream Processing</h3>
 *
 * <pre>{@code
 * List<String> names = result.stream()
 *     .map(row -> (String) row.get("name"))
 *     .filter(name -> name.startsWith("A"))
 *     .collect(Collectors.toList());
 * }</pre>
 *
 * <h3>Iterator Pattern</h3>
 *
 * <pre>{@code
 * Iterator<Map<String, Object>> iter = result.iterator();
 * while (iter.hasNext()) {
 *     Map<String, Object> row = iter.next();
 *     // Process row
 * }
 * }</pre>
 *
 * @see SourceProvider#execute(SourceProviderQuery)
 * @see SourceProviderQuery
 * @since 1.0
 */
public interface SourceProviderQueryResult extends AutoCloseable {

  /**
   * Returns all rows as an immutable list.
   *
   * <p>For large result sets, consider using {@link #iterator()} or {@link #stream()} to avoid
   * loading all data into memory at once.
   *
   * <h3>Row Structure</h3>
   *
   * <p>Each row is a {@code Map<String, Object>} where:
   *
   * <ul>
   *   <li>Keys are column names (case-sensitive, as returned by the source)
   *   <li>Values are column values with appropriate Java types
   * </ul>
   *
   * @return an immutable list of rows, never {@code null}; may be empty
   */
  List<Map<String, Object>> rows();

  /**
   * Returns the number of rows in the result set.
   *
   * @return the row count, zero or positive
   */
  int rowCount();

  /**
   * Checks whether the result set is empty.
   *
   * <p>This is a convenience method equivalent to {@code rowCount() == 0}.
   *
   * @return {@code true} if the result set contains no rows, {@code false} otherwise
   */
  default boolean isEmpty() {
    return rowCount() == 0;
  }

  /**
   * Returns an iterator over the rows.
   *
   * <p>The iterator provides sequential access to rows, which may be more memory-efficient than
   * loading all rows via {@link #rows()} for large result sets.
   *
   * <h3>Implementation Note</h3>
   *
   * <p>For in-memory results, this typically returns {@code rows().iterator()}. For streaming
   * results, implementations may provide a cursor-based iterator that fetches rows on demand.
   *
   * @return an iterator over the result rows, never {@code null}
   */
  Iterator<Map<String, Object>> iterator();

  /**
   * Returns a stream of rows for functional-style processing.
   *
   * <p>Streams enable efficient, declarative processing of results using map, filter, and collect
   * operations. For large result sets, implementations may provide lazy evaluation.
   *
   * <h3>Stream Lifecycle</h3>
   *
   * <p>The stream should be closed after use (typically handled by closing the result). When using
   * operations that short-circuit (e.g., {@code findFirst()}, {@code limit()}), resources should be
   * released appropriately.
   *
   * <h3>Example</h3>
   *
   * <pre>{@code
   * long count = result.stream()
   *     .filter(row -> (Integer) row.get("age") > 18)
   *     .count();
   * }</pre>
   *
   * @return a stream of rows, never {@code null}
   * @throws SourceProviderException if an error occurs creating the stream
   */
  Stream<Map<String, Object>> stream();

  /**
   * Closes this result set and releases any associated resources.
   *
   * <p>After calling {@code close()}, attempts to access the result data may throw exceptions. This
   * method should be idempotent (safe to call multiple times).
   *
   * <h3>Resources Released</h3>
   *
   * <ul>
   *   <li>Database cursors or result sets
   *   <li>Network connections (for streaming results)
   *   <li>File handles
   *   <li>Memory buffers
   * </ul>
   *
   * @throws SourceProviderException if an error occurs during cleanup
   */
  @Override
  void close() throws SourceProviderException;
}
