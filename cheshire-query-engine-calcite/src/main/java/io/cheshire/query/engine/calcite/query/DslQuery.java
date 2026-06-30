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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DslQuery(List<String> select, String from, Map<String, Object> parameters)
    implements CalciteLogicalQuery {

  public DslQuery {
    select = normalizeSelect(select);
    from = Objects.requireNonNull(from, "from must not be null").trim();
    parameters = Map.copyOf(Objects.requireNonNull(parameters, "parameters must not be null"));
  }

  public static DslQuery select(String projection) {
    return new DslQuery(List.of(projection), "", Map.of());
  }

  public DslQuery from(String source) {
    return new DslQuery(select, source, parameters);
  }

  public String toSql() {
    final String projection = String.join(", ", select);
    return from.isBlank() ? "SELECT " + projection : "SELECT " + projection + " FROM " + from;
  }

  @Override
  public Map<String, Object> query() {
    final Map<String, Object> dsl = new LinkedHashMap<>();
    dsl.put("select", select);
    if (!from.isBlank()) {
      dsl.put("from", from);
    }
    return Map.copyOf(dsl);
  }

  @Override
  public boolean hasParameters() {
    return !parameters.isEmpty();
  }

  private static List<String> normalizeSelect(List<String> select) {
    final List<String> projections =
        Objects.requireNonNull(select, "select must not be null").stream()
            .map(projection -> Objects.requireNonNull(projection, "projection must not be null"))
            .map(String::trim)
            .toList();

    if (projections.isEmpty() || projections.stream().anyMatch(String::isBlank)) {
      throw new IllegalArgumentException("select must contain at least one non-blank projection");
    }

    return List.copyOf(projections);
  }
}
