/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.pipeline.model;

import java.util.Objects;
import org.apache.calcite.sql.SqlNode;

public record ParsedQuery(SqlNode node) {
  public ParsedQuery {
    Objects.requireNonNull(node, "node");
  }
}
