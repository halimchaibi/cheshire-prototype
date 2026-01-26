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
import java.util.stream.Collectors;
import org.apache.calcite.plan.Convention;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.tools.RuleSet;
import org.apache.calcite.tools.RuleSets;

public final class CustomRuleSet {

  private final List<RelOptRule> rules;
  private final Convention convention;
  private static final List<RelOptRule> DEFAULT_RULES;

  static {
    // ----------------------------------------------
    // Default logical rules (applied to all queries)
    // -----------------------------------------------
    // TODO: Guestestimate default rules. Requires review
    DEFAULT_RULES =
        List.of(
            CoreRules.FILTER_AGGREGATE_TRANSPOSE,
            CoreRules.FILTER_INTO_JOIN,
            CoreRules.PROJECT_JOIN_TRANSPOSE,
            // CoreRules.JOIN_COMMUTE);
            // CoreRules.JOIN_ASSOCIATE);
            CoreRules.AGGREGATE_PROJECT_MERGE,
            CoreRules.CALC_REDUCE_EXPRESSIONS,
            CoreRules.FILTER_REDUCE_EXPRESSIONS,
            CoreRules.PROJECT_REDUCE_EXPRESSIONS);
  }

  public CustomRuleSet() {
    this.convention = Convention.NONE;
    this.rules = createDefaultRules();
  }

  public CustomRuleSet(Convention convention) {
    this.convention = convention;
    this.rules = createDefaultRules();
  }

  public CustomRuleSet(List<RelOptRule> rules) {
    this.convention = Convention.NONE;
    this.rules = new ArrayList<>(rules);
  }

  public CustomRuleSet(List<RelOptRule> rules, Convention convention) {
    this.convention = convention;
    this.rules = new ArrayList<>(rules);
  }

  private List<RelOptRule> createDefaultRules() {
    List<RelOptRule> ruleList = new ArrayList<>();

    // Filter rules - optimize filter operations
    ruleList.addAll(
        Arrays.asList(
            CoreRules.FILTER_MERGE, // Combine multiple filters
            CoreRules.FILTER_AGGREGATE_TRANSPOSE, // Move filters before aggregates
            CoreRules.FILTER_PROJECT_TRANSPOSE // Move filters before projections
            // Note: FILTER_INTO_JOIN is excluded as it can cause infinite rewrite loops
            // when combined with JOIN_EXTRACT_FILTER and other join optimization rules
            ));

    // Projection rules - optimize projection operations
    ruleList.addAll(
        Arrays.asList(
            CoreRules.PROJECT_MERGE, // Combine multiple projections
            CoreRules.PROJECT_REMOVE // Remove unnecessary projections
            // Note: PROJECT_JOIN_TRANSPOSE is excluded as it can cause infinite rewrite loops
            // in complex join queries when combined with other optimization rules
            // Note: PROJECT_FILTER_TRANSPOSE is excluded to avoid conflicts with
            // FILTER_PROJECT_TRANSPOSE
            // Having both can cause infinite rewrite loops (StackOverflowError)
            ));

    // Join rules - optimize join operations
    ruleList.addAll(
        Arrays.asList(
            CoreRules.JOIN_CONDITION_PUSH, // Push join conditions down
            CoreRules.JOIN_EXTRACT_FILTER // Extract filters from joins
            // Note: JOIN_PUSH_TRANSITIVE_PREDICATES is excluded as it can cause issues
            // in complex join queries with multiple tables
            // Note: JOIN_COMMUTE is excluded as it can cause infinite rewrite loops
            // when combined with other join optimization rules, especially in complex queries
            ));

    // Aggregate rules - optimize aggregation operations
    ruleList.addAll(
        Arrays.asList(
            CoreRules.AGGREGATE_PROJECT_MERGE, // Merge aggregate with projection
            CoreRules.AGGREGATE_REMOVE // Remove unnecessary aggregates
            ));

    // Sort rules - optimize sort operations
    ruleList.addAll(
        Arrays.asList(
            CoreRules.SORT_REMOVE // Remove unnecessary sorts
            // Note: SORT_PROJECT_TRANSPOSE and SORT_JOIN_TRANSPOSE are excluded
            // as they can cause infinite rewrite loops in complex queries
            ));

    return ruleList;
  }

