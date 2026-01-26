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
import java.util.Map;

public record CalciteQueryEngineConfig(
    String name, Map<String, Object> config, Map<String, Object> sources)
    implements QueryEngineConfig {

  @Override
  public boolean validate() {
    // TODO: Validation logic placeholder
    return name != null && !name.isBlank() && config != null;
  }

  @Override
  public Map<String, Object> asMap() {
    // TODO: convert to typed map for serialization
    return config;
  }
}
