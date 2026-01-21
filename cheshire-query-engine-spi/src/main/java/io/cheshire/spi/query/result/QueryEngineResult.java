/*-
 * #%L
 * Cheshire :: Query Engine :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.query.result;

import io.cheshire.spi.query.exception.QueryExecutionException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public record QueryEngineResult(List<Column> columns, List<Map<String, Object>> rows)
    implements Iterable<Map<String, Object>>, AutoCloseable {

  public QueryEngineResult(List<Column> columns, List<Map<String, Object>> rows) {
    this.columns = columns != null ? List.copyOf(columns) : Collections.emptyList();
    this.rows = rows != null ? List.copyOf(rows) : Collections.emptyList();
  }

  @Override
  public List<Column> columns() {
    return columns;
  }

  public Map<String, Object> row(int index) {
    if (index < 0 || index >= rows.size()) {
      throw new IndexOutOfBoundsException("Invalid row index: " + index);
    }
    return rows.get(index);
  }

  @Override
  public List<Map<String, Object>> rows() {
    return rows;
  }

  public int rowCount() {
    return rows.size();
  }

  @Override
  @NotNull
  public Iterator<Map<String, Object>> iterator() {
    return rows.iterator();
  }

  public Stream<Map<String, Object>> stream() throws QueryExecutionException {
    return rows.stream();
  }

  public boolean isEmpty() {
    return rows.isEmpty();
  }

  @Override
  public void close() {
    // No resources to clean up for in-memory results
  }

  public record Column(String name, String type, boolean nullable) {

    public Column {
      java.util.Objects.requireNonNull(name, "Column name cannot be null");
      java.util.Objects.requireNonNull(type, "Column type cannot be null");
    }
  }
}
