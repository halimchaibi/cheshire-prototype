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

import org.apache.calcite.rel.core.*;

/**
 * Default rule selector that chooses optimization rules based on query type, schema
 * characteristics, and query complexity.
 */

// TODO: analyze and delete this class, handled by RuleSetBuilder
public class DefaultRuleSelector {
  //
  //  public RuleSetManager selectRules(RelNode logicalPlan, OptimizationContext context) {
  //
  //    // Start with base rule set
  //    RuleSetManager.Builder builder = RuleSetManager.builder();
  //
  //    // Analyze the plan structure to understand what's needed
  //    PlanAnalysis analysis = analyzePlan(logicalPlan);
  //
  //    // Select rules based on query type
  //    switch (context.getQueryType()) {
  //      case OLAP:
  //        return selectOLAPRules(builder, analysis, context);
  //      case OLTP:
  //        return selectOLTPRules(builder, analysis, context);
  //      case FEDERATED:
  //        return selectFederatedRules(builder, analysis, context);
  //      case SIMPLE_SELECT:
  //        return selectSimpleRules(builder, analysis, context);
  //      case COMPLEX_JOIN:
  //        return selectJoinHeavyRules(builder, analysis, context);
  //      default:
  //        return selectDefaultRules(builder, analysis, context);
  //    }
  //  }
  //
  //  private RuleSetManager selectOLAPRules(
  //      RuleSetManager.Builder builder, PlanAnalysis analysis, OptimizationContext context) {
  //
  //    // OLAP queries benefit from aggressive aggregation and filter pushdown
  //    builder.withAggregateRules();
  //
  //    if (analysis.hasFilters) {
  //      builder.withSafeFilterRules(); // Push filters early
  //    }
  //
  //    if (analysis.hasProjections) {
  //      builder.addRules(
  //          org.apache.calcite.rel.rules.CoreRules.PROJECT_MERGE,
  //          org.apache.calcite.rel.rules.CoreRules.PROJECT_REMOVE);
  //    }
  //
  //    // For remote schemas, be more aggressive with pushdown
  //    if (context.isRemoteSchemaInvolved()) {
  //      builder.addRule(org.apache.calcite.rel.rules.CoreRules.FILTER_AGGREGATE_TRANSPOSE);
  //    }
  //
  //    return builder.build();
  //  }
  //
  //  private RuleSetManager selectOLTPRules(
  //      RuleSetManager.Builder builder, PlanAnalysis analysis, OptimizationContext context) {
  //
  //    // OLTP queries should be kept simple - focus on filter pushdown
  //    builder.withSafeFilterRules();
  //
  //    if (analysis.hasProjections) {
  //      builder.addRules(
  //          org.apache.calcite.rel.rules.CoreRules.PROJECT_REMOVE,
  //          org.apache.calcite.rel.rules.CoreRules.PROJECT_MERGE);
  //    }
  //
  //    // Avoid expensive join reordering for OLTP
  //    if (analysis.hasJoins && analysis.joinCount <= 2) {
  //      builder.addRule(org.apache.calcite.rel.rules.CoreRules.JOIN_CONDITION_PUSH);
  //    }
  //
  //    return builder.build();
  //  }
  //
  //  private RuleSetManager selectFederatedRules(
  //      RuleSetManager.Builder builder, PlanAnalysis analysis, OptimizationContext context) {
  //
  //    // Federated queries need aggressive pushdown to reduce network transfer
  //    builder
  //        .withSafeFilterRules()
  //        .addRules(
  //            org.apache.calcite.rel.rules.CoreRules.PROJECT_MERGE,
  //            org.apache.calcite.rel.rules.CoreRules.PROJECT_REMOVE,
  //            org.apache.calcite.rel.rules.CoreRules.AGGREGATE_PROJECT_MERGE);
  //
  //    if (analysis.hasJoins) {
  //      // Be careful with join rules in federated context
  //      builder.addRules(
  //          org.apache.calcite.rel.rules.CoreRules.JOIN_CONDITION_PUSH,
  //          org.apache.calcite.rel.rules.CoreRules.JOIN_EXTRACT_FILTER);
  //    }
  //
  //    return builder.build();
  //  }
  //
  //  private RuleSetManager selectSimpleRules(
  //      RuleSetManager.Builder builder, PlanAnalysis analysis, OptimizationContext context) {
  //
  //    // Simple queries need minimal optimization
  //    builder.addRules(
  //        org.apache.calcite.rel.rules.CoreRules.FILTER_MERGE,
  //        org.apache.calcite.rel.rules.CoreRules.PROJECT_MERGE,
  //        org.apache.calcite.rel.rules.CoreRules.PROJECT_REMOVE);
  //
  //    return builder.build();
  //  }
  //
  //  private RuleSetManager selectJoinHeavyRules(
  //      RuleSetManager.Builder builder, PlanAnalysis analysis, OptimizationContext context) {
  //
  //    // Join-heavy queries need careful rule selection to avoid infinite loops
  //    builder.withSafeFilterRules();
  //
  //    // Add safe join rules only
  //    builder.addRules(
  //        org.apache.calcite.rel.rules.CoreRules.JOIN_CONDITION_PUSH,
  //        org.apache.calcite.rel.rules.CoreRules.JOIN_EXTRACT_FILTER);
  //
  //    // Only add JOIN_PUSH_TRANSITIVE_PREDICATES for simple cases
  //    if (analysis.joinCount <= 3) {
  //      builder.addRule(org.apache.calcite.rel.rules.CoreRules.JOIN_PUSH_TRANSITIVE_PREDICATES);
  //    }
  //
  //    if (analysis.hasProjections) {
  //      builder.addRules(
  //          org.apache.calcite.rel.rules.CoreRules.PROJECT_MERGE,
  //          org.apache.calcite.rel.rules.CoreRules.PROJECT_REMOVE);
  //    }
  //
  //    return builder.build();
  //  }
  //
  //  private RuleSetManager selectDefaultRules(
  //      RuleSetManager.Builder builder, PlanAnalysis analysis, OptimizationContext context) {
  //
  //    // Use the safe default rules from RuleSetManager
  //    return new RuleSetManager();
  //  }
  //
  //  /** Analyze the logical plan to determine its characteristics */
  //  private PlanAnalysis analyzePlan(RelNode plan) {
  //    PlanAnalysis analysis = new PlanAnalysis();
  //    analyzeNode(plan, analysis);
  //    return analysis;
  //  }
  //
  //  private void analyzeNode(RelNode node, PlanAnalysis analysis) {
  //    // Check node type
  //    if (node instanceof Filter) {
  //      analysis.hasFilters = true;
  //    } else if (node instanceof Project) {
  //      analysis.hasProjections = true;
  //    } else if (node instanceof Join) {
  //      analysis.hasJoins = true;
  //      analysis.joinCount++;
  //    } else if (node instanceof Aggregate) {
  //      analysis.hasAggregates = true;
  //    } else if (node instanceof Sort) {
  //      analysis.hasSorts = true;
  //    } else if (node instanceof Window) {
  //      analysis.hasWindows = true;
  //    }
  //
  //    // Recursively analyze children
  //    for (RelNode input : node.getInputs()) {
  //      analyzeNode(input, analysis);
  //    }
  //  }
  //
  //  /** Internal class to hold plan analysis results */
  //  private static class PlanAnalysis {
  //    boolean hasFilters = false;
  //    boolean hasProjections = false;
  //    boolean hasJoins = false;
  //    boolean hasAggregates = false;
  //    boolean hasSorts = false;
  //    boolean hasWindows = false;
  //    int joinCount = 0;
  //  }
}
