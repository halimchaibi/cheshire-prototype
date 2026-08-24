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

import io.cheshire.query.engine.calcite.query.QueryType;
import io.cheshire.spi.query.exception.QueryExecutionException;
import java.util.List;
import java.util.Objects;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.tools.FrameworkConfig;

/**
 * Applies heuristic (Hep) optimization using the query-scoped rule set. Identity optimization is
 * not the intended path — callers should supply a non-empty rule program whenever possible.
 */
public final class QueryOptimizer {

  private static final int DEFAULT_MAX_ITERATIONS = 1000;
  private static final int COMPLEX_QUERY_MAX_ITERATIONS = 2000;

  private final FrameworkConfig frameworkConfig;

  private QueryOptimizer(final FrameworkConfig frameworkConfig) {
    this.frameworkConfig = frameworkConfig;
  }

  /**
   * Optimizes {@code plan} with rules from {@code ruleSet}.
   *
   * @param plan logical plan from CONVERT
   * @param ruleSet query-scoped rules from SELECT_RULES
   * @param runtime query runtime context (reserved for future cost/hint wiring)
   * @return optimized relational plan
   */
  public RelNode optimize(
      final RelNode plan, final RuleSetManager ruleSet, final QueryRuntimeContext runtime)
      throws QueryExecutionException {
    Objects.requireNonNull(plan, "plan");
    Objects.requireNonNull(ruleSet, "ruleSet");
    Objects.requireNonNull(runtime, "runtime");

    try {
      final List<RelOptRule> rules = ruleSet.getRules();
      if (rules.isEmpty()) {
        return plan;
      }

      final HepProgramBuilder programBuilder = new HepProgramBuilder();
      rules.forEach(programBuilder::addRuleInstance);
      final HepProgram program = programBuilder.build();

      final HepPlanner hepPlanner = new HepPlanner(program);
      hepPlanner.setRoot(plan);
      return hepPlanner.findBestExp();
    } catch (Exception e) {
      throw new QueryExecutionException("Query optimization failed", e);
    }
  }

  int maxIterations(final RelNode plan, final OptimizationContext context) {
    final int nodeCount = countNodes(plan);
    if (context.getQueryType() == QueryType.OLAP || nodeCount > 20) {
      return COMPLEX_QUERY_MAX_ITERATIONS;
    }
    return DEFAULT_MAX_ITERATIONS;
  }

  private int countNodes(final RelNode node) {
    int count = 1;
    for (final RelNode input : node.getInputs()) {
      count += countNodes(input);
    }
    return count;
  }

  FrameworkConfig frameworkConfig() {
    return frameworkConfig;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private FrameworkConfig frameworkConfig;

    public Builder withFrameworkConfig(final FrameworkConfig config) {
      this.frameworkConfig = config;
      return this;
    }

    public QueryOptimizer build() {
      return new QueryOptimizer(frameworkConfig);
    }
  }
}