  public CustomRuleSet addRule(RelOptRule rule) {
    if (rule != null && !rules.contains(rule)) {
      rules.add(rule);
    }
    return this;
  }

  public CustomRuleSet addRules(List<RelOptRule> rules) {
    if (rules != null) {
      for (RelOptRule rule : rules) {
        addRule(rule);
      }
    }
    return this;
  }

  public CustomRuleSet removeRule(RelOptRule rule) {
    rules.remove(rule);
    return this;
  }

  public CustomRuleSet removeRulesByClass(Class<? extends RelOptRule> ruleClass) {
    rules.removeIf(rule -> ruleClass.isInstance(rule));
    return this;
  }

  public CustomRuleSet removeRulesByPattern(String pattern) {
    rules.removeIf(rule -> rule.toString().contains(pattern));
    return this;
  }

  public CustomRuleSet clear() {
    rules.clear();
    return this;
  }

  public List<RelOptRule> getRules() {
    return Collections.unmodifiableList(rules);
  }

  public List<RelOptRule> getMutableRules() {
    return new ArrayList<>(rules);
  }

  public RuleSet toRuleSet() {
    return RuleSets.ofList(rules);
  }

  public int size() {
    return rules.size();
  }

  public boolean isEmpty() {
    return rules.isEmpty();
  }

  public boolean contains(RelOptRule rule) {
    return rules.contains(rule);
  }

  public CustomRuleSet copy() {
    return new CustomRuleSet(new ArrayList<>(rules), convention);
  }

  public CustomRuleSet merge(CustomRuleSet other) {
    List<RelOptRule> mergedRules = new ArrayList<>(this.rules);
    for (RelOptRule rule : other.rules) {
      if (!mergedRules.contains(rule)) {
        mergedRules.add(rule);
      }
    }
    return new CustomRuleSet(mergedRules, this.convention);
  }

  // ========== Pre-built Rule Sets ==========

  public static CustomRuleSet createFilterPushdownRuleSet() {
    return new CustomRuleSet(
        Arrays.asList(
            CoreRules.FILTER_INTO_JOIN,
            CoreRules.FILTER_PROJECT_TRANSPOSE,
            CoreRules.FILTER_AGGREGATE_TRANSPOSE,
            CoreRules.FILTER_MERGE));
  }

  public static CustomRuleSet createProjectionPushdownRuleSet() {
    return new CustomRuleSet(
        Arrays.asList(
            CoreRules.PROJECT_MERGE,
            CoreRules.PROJECT_JOIN_TRANSPOSE,
            CoreRules.PROJECT_FILTER_TRANSPOSE,
            CoreRules.PROJECT_REMOVE));
  }

  public static CustomRuleSet createJoinOptimizationRuleSet() {
    return new CustomRuleSet(
        Arrays.asList(
            CoreRules.JOIN_CONDITION_PUSH,
            CoreRules.JOIN_EXTRACT_FILTER,
            CoreRules.JOIN_PUSH_TRANSITIVE_PREDICATES,
            CoreRules.JOIN_COMMUTE));
  }

  public static CustomRuleSet createAggregateOptimizationRuleSet() {
    return new CustomRuleSet(
        Arrays.asList(
            CoreRules.AGGREGATE_PROJECT_MERGE,
            CoreRules.AGGREGATE_REMOVE,
            CoreRules.FILTER_AGGREGATE_TRANSPOSE));
  }

  public static CustomRuleSet createMinimalRuleSet() {
    return new CustomRuleSet(Arrays.asList(CoreRules.FILTER_MERGE, CoreRules.PROJECT_MERGE));
  }

  public static CustomRuleSet createEmptyRuleSet() {
    return new CustomRuleSet(new ArrayList<>());
  }

