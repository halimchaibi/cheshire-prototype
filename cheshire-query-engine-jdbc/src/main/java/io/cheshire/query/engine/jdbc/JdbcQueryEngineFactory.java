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

import io.cheshire.spi.query.engine.QueryEngineFactory;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.exception.QueryEngineInitializationException;

public class JdbcQueryEngineFactory implements QueryEngineFactory<JdbcQueryEngineConfig> {

  public JdbcQueryEngineFactory() {
    // For ServiceLoader
  }

  @Override
  public Class<JdbcQueryEngineConfig> configType() {
    return JdbcQueryEngineConfig.class;
  }

  @Override
  public JdbcQueryEngine create(JdbcQueryEngineConfig config) throws QueryEngineException {
    try {
      validate(config);
      return new JdbcQueryEngine(config);
    } catch (Exception e) {
      throw new QueryEngineInitializationException(
          "Failed to create JdbcQueryEngine", config.name(), e);
    }
  }

  @Override
  public JdbcQueryEngineConfigAdapter adapter() {
    return new JdbcQueryEngineConfigAdapter();
  }

  @Override
  public void validate(JdbcQueryEngineConfig config) throws QueryEngineException {
    // TODO: validate config;
  }
}
