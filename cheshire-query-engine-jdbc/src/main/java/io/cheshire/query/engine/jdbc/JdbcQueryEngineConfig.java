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
import java.util.List;
import java.util.Map;

public record JdbcQueryEngineConfig(String name, List<String> sources)
    implements QueryEngineConfig {

  public JdbcQueryEngineConfig {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Engine name cannot be null or blank");
    }
    // Normalize sources to immutable list
    sources = sources != null ? List.copyOf(sources) : List.of();
  }

  @Override
  public Map<String, Object> asMap() {
    return Map.of("name", name, "sources", sources);
  }

  @Override
  public boolean validate() {
    return name != null && !name.isBlank();
  }
}
