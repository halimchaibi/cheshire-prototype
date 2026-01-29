/*-
 * #%L
 * Cheshire :: Query Engine :: JDBC
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.jdbc;

import io.cheshire.common.utils.MapUtils;
import io.cheshire.spi.query.engine.QueryEngineConfigAdapter;
import io.cheshire.spi.query.exception.QueryEngineConfigurationException;
import io.cheshire.spi.query.exception.QueryEngineException;
import java.util.Map;

public final class JdbcQueryEngineConfigAdapter
    implements QueryEngineConfigAdapter<JdbcQueryEngineConfig> {

  @Override
  public JdbcQueryEngineConfig adapt(Map<String, Object> engineConfig) throws QueryEngineException {

    // TODO: validate config cleanly here.
    String name =
        MapUtils.someValueFromMapAs(engineConfig, "name", String.class)
            .orElseThrow(
                () -> new QueryEngineConfigurationException("Source name cannot be null or blank"));

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

    return new JdbcQueryEngineConfig(name, sources, config);
  }
}
