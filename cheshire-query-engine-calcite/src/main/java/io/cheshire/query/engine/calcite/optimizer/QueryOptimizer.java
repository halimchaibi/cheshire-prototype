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
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.tools.FrameworkConfig;

// TODO: requires builder all this is messy
public class QueryOptimizer {

  private final HepPlanner planner;
  private final List<RelOptRule> rules;
  private final FrameworkConfig frameworkConfig;

  List<RelTraitDef> traitDefs =
      List.of(
          ConventionTraitDef.INSTANCE, RelCollationTraitDef.INSTANCE
          // Optional:
          // RelDistributionTraitDef.INSTANCE //TODO: Kept as reference for advanced use
          // cases(engine that support distribution)
          );

  RelOptCostFactory costFactory = RelOptCostImpl.FACTORY;

  // TODO: the addMatchLimit needs to be reviewed basically it supposed to prevent infinite loops,
  // when rules are conflicting
  private static final int MAX_ITERATIONS = 1000;

  private QueryOptimizer() {
    // TODO: requires builder all this is messy
    planner = null;
    rules = List.of();
    frameworkConfig = null;
  }

  public QueryOptimizer(FrameworkConfig config) {
    this.frameworkConfig = config;

    this.rules = CustomRuleSet.builder().build().getRules();
    HepProgram program =
        new HepProgramBuilder().addRuleCollection(rules).addMatchLimit(MAX_ITERATIONS).build();
    this.planner = new HepPlanner(program);
  }

  public QueryOptimizer(List<RelOptRule> rules) {
    this.rules = Objects.requireNonNull(rules, "Rules cannot be null");
    HepProgram program =
        new HepProgramBuilder().addRuleCollection(rules).addMatchLimit(MAX_ITERATIONS).build();
    this.planner = new HepPlanner(program);

    // TODO: requires builder all this is messy
    frameworkConfig = null;
  }

  public RelNode optimize(RelNode logicalPlan) throws OptimizationException {
    return optimize(logicalPlan, null);
  }

  public RelNode optimizeWithVolcano(RelNode logicalRoot) {

    List<RelOptRule> rules = CustomRuleSet.builder().build().getRules();
    Context context = frameworkConfig.getContext();

    // TODO This had been set in FrameworkInitializer, requires to Check what is really the purpose
    // of the Context
    // It might be very help full to wrap any contexts on in it and get what is required when needed
    // by calling using
    // ataContext dataContext = context.unwrap(DataContext.class);

    //    DataContext dataContext = new CalciteDataContext(schemaManager);
    //    PlannerContext plannerContext = new PlannerContext(dataContext);

    VolcanoPlanner planner = new VolcanoPlanner(costFactory, context);

    traitDefs.forEach(planner::addRelTraitDef);

    rules.forEach(planner::addRule);

    planner.setRoot(logicalRoot);

    RelTraitSet desiredTraits = logicalRoot.getTraitSet().replace(EnumerableConvention.INSTANCE);

    RelNode physicalRoot = planner.changeTraits(logicalRoot, desiredTraits);

    planner.setRoot(physicalRoot);

    return planner.findBestExp();
  }

  public RelNode optimize(RelNode logicalPlan, CustomRuleSet ruleSet) throws OptimizationException {
    Objects.requireNonNull(logicalPlan, "Logical plan cannot be null");

    // TODO: Use a query scoped planner
    HepPlanner plannerToUse = planner;

    if (ruleSet != null) {
      List<RelOptRule> customRules = ruleSet.getRules();
      HepProgram program =
          new HepProgramBuilder().addRuleCollection(customRules).addMatchLimit(1000).build();
      plannerToUse = new HepPlanner(program);
    }

    try {
      plannerToUse.setRoot(logicalPlan);
      return plannerToUse.findBestExp();
    } catch (Exception e) {
      throw new OptimizationException("Query optimization failed: " + e.getMessage(), e);
    } catch (StackOverflowError e) {
      // Protect against planner rule oscillation (infinite rewrite loops).
      throw new OptimizationException(
          "Query optimization failed due to a StackOverflowError. "
              + "This often happens when conflicting transformation rules (e.g. both project<->filter transpose rules) "
              + "are enabled and keep re-writing the plan. Consider removing one of the transpose rules.",
          e);
    } finally {
      plannerToUse.clear();
    }
  }

  public String explain(RelNode logicalPlan) throws OptimizationException {
    RelNode optimized = optimize(logicalPlan);
    return optimized.toString();
  }

  public List<RelOptRule> getRules() {
    return new ArrayList<>(rules);
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
