/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite;

import io.cheshire.common.utils.MapUtils;
import io.cheshire.query.engine.calcite.config.CalciteQueryEngineConfig;
import io.cheshire.spi.query.engine.QueryEngineConfigAdapter;
import io.cheshire.spi.query.exception.QueryEngineConfigurationException;
import io.cheshire.spi.query.exception.QueryEngineException;
import java.util.Map;

/** The type Calcite query engine config adapter. */
public class CalciteQueryEngineConfigAdapter
    implements QueryEngineConfigAdapter<CalciteQueryEngineConfig> {

  @Override
  public CalciteQueryEngineConfig adapt(Map<String, Object> engineConfig)
      throws QueryEngineException {

    String name =
        MapUtils.someValueFromMapAs(engineConfig, "name", String.class)
            .orElseThrow(
                () -> new QueryEngineConfigurationException("Engine name cannot be null or blank"));

    @SuppressWarnings("unchecked")
    Map<String, Object> sources =
        MapUtils.someValueFromMapAs(engineConfig, "sources", Map.class)
            .orElseThrow(
                () -> new QueryEngineConfigurationException("Sources cannot be null or blank"));

    @SuppressWarnings("unchecked")
    Map<String, Object> config =
        MapUtils.someValueFromMapAs(engineConfig, "config", Map.class)
            .orElseThrow(
                () -> new QueryEngineConfigurationException("Sources cannot be null or blank"));

    return new CalciteQueryEngineConfig(name, sources, config);
  }
}
