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
import org.apache.calcite.rel.RelNode;

public record OptimizedPlan(RelNode node) {
  public OptimizedPlan {
    Objects.requireNonNull(node, "node");
  }
}
