/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.query;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record SqlQuery(String query, Map<String, Object> parameters)
    implements CalciteLogicalQuery {
  /**
   * Returns the query parameters.
   *
   * @return a map of parameter names to values, or an empty map; never {@code null}
   */
  @Override
  public Map<String, Object> parameters() {
    return Optional.ofNullable(parameters).orElse(new HashMap<>());
  }

  /**
   * Checks whether this query has parameters.
   *
   * @return {@code true} if parameters are present, {@code false} otherwise
   */
  @Override
  public boolean hasParameters() {
    return !parameters().isEmpty();
  }
}