  public static CustomRuleSet createFederatedQueryRuleSet() {
    return new CustomRuleSet(
        Arrays.asList(
            // Push filters to remote sources
            CoreRules.FILTER_INTO_JOIN,
            CoreRules.FILTER_PROJECT_TRANSPOSE,
            CoreRules.FILTER_MERGE,

            // Push projections to reduce data transfer
            CoreRules.PROJECT_MERGE,
            CoreRules.PROJECT_JOIN_TRANSPOSE,
            CoreRules.PROJECT_REMOVE,

            // Optimize joins
            CoreRules.JOIN_CONDITION_PUSH,
            CoreRules.JOIN_PUSH_TRANSITIVE_PREDICATES,

            // Remove unnecessary operations
            CoreRules.AGGREGATE_REMOVE));
  }

  public static CustomRuleSet createOLAPRuleSet() {
    return new CustomRuleSet(
        Arrays.asList(
            CoreRules.AGGREGATE_PROJECT_MERGE,
            CoreRules.FILTER_AGGREGATE_TRANSPOSE,
            CoreRules.PROJECT_MERGE,
            CoreRules.FILTER_MERGE,
            CoreRules.SORT_REMOVE));
  }

  public static CustomRuleSet createOLTPRuleSet() {
    return new CustomRuleSet(
        Arrays.asList(
            CoreRules.FILTER_MERGE, CoreRules.PROJECT_REMOVE, CoreRules.FILTER_PROJECT_TRANSPOSE));
  }

  // ========== Builder Pattern ==========

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final List<RelOptRule> rules;
    private Convention convention;

    public Builder() {
      this.rules = new ArrayList<>();
      this.convention = Convention.NONE;
    }

    public Builder withConvention(Convention convention) {
      this.convention = convention;
      return this;
    }

    public Builder addRule(RelOptRule rule) {
      if (rule != null && !rules.contains(rule)) {
        rules.add(rule);
      }
      return this;
    }

    public Builder addRules(RelOptRule... rules) {
      for (RelOptRule rule : rules) {
        addRule(rule);
      }
      return this;
    }

    public Builder addRules(List<RelOptRule> rules) {
      for (RelOptRule rule : rules) {
        addRule(rule);
      }
      return this;
    }

    public Builder withFilterRules() {
      return addRules(
          CoreRules.FILTER_INTO_JOIN,
          CoreRules.FILTER_MERGE,
          CoreRules.FILTER_AGGREGATE_TRANSPOSE,
          CoreRules.FILTER_PROJECT_TRANSPOSE);
    }

    public Builder withProjectionRules() {
      return addRules(
          CoreRules.PROJECT_MERGE,
          CoreRules.PROJECT_REMOVE,
          CoreRules.PROJECT_JOIN_TRANSPOSE,
          CoreRules.PROJECT_FILTER_TRANSPOSE);
    }

    public Builder withJoinRules() {
      return addRules(
          CoreRules.JOIN_CONDITION_PUSH,
          CoreRules.JOIN_EXTRACT_FILTER,
          CoreRules.JOIN_PUSH_TRANSITIVE_PREDICATES,
          CoreRules.JOIN_COMMUTE);
    }

    public Builder withAggregateRules() {
      return addRules(CoreRules.AGGREGATE_PROJECT_MERGE, CoreRules.AGGREGATE_REMOVE);
    }

    public Builder withSortRules() {
      return addRules(
          CoreRules.SORT_REMOVE, CoreRules.SORT_PROJECT_TRANSPOSE, CoreRules.SORT_JOIN_TRANSPOSE);
    }

    public Builder withDefaultRules() {
      return withFilterRules()
          .withProjectionRules()
          .withJoinRules()
          .withAggregateRules()
          .withSortRules();
    }

    public CustomRuleSet build() {
      return new CustomRuleSet(rules, convention);
    }
  }

  @Override
  public String toString() {
    return "CustomRuleSet{"
        + "ruleCount="
        + rules.size()
        + ", convention="
        + convention
        + ", rules="
        + rules.stream().map(r -> r.getClass().getSimpleName()).collect(Collectors.joining(", "))
        + '}';
  }
}
