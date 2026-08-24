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

import io.cheshire.spi.query.engine.QueryEngineConfig;
import java.util.Map;

public record JdbcQueryEngineConfig(
    String name, Map<String, Object> sources, Map<String, Object> config)
    implements QueryEngineConfig {

  @Override
  public Map<String, Object> asMap() {
    // TODO: As of now only rebuild the original map ...
    return Map.of("name", name, "sources", sources, "config", config);
  }

  @Override
  public boolean validate() {
    // TODO: implement validation
    return name != null && !name.isBlank();
  }
}
