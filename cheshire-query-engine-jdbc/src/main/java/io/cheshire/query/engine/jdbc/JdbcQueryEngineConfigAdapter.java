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

import io.cheshire.spi.query.engine.QueryEngineConfigAdapter;
import io.cheshire.spi.query.exception.QueryEngineConfigurationException;
import io.cheshire.spi.query.exception.QueryEngineException;
import java.util.List;
import java.util.Map;

public final class JdbcQueryEngineConfigAdapter
    implements QueryEngineConfigAdapter<JdbcQueryEngineConfig> {

  @Override
  public JdbcQueryEngineConfig adapt(Map<String, Object> config) throws QueryEngineException {

    // TODO: validate config cleanly here.
    if (config == null || config.isEmpty()) {
      throw new QueryEngineConfigurationException("Engine name cannot be null or blank");
    }

    // TODO: Make it type safe
    List<String> sources = (List<String>) config.getOrDefault("sources", List.of());

    return new JdbcQueryEngineConfig("jdbc", sources);
  }
}
