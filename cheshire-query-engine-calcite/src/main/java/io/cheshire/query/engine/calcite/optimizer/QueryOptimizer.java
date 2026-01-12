package io.cheshire.query.engine.calcite.optimizer;

import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Query Optimizer component that optimizes logical plans for better performance.
 * <p>
 * This optimizer uses Apache Calcite's HepPlanner to apply transformation rules:
 * - Predicate pushdown
 * - Projection pushdown
 * - Join reordering
 * - Constant folding
 * - Column pruning
 */
@Slf4j
public class QueryOptimizer {

    private final HepPlanner planner;
    private final List<RelOptRule> defaultRules;

    /**
     * Creates a new query optimizer with default optimization rules.
     */
    public QueryOptimizer() {
        this.defaultRules = createDefaultRules();
        HepProgram program = new HepProgramBuilder()
                .addRuleCollection(defaultRules)
                .addMatchLimit(1000)
                .build();
        this.planner = new HepPlanner(program);
    }

    /**
     * Creates a new query optimizer with custom rules.
     */
    public QueryOptimizer(List<RelOptRule> rules) {
        this.defaultRules = Objects.requireNonNull(rules, "Rules cannot be null");
        HepProgram program = new HepProgramBuilder()
                .addRuleCollection(defaultRules)
                .addMatchLimit(1000)
                .build();
        this.planner = new HepPlanner(program);
    }

    /**
     * Optimizes a logical plan using the default rule set.
     *
     * @param logicalPlan the logical plan to optimize
     * @return the optimized logical plan
     * @throws OptimizationException if optimization fails
     */
    public RelNode optimize(RelNode logicalPlan) throws OptimizationException {
        return optimize(logicalPlan, null);
    }

    /**
     * Optimizes a logical plan using a specific rule set.
     * If ruleSet is null, uses the default rule set configured for this optimizer.
     *
     * @param logicalPlan the logical plan to optimize
     * @param ruleSet     the custom rule set to use, or null to use default
     * @return the optimized logical plan
     * @throws OptimizationException if optimization fails
     */
    public RelNode optimize(RelNode logicalPlan, CustomRuleSet ruleSet) throws OptimizationException {
        Objects.requireNonNull(logicalPlan, "Logical plan cannot be null");

        HepPlanner plannerToUse = planner;

        // If a custom rule set is provided, create a temporary planner for it
        if (ruleSet != null) {
            List<RelOptRule> customRules = ruleSet.getRules();
            HepProgram program = new HepProgramBuilder()
                    .addRuleCollection(customRules)
                    .addMatchLimit(1000)
                    .build();
            plannerToUse = new HepPlanner(program);
        }

        try {
            plannerToUse.setRoot(logicalPlan);
            RelNode optimizedPlan = plannerToUse.findBestExp();
            return optimizedPlan;
        } catch (Exception e) {
            throw new OptimizationException("Query optimization failed: " + e.getMessage(), e);
        } catch (StackOverflowError e) {
            // Protect against planner rule oscillation (infinite rewrite loops).
            throw new OptimizationException("Query optimization failed due to a StackOverflowError. " +
                    "This often happens when conflicting transformation rules are enabled. " +
                    "Consider removing conflicting rules.", e);
        } finally {
            plannerToUse.clear();
        }
    }

    /**
     * Gets a string representation of the optimized plan.
     *
     * @param logicalPlan the logical plan to optimize
     * @return string representation of the optimized plan
     * @throws OptimizationException if optimization fails
     */
    public String explainOptimizedPlan(RelNode logicalPlan) throws OptimizationException {
        RelNode optimized = optimize(logicalPlan);
        return optimized.toString();
    }

    /**
     * Creates default optimization rules.
     */
    private List<RelOptRule> createDefaultRules() {
        List<RelOptRule> defaultRules = new ArrayList<>();

        // Core optimization rules
        defaultRules.add(org.apache.calcite.rel.rules.CoreRules.FILTER_AGGREGATE_TRANSPOSE);
        defaultRules.add(org.apache.calcite.rel.rules.CoreRules.FILTER_INTO_JOIN);
        defaultRules.add(org.apache.calcite.rel.rules.CoreRules.PROJECT_JOIN_TRANSPOSE);
        defaultRules.add(org.apache.calcite.rel.rules.CoreRules.AGGREGATE_PROJECT_MERGE);
        defaultRules.add(org.apache.calcite.rel.rules.CoreRules.CALC_REDUCE_EXPRESSIONS);
        defaultRules.add(org.apache.calcite.rel.rules.CoreRules.FILTER_REDUCE_EXPRESSIONS);
        defaultRules.add(org.apache.calcite.rel.rules.CoreRules.PROJECT_REDUCE_EXPRESSIONS);

        return defaultRules;
    }

    /**
     * Gets the list of optimization rules being used.
     */
    public List<RelOptRule> getRules() {
        return new ArrayList<>(defaultRules);
    }

    /**
     * Exception thrown when query optimization fails.
     */
    public static class OptimizationException extends Exception {
        public OptimizationException(String message) {
            super(message);
        }

        public OptimizationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

