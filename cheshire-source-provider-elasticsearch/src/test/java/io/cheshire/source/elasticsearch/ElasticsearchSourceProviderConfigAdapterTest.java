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

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ElasticsearchSourceProviderConfigAdapterTest {

  @Test
  void adaptsTypedConfigurationWithSafeDefaults() throws Exception {
    final var adapter = new ElasticsearchSourceProviderConfigAdapter();

    final var config =
        adapter.adapt(
            Map.of(
                "name",
                "search",
                "endpoint",
                "http://localhost:9200/",
                "apiKey",
                "abcdef123456",
                "requestTimeoutMs",
                2_000,
                "headers",
                Map.of("X-Tenant", "blog")));

    assertThat(config.name()).isEqualTo("search");
    assertThat(config.endpoint().toString()).isEqualTo("http://localhost:9200");
    assertThat(config.apiKey()).contains("abcdef123456");
    assertThat(config.basicAuth()).isEmpty();
    assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
    assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(2));
    assertThat(config.maxResults()).isEqualTo(1_000);
    assertThat(config.headers()).containsEntry("X-Tenant", "blog");
    assertThat(config.asMap()).containsEntry("apiKey", "****3456");
  }
}
