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

import io.cheshire.spi.source.SourceProviderConfig;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ElasticsearchSourceProviderConfig(
    String name,
    URI endpoint,
    Optional<String> apiKey,
    Optional<BasicAuth> basicAuth,
    Duration connectTimeout,
    Duration requestTimeout,
    int maxResults,
    Map<String, String> headers)
    implements SourceProviderConfig {

  public ElasticsearchSourceProviderConfig {
    name = requireText(name, "name");
    endpoint = normalizeEndpoint(endpoint);
    apiKey = apiKey == null ? Optional.empty() : apiKey.filter(value -> !value.isBlank());
    basicAuth = basicAuth == null ? Optional.empty() : basicAuth;
    connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
    requestTimeout = requestTimeout == null ? Duration.ofSeconds(30) : requestTimeout;
    if (maxResults <= 0) {
      throw new IllegalArgumentException("maxResults must be positive");
    }
    headers = headers == null ? Map.of() : Map.copyOf(headers);
  }

  @Override
  public Map<String, Object> asMap() {
    return Map.of(
        "name",
        name,
        "endpoint",
        endpoint.toString(),
        "apiKey",
        apiKey.map(ElasticsearchSourceProviderConfig::mask).orElse(""),
        "basicAuth",
        basicAuth.map(auth -> auth.username() + ":***").orElse(""),
        "connectTimeoutMs",
        connectTimeout.toMillis(),
        "requestTimeoutMs",
        requestTimeout.toMillis(),
        "maxResults",
        maxResults,
        "headers",
        headers);
  }

  @Override
  public String get(String key) {
    return switch (key) {
      case "name" -> name;
      case "endpoint", "url" -> endpoint.toString();
      case "connectTimeoutMs" -> Long.toString(connectTimeout.toMillis());
      case "requestTimeoutMs" -> Long.toString(requestTimeout.toMillis());
      case "maxResults" -> Integer.toString(maxResults);
      default -> headers.get(key);
    };
  }

  public record BasicAuth(String username, String password) {
    public BasicAuth {
      username = requireText(username, "username");
      password = Objects.requireNonNull(password, "password must not be null");
    }
  }

  private static URI normalizeEndpoint(URI endpoint) {
    final var value = Objects.requireNonNull(endpoint, "endpoint must not be null").normalize();
    if (!"http".equals(value.getScheme()) && !"https".equals(value.getScheme())) {
      throw new IllegalArgumentException("endpoint must use http or https");
    }
    return URI.create(value.toString().replaceAll("/+$", ""));
  }

  private static String requireText(String value, String field) {
    final var normalized = Objects.requireNonNull(value, field + " must not be null").trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return normalized;
  }

  private static String mask(String value) {
    return value.length() <= 4 ? "****" : "****" + value.substring(value.length() - 4);
  }
}
