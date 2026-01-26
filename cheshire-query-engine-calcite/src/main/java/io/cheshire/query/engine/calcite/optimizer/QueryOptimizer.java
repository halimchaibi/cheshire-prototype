/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.optimizer;

import java.util.*;
import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.plan.*;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.tools.FrameworkConfig;

// TODO: requires builder all this is messy
public class QueryOptimizer {

  private final List<RelOptRule> defaultRules;
  private final FrameworkConfig frameworkConfig;

  // TODO: the addMatchLimit needs to be reviewed basically it supposed to prevent infinite loops,
  // when rules are conflicting
  private static final int MAX_ITERATIONS = 1000;

  private QueryOptimizer() {
    defaultRules = List.of();
    frameworkConfig = null;
  }

  public QueryOptimizer(FrameworkConfig config) {
    this.frameworkConfig = config;
    this.defaultRules = RuleSetManager.builder().build().getRules();
  }

  public RelNode optimize(RelNode logicalPlan) throws OptimizationException {
    try {
      HepProgram program =
          new HepProgramBuilder()
              .addRuleCollection(this.defaultRules)
              .addMatchLimit(MAX_ITERATIONS)
              .build();
      HepPlanner planner = new HepPlanner(program);

      planner.setRoot(logicalPlan);
      RelNode relNode = planner.findBestExp();
      planner.clear();
      return relNode;
    } catch (Exception e) {
      throw new OptimizationException("Query optimization failed: " + e.getMessage(), e);
    }
  }

  public RelNode optimizeWithVolcano(RelNode logicalRoot) {

    List<RelOptRule> rules = RuleSetManager.builder().withDefaultRules().build().getRules();
    Context context = frameworkConfig.getContext();

    frameworkConfig.getCostFactory();
    VolcanoPlanner planner = new VolcanoPlanner(frameworkConfig.getCostFactory(), context);

    rules.forEach(planner::addRule);
    planner.setRoot(logicalRoot);
    RelTraitSet desiredTraits = logicalRoot.getTraitSet().replace(EnumerableConvention.INSTANCE);
    RelNode physicalRoot = planner.changeTraits(logicalRoot, desiredTraits);
    planner.setRoot(physicalRoot);
    return planner.findBestExp();
  }

  public String explain(RelNode logicalPlan) throws OptimizationException {
    RelNode optimized = optimize(logicalPlan);
    return optimized.toString();
  }

  public static class OptimizationException extends Exception {
    public OptimizationException(String message) {
      super(message);
    }

    public OptimizationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
