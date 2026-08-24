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

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ElasticsearchSourceProviderTest {

  private HttpServer server;
  private AtomicReference<String> requestBody;

  @BeforeEach
  void startServer() throws IOException {
    requestBody = new AtomicReference<>("");
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", this::handle);
    server.start();
  }

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void executesSearchAgainstElasticsearchHttpApi() throws Exception {
    final var provider =
        new ElasticsearchSourceProvider(
            new ElasticsearchSourceProviderConfig(
                "blog-search",
                URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                Optional.empty(),
                Optional.empty(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                25,
                Map.of()));

    final var result =
        provider.execute(
            ElasticsearchQuery.search(
                "articles", Map.of("query", Map.of("match", Map.of("title", "cheshire")))));

    assertThat(provider.isOpen()).isTrue();
    assertThat(requestBody.get()).contains("\"size\":25");
    assertThat(result.rows())
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("title", "Cheshire MVP");
              assertThat(row).containsEntry("_index", "articles");
              assertThat(row).containsEntry("_id", "article-1");
            });
  }

  private void handle(HttpExchange exchange) throws IOException {
    final var path = exchange.getRequestURI().getPath();
    final var response =
        switch (path) {
          case "/" -> "{}";
          case "/articles/_search" -> {
            requestBody.set(
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            yield """
                {
                  "hits": {
                    "hits": [
                      {
                        "_index": "articles",
                        "_id": "article-1",
                        "_score": 1.0,
                        "_source": {
                          "title": "Cheshire MVP",
                          "status": "published"
                        }
                      }
                    ]
                  }
                }
                """;
          }
          default -> throw new IllegalArgumentException("Unexpected path: " + path);
        };

    final var bytes = response.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
