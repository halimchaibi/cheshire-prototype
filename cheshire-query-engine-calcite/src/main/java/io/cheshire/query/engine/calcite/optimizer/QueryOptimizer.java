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

import io.cheshire.query.engine.calcite.OptimizationContext;
import io.cheshire.query.engine.calcite.query.QueryType;
import io.cheshire.spi.query.exception.QueryExecutionException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.plan.*;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.plan.volcano.VolcanoPlanner;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelWriter;
import org.apache.calcite.rel.externalize.RelWriterImpl;
import org.apache.calcite.tools.FrameworkConfig;

/**
 * Query optimizer that applies optimization rules based on query characteristics and schema
 * metadata. Supports both heuristic (HepPlanner) and cost-based (VolcanoPlanner) optimization
 * strategies.
 */
public class QueryOptimizer {

  private final FrameworkConfig frameworkConfig;
  private final QueryOptimizer.RuleSelector ruleSelector;

  private static final int DEFAULT_MAX_ITERATIONS = 1000;
  private static final int COMPLEX_QUERY_MAX_ITERATIONS = 2000;

  private QueryOptimizer(QueryOptimizer.Builder builder) {
    this.frameworkConfig = Objects.requireNonNull(builder.frameworkConfig, "frameworkConfig");
    this.ruleSelector =
        builder.ruleSelector != null ? builder.ruleSelector : new DefaultRuleSelector();
  }

  /** Optimize using heuristic planner with query-specific rules */
  public RelNode optimize(RelNode logicalPlan, OptimizationContext context)
      throws QueryExecutionException {
    try {
      // Select rules based on query characteristics
      RuleSetManager ruleSet = ruleSelector.selectRules(logicalPlan, context);

      // Determine iteration limit based on query complexity
      int maxIterations = determineMaxIterations(logicalPlan, context);

      HepProgram program =
          new HepProgramBuilder()
              .addRuleCollection(ruleSet.getRules())
              .addMatchLimit(maxIterations)
              .build();

      HepPlanner planner = new HepPlanner(program);
      planner.setRoot(logicalPlan);
      RelNode optimized = planner.findBestExp();
      planner.clear();

      return optimized;
    } catch (Exception e) {
      throw new QueryExecutionException("Heuristic optimization failed: " + e.getMessage(), e);
    }
  }

  /** Optimize using cost-based volcano planner with query-specific rules */
  public RelNode optimizeWithVolcano(RelNode logicalPlan, OptimizationContext context)
      throws QueryExecutionException {
    try {
      // Select rules based on query characteristics
      RuleSetManager ruleSet = ruleSelector.selectRules(logicalPlan, context);

      Context plannerContext = frameworkConfig.getContext();
      VolcanoPlanner planner = new VolcanoPlanner(frameworkConfig.getCostFactory(), plannerContext);

      // Add selected rules to planner
      ruleSet.getRules().forEach(planner::addRule);

      // Set root and convert to physical plan
      planner.setRoot(logicalPlan);
      RelTraitSet desiredTraits = logicalPlan.getTraitSet().replace(EnumerableConvention.INSTANCE);
      RelNode physicalRoot = planner.changeTraits(logicalPlan, desiredTraits);
      planner.setRoot(physicalRoot);

      return planner.findBestExp();
    } catch (Exception e) {
      throw new QueryExecutionException("Cost-based optimization failed: " + e.getMessage(), e);
    }
  }

  /** Determine max iterations based on query complexity */
  private int determineMaxIterations(RelNode plan, OptimizationContext context) {
    // Count nodes in the plan
    int nodeCount = countNodes(plan);

    // Adjust based on query type
    if (context.getQueryType() == QueryType.OLAP || nodeCount > 20) {
      return COMPLEX_QUERY_MAX_ITERATIONS;
    }

    return DEFAULT_MAX_ITERATIONS;
  }

  private int countNodes(RelNode node) {
    int count = 1;
    for (RelNode input : node.getInputs()) {
      count += countNodes(input);
    }
    return count;
  }

  public String explain(RelNode logicalPlan, OptimizationContext context)
      throws QueryExecutionException {
    RelNode optimized = optimize(logicalPlan, context);
    try (StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw)) {
      RelWriter planWriter = new RelWriterImpl(pw);
      optimized.explain(planWriter);
      pw.flush();
      return sw.toString();
    } catch (IOException e) {
      throw new RuntimeException("Failed to render explain plan", e);
    }
  }

  //  public RelNode optimize(RelNode logicalPlan) throws QueryExecutionException {
  //    try {
  //      HepProgram program =
  //          new HepProgramBuilder()
  //              .addRuleCollection(this.defaultRules)
  //              .addMatchLimit(MAX_ITERATIONS)
  //              .build();
  //      HepPlanner planner = new HepPlanner(program);
  //
  //      planner.setRoot(logicalPlan);
  //      RelNode relNode = planner.findBestExp();
  //      planner.clear();
  //      return relNode;
  //    } catch (Exception e) {
  //      throw new QueryExecutionException("Query optimization failed: " + e.getMessage(), e);
  //    }
  //  }

  //  public RelNode optimizeWithVolcano(RelNode logicalRoot) {
  //
  //    List<RelOptRule> rules = RuleSetManager.builder().withDefaultRules().build().getRules();
  //    Context context = frameworkConfig.getContext();
  //
  //    frameworkConfig.getCostFactory();
  //    VolcanoPlanner planner = new VolcanoPlanner(frameworkConfig.getCostFactory(), context);
  //
  //    rules.forEach(planner::addRule);
  //    planner.setRoot(logicalRoot);
  //    RelTraitSet desiredTraits =
  // logicalRoot.getTraitSet().replace(EnumerableConvention.INSTANCE);
  //    RelNode physicalRoot = planner.changeTraits(logicalRoot, desiredTraits);
  //    planner.setRoot(physicalRoot);
  //    return planner.findBestExp();
  //  }

  //  public String explain(RelNode logicalPlan) throws QueryExecutionException {
  //    RelNode optimized = optimize(logicalPlan);
  //    return optimized.toString();
  //  }

  public static QueryOptimizer.Builder builder() {
    return new QueryOptimizer.Builder();
  }

  public static class Builder {
    private FrameworkConfig frameworkConfig;
    private QueryOptimizer.RuleSelector ruleSelector;

    public QueryOptimizer.Builder withFrameworkConfig(FrameworkConfig config) {
      this.frameworkConfig = config;
      return this;
    }

    public QueryOptimizer.Builder withRuleSelector(QueryOptimizer.RuleSelector selector) {
      this.ruleSelector = selector;
      return this;
    }

    public QueryOptimizer build() {
      return new QueryOptimizer(this);
    }
  }

  /**
   * Strategy interface for selecting optimization rules based on query characteristics and schema
   * metadata.
   */
  @FunctionalInterface
  public static interface RuleSelector {

    /**
     * Select an appropriate rule set for the given query plan and context
     *
     * @param logicalPlan The logical query plan
     * @param context Query-specific context information
     * @return A RuleSetManager with selected rules
     */
    RuleSetManager selectRules(RelNode logicalPlan, OptimizationContext context);
  }
}
