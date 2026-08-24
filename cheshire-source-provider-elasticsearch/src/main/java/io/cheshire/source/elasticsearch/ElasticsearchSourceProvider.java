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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cheshire.spi.source.SourceProvider;
import io.cheshire.spi.source.exception.SourceProviderConnectionException;
import io.cheshire.spi.source.exception.SourceProviderException;
import io.cheshire.spi.source.exception.SourceProviderExecutionException;
import io.cheshire.spi.source.exception.SourceProviderTimeoutException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ElasticsearchSourceProvider implements SourceProvider<ElasticsearchQuery> {

  private static final Logger log = LoggerFactory.getLogger(ElasticsearchSourceProvider.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
  private static final int MAX_ATTEMPTS = 3;

  private final ElasticsearchSourceProviderConfig config;
  private final AtomicBoolean open = new AtomicBoolean(false);
  private volatile HttpClient client;

  public ElasticsearchSourceProvider(ElasticsearchSourceProviderConfig config) {
    this.config = Objects.requireNonNull(config, "config must not be null");
  }

  @Override
  public String name() {
    return config.name();
  }

  @Override
  public void open() throws SourceProviderConnectionException {
    if (open.get()) {
      return;
    }

    synchronized (this) {
      if (open.get()) {
        return;
      }

      client =
          HttpClient.newBuilder()
              .connectTimeout(config.connectTimeout())
              .followRedirects(HttpClient.Redirect.NEVER)
              .build();

      try {
        final var response = send(request(config.endpoint()).GET().build());
        if (response.statusCode() >= 400) {
          throw new SourceProviderConnectionException(
              "Elasticsearch health check failed with HTTP " + response.statusCode(),
              config.name(),
              config.endpoint().toString(),
              null);
        }
        open.set(true);
        log.info("Elasticsearch source '{}' opened at {}", config.name(), config.endpoint());
      } catch (IOException e) {
        client = null;
        throw new SourceProviderConnectionException(
            "Failed to connect to Elasticsearch", config.name(), config.endpoint().toString(), e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        client = null;
        throw new SourceProviderConnectionException(
            "Interrupted while connecting to Elasticsearch",
            config.name(),
            config.endpoint().toString(),
            e);
      }
    }
  }

  @Override
  public boolean isOpen() {
    return open.get() && client != null;
  }

  @Override
  public ElasticsearchSourceProviderConfig config() {
    return config;
  }

  @Override
  public ElasticsearchQueryResult execute(ElasticsearchQuery query) throws SourceProviderException {
    ensureOpen();

    final var startedAt = Instant.now();
    final var request =
        request(searchUri(query.index()))
            .POST(HttpRequest.BodyPublishers.ofString(searchBody(query)))
            .build();

    try {
      final var response = sendWithRetry(request);
      if (response.statusCode() >= 400) {
        throw new SourceProviderExecutionException(
            "Elasticsearch query failed with HTTP "
                + response.statusCode()
                + ": "
                + response.body(),
            config.name());
      }
      return ElasticsearchQueryResult.of(rowsFrom(response.body()));
    } catch (HttpTimeoutException e) {
      throw new SourceProviderTimeoutException(
          "Elasticsearch query timed out",
          config.name(),
          Duration.between(startedAt, Instant.now()),
          config.requestTimeout(),
          e);
    } catch (IOException e) {
      throw new SourceProviderExecutionException(
          "Elasticsearch query failed", config.name(), query, null, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SourceProviderExecutionException(
          "Elasticsearch query was interrupted", config.name(), query, null, e);
    }
  }

  @Override
  public void close() {
    open.set(false);
    client = null;
  }

  private void ensureOpen() throws SourceProviderConnectionException {
    if (!isOpen()) {
      open();
    }
  }

  private HttpRequest.Builder request(URI uri) {
    final var builder =
        HttpRequest.newBuilder(uri)
            .timeout(config.requestTimeout())
            .header("Accept", "application/json")
            .header("Content-Type", "application/json");

    config.headers().forEach(builder::header);
    config.apiKey().ifPresent(apiKey -> builder.header("Authorization", "ApiKey " + apiKey));
    config.basicAuth().ifPresent(auth -> builder.header("Authorization", basicAuth(auth)));
    return builder;
  }

  private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
    return Objects.requireNonNull(client, "client must be initialized")
        .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  private HttpResponse<String> sendWithRetry(HttpRequest request)
      throws IOException, InterruptedException {
    HttpResponse<String> response = null;
    IOException failure = null;

    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        response = send(request);
        if (!isRetryable(response.statusCode()) || attempt == MAX_ATTEMPTS) {
          return response;
        }
      } catch (IOException e) {
        failure = e;
        if (attempt == MAX_ATTEMPTS) {
          throw e;
        }
      }
      sleepBeforeRetry(attempt);
    }

    if (failure != null) {
      throw failure;
    }
    return Objects.requireNonNull(response, "response must not be null");
  }

  private URI searchUri(String index) {
    final var encoded = URLEncoder.encode(index, StandardCharsets.UTF_8).replace("+", "%20");
    return config.endpoint().resolve("/" + encoded + "/_search");
  }

  private String searchBody(ElasticsearchQuery query) throws SourceProviderExecutionException {
    final var body = new LinkedHashMap<>(query.body());
    final var size = body.get("size");
    if (size instanceof Number number) {
      body.put("size", Math.min(number.intValue(), config.maxResults()));
    } else {
      body.put("size", config.maxResults());
    }

    try {
      return MAPPER.writeValueAsString(body);
    } catch (JsonProcessingException e) {
      throw new SourceProviderExecutionException(
          "Failed to serialize Elasticsearch query", config.name(), query, null, e);
    }
  }

  private List<Map<String, Object>> rowsFrom(String responseBody) throws IOException {
    final JsonNode root = MAPPER.readTree(responseBody);
    final JsonNode hits = root.path("hits").path("hits");
    if (!hits.isArray()) {
      return List.of(MAPPER.convertValue(root, MAP_TYPE));
    }
    return StreamSupport.stream(hits.spliterator(), false).map(this::rowFromHit).toList();
  }

  private Map<String, Object> rowFromHit(JsonNode hit) {
    final var row = new LinkedHashMap<String, Object>();
    final JsonNode source = hit.path("_source");
    if (!source.isMissingNode() && !source.isNull()) {
      row.putAll(MAPPER.convertValue(source, MAP_TYPE));
      row.put("_source", MAPPER.convertValue(source, MAP_TYPE));
    }
    putIfPresent(row, "_index", hit.path("_index"));
    putIfPresent(row, "_id", hit.path("_id"));
    putIfPresent(row, "_score", hit.path("_score"));
    return Map.copyOf(row);
  }

  private static void putIfPresent(Map<String, Object> row, String key, JsonNode value) {
    if (!value.isMissingNode() && !value.isNull()) {
      row.put(key, value.isNumber() ? value.numberValue() : value.asText());
    }
  }

  private static String basicAuth(ElasticsearchSourceProviderConfig.BasicAuth auth) {
    final var token = auth.username() + ":" + auth.password();
    return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
  }

  private static boolean isRetryable(int statusCode) {
    return statusCode == 429 || statusCode == 502 || statusCode == 503 || statusCode == 504;
  }

  private static void sleepBeforeRetry(int attempt) throws InterruptedException {
    final var jitterMs = ThreadLocalRandom.current().nextLong(25, 75);
    Thread.sleep((100L * attempt) + jitterMs);
  }
}
