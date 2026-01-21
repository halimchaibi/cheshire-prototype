/*-
 * #%L
 * Cheshire :: Source Provider :: JDBC
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.source.jdbc;

import io.cheshire.spi.source.SourceProviderQueryResult;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record SqlSourceProviderQueryResult(List<Map<String, Object>> rows)
    implements SourceProviderQueryResult {

  public static SqlSourceProviderQueryResult of(List<Map<String, Object>> rows) {
    return new SqlSourceProviderQueryResult(rows);
  }

  @Override
  public int rowCount() {
    return rows.size();
  }

  @Override
  public boolean isEmpty() {
    return rows.isEmpty();
  }

  @Override
  public Iterator<Map<String, Object>> iterator() {
    return rows.iterator();
  }

  @Override
  public Stream<Map<String, Object>> stream() {
    return rows.stream();
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub
  }
}
