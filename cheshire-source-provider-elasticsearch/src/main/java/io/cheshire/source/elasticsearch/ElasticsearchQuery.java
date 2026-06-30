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

import io.cheshire.spi.source.SourceProviderQuery;
import java.util.Map;
import java.util.Objects;

public record ElasticsearchQuery(
    String index, Map<String, Object> body, Map<String, Object> parameters)
    implements SourceProviderQuery {

  public ElasticsearchQuery {
    index = requireText(index, "index");
    body = body == null ? Map.of() : Map.copyOf(body);
    parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
  }

  public static ElasticsearchQuery search(String index, Map<String, Object> body) {
    return new ElasticsearchQuery(index, body, Map.of());
  }

  @Override
  public Object query() {
    return body;
  }

  private static String requireText(String value, String field) {
    final var normalized = Objects.requireNonNull(value, field + " must not be null").trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return normalized;
  }
}
