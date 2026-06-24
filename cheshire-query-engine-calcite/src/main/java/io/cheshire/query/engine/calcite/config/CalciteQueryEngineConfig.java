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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record CalciteQueryEngineConfig(
    String name, Map<String, Object> sources, Map<String, Object> config)
    implements QueryEngineConfig {

  @Override
  public Map<String, Object> asMap() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("name", name);
    values.put("sources", sources == null ? Map.of() : Map.copyOf(sources));
    values.put("config", config == null ? Map.of() : Map.copyOf(config));
    return Collections.unmodifiableMap(values);
  }

  @Override
  public boolean validate() throws QueryEngineConfigurationException {
    if (name == null || name.isBlank()) {
      throw new QueryEngineConfigurationException("Calcite query engine name is required", this);
    }
    if (sources == null || sources.isEmpty()) {
      throw new QueryEngineConfigurationException(
          "Calcite query engine requires at least one source", this);
    }
    return true;
  }
}
