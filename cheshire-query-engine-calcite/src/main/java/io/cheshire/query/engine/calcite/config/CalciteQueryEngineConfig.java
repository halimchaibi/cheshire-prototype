/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.config;

import io.cheshire.spi.query.engine.QueryEngineConfig;
import io.cheshire.spi.query.exception.QueryEngineConfigurationException;
import java.util.Map;
import java.util.Objects;

public record CalciteQueryEngineConfig(
    String name, Map<String, Object> sources, Map<String, Object> config)
    implements QueryEngineConfig {

  public CalciteQueryEngineConfig {
    name = Objects.requireNonNull(name, "Calcite query engine name is required");
    sources =
        Map.copyOf(Objects.requireNonNull(sources, "Calcite query engine sources are required"));
    config = Map.copyOf(Objects.requireNonNull(config, "Calcite query engine config is required"));
  }

  @Override
  public Map<String, Object> asMap() {
    return Map.of("name", name, "sources", sources, "config", config);
  }

  @Override
  public boolean validate() throws QueryEngineConfigurationException {
    if (name.isBlank()) {
      throw new QueryEngineConfigurationException("Calcite query engine name is required", this);
    }
    if (sources.isEmpty()) {
      throw new QueryEngineConfigurationException(
          "Calcite query engine requires at least one source", this);
    }
    return true;
  }
}
