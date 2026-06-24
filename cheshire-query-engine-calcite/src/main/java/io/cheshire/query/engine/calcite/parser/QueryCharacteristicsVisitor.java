/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.parser;

import io.cheshire.query.engine.calcite.query.QueryCharacteristics;
import java.util.HashSet;
import java.util.Set;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.util.SqlBasicVisitor;

public class QueryCharacteristicsVisitor extends SqlBasicVisitor<Void> {
  private boolean hasJoins = false;
  private boolean hasAggregations = false;
  private final Set<String> tables = new HashSet<>();

  @Override
  public Void visit(SqlCall call) {
    if (call instanceof SqlJoin) {
      hasJoins = true;
    }

    if (call instanceof SqlSelect select) {
      if (select.getGroup() != null && !select.getGroup().isEmpty()) {
        this.hasAggregations = true;
      }
    }

    if (call.getKind().belongsTo(SqlKind.AGGREGATE)) {
      hasAggregations = true;
    }

    return super.visit(call);
  }

  public QueryCharacteristics buildCharacteristics() {
    return QueryCharacteristics.builder()
        .withJoins(hasJoins)
        .withAggregations(hasAggregations)
        .withTableCount(tables.size())
        .build();
  }
}
