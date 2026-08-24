/*-
 * #%L
 * Cheshire :: Source Provider :: Elasticsearch
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.source.elasticsearch;

import io.cheshire.spi.source.SourceProviderQueryResult;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record ElasticsearchQueryResult(List<Map<String, Object>> rows)
    implements SourceProviderQueryResult {

  public ElasticsearchQueryResult {
    rows = rows == null ? List.of() : List.copyOf(rows);
  }

  public static ElasticsearchQueryResult of(List<Map<String, Object>> rows) {
    return new ElasticsearchQueryResult(rows);
  }

  @Override
  public int rowCount() {
    return rows.size();
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
  public void close() {}
}
