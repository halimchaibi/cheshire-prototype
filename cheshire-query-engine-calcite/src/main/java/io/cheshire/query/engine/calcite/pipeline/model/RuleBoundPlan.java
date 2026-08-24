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

import io.cheshire.query.engine.calcite.optimizer.RuleSetManager;
import java.util.Objects;
import org.apache.calcite.rel.RelNode;

public record RuleBoundPlan(RelNode node, RuleSetManager ruleSet) {
  public RuleBoundPlan {
    Objects.requireNonNull(node, "node");
    Objects.requireNonNull(ruleSet, "ruleSet");
  }
}
