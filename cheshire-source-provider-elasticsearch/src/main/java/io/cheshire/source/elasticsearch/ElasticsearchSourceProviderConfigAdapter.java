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

import io.cheshire.spi.source.SourceProviderConfigAdapter;
import io.cheshire.spi.source.exception.SourceProviderConfigException;
import io.cheshire.spi.source.exception.SourceProviderException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ElasticsearchSourceProviderConfigAdapter
    implements SourceProviderConfigAdapter<ElasticsearchSourceProviderConfig> {

  @Override
  public ElasticsearchSourceProviderConfig adapt(Map<String, Object> config)
      throws SourceProviderException {
    final var values = config == null ? Map.<String, Object>of() : Map.copyOf(config);
    try {
      return new ElasticsearchSourceProviderConfig(
          text(values, "name").orElse("elasticsearch"),
          URI.create(text(values, "endpoint").or(() -> text(values, "url")).orElseThrow()),
          text(values, "apiKey"),
          basicAuth(values),
          duration(values, "connectTimeoutMs", 5_000),
          duration(values, "requestTimeoutMs", 30_000),
          integer(values, "maxResults", 1_000),
          headers(values));
    } catch (Exception e) {
      throw new SourceProviderConfigException("Invalid Elasticsearch source configuration", e);
    }
  }

  private static Optional<ElasticsearchSourceProviderConfig.BasicAuth> basicAuth(
      Map<String, Object> values) {
    final var username = text(values, "username");
    final var password = text(values, "password");
    if (username.isEmpty() || password.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new ElasticsearchSourceProviderConfig.BasicAuth(username.get(), password.get()));
  }

  private static Optional<String> text(Map<String, Object> values, String key) {
    return Optional.ofNullable(values.get(key))
        .map(Object::toString)
        .filter(value -> !value.isBlank());
  }

  private static Duration duration(Map<String, Object> values, String key, long defaultMillis) {
    return Duration.ofMillis(integer(values, key, (int) defaultMillis));
  }

  private static int integer(Map<String, Object> values, String key, int defaultValue) {
    return Optional.ofNullable(values.get(key))
        .map(Object::toString)
        .map(Integer::parseInt)
        .orElse(defaultValue);
  }

  private static Map<String, String> headers(Map<String, Object> values) {
    final var rawHeaders = values.get("headers");
    if (!(rawHeaders instanceof Map<?, ?> headers)) {
      return Map.of();
    }
    return headers.entrySet().stream()
        .collect(
            Collectors.toUnmodifiableMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
  }
}
