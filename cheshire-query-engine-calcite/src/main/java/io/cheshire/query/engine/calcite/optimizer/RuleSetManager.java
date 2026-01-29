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

public final class RuleSetManager {

  private final List<RelOptRule> rules;
  private final Convention convention;

  public RuleSetManager() {
    this.convention = Convention.NONE;
    this.rules = new ArrayList<>();
  }

  public RuleSetManager(List<RelOptRule> rules) {
    this.convention = Convention.NONE;
    this.rules = new ArrayList<>(rules);
  }

  public RuleSetManager(List<RelOptRule> rules, Convention convention) {
    this.convention = convention;
    this.rules = new ArrayList<>(rules);
  }

  public RuleSetManager addRule(RelOptRule rule) {
    if (rule != null && !rules.contains(rule)) {
      rules.add(rule);
    }
    return this;
  }

  public RuleSetManager addRules(List<RelOptRule> rules) {
    if (rules != null) {
      for (RelOptRule rule : rules) {
        addRule(rule);
      }
    }
    return this;
  }

  public RuleSetManager removeRule(RelOptRule rule) {
    rules.remove(rule);
    return this;
  }

  public RuleSetManager removeRulesByClass(Class<? extends RelOptRule> ruleClass) {
    rules.removeIf(rule -> ruleClass.isInstance(rule));
    return this;
  }

  public RuleSetManager removeRulesByPattern(String pattern) {
    rules.removeIf(rule -> rule.toString().contains(pattern));
    return this;
  }

  public RuleSetManager clear() {
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

  public RuleSetManager copy() {
    return new RuleSetManager(new ArrayList<>(rules), convention);
  }

  public RuleSetManager merge(RuleSetManager other) {
    List<RelOptRule> mergedRules = new ArrayList<>(this.rules);
    for (RelOptRule rule : other.rules) {
      if (!mergedRules.contains(rule)) {
        mergedRules.add(rule);
      }
    }
    return new RuleSetManager(mergedRules, this.convention);
  }

  // ========== Pre-built Rule Sets ==========

  public static RuleSetManager createFilterPushdownRuleSet() {
    return new RuleSetManager(
        Arrays.asList(
            CoreRules.FILTER_INTO_JOIN,
            CoreRules.FILTER_PROJECT_TRANSPOSE,
            CoreRules.FILTER_AGGREGATE_TRANSPOSE,
            CoreRules.FILTER_MERGE));
  }

  public static RuleSetManager createProjectionPushdownRuleSet() {
    return new RuleSetManager(
        Arrays.asList(
            CoreRules.PROJECT_MERGE,
            CoreRules.PROJECT_JOIN_TRANSPOSE,
            CoreRules.PROJECT_FILTER_TRANSPOSE,
            CoreRules.PROJECT_REMOVE));
  }

  public static RuleSetManager createJoinOptimizationRuleSet() {
    return new RuleSetManager(
        Arrays.asList(
            CoreRules.JOIN_CONDITION_PUSH,
            CoreRules.JOIN_EXTRACT_FILTER,
            CoreRules.JOIN_PUSH_TRANSITIVE_PREDICATES,
            CoreRules.JOIN_COMMUTE));
  }

  public static RuleSetManager createAggregateOptimizationRuleSet() {
    return new RuleSetManager(
        Arrays.asList(
            CoreRules.AGGREGATE_PROJECT_MERGE,
            CoreRules.AGGREGATE_REMOVE,
            CoreRules.FILTER_AGGREGATE_TRANSPOSE));
  }

  public static RuleSetManager createMinimalRuleSet() {
    return new RuleSetManager(Arrays.asList(CoreRules.FILTER_MERGE, CoreRules.PROJECT_MERGE));
  }

  public static RuleSetManager createEmptyRuleSet() {
    return new RuleSetManager(new ArrayList<>());
  }

  public static RuleSetManager createFederatedQueryRuleSet() {
    return new RuleSetManager(
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

  public static RuleSetManager createOLAPRuleSet() {
    return new RuleSetManager(
        Arrays.asList(
            CoreRules.AGGREGATE_PROJECT_MERGE,
            CoreRules.FILTER_AGGREGATE_TRANSPOSE,
            CoreRules.PROJECT_MERGE,
            CoreRules.FILTER_MERGE,
            CoreRules.SORT_REMOVE));
  }

  public static RuleSetManager createOLTPRuleSet() {
    return new RuleSetManager(
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

    public Builder withSafeDefaultRules() {
      // Use the carefully curated rules from createDefaultRules()
      RuleSetManager defaultSet = new RuleSetManager();
      return addRules(defaultSet.getMutableRules());
    }

    public Builder withAggressiveFilterRules() {
      // Include potentially problematic rules for advanced users
      return addRules(
          CoreRules.FILTER_INTO_JOIN, // May cause loops
          CoreRules.FILTER_MERGE,
          CoreRules.FILTER_AGGREGATE_TRANSPOSE,
          CoreRules.FILTER_PROJECT_TRANSPOSE);
    }

    public Builder withSafeFilterRules() {
      // Exclude problematic rules
      return addRules(
          CoreRules.FILTER_MERGE,
          CoreRules.FILTER_AGGREGATE_TRANSPOSE,
          CoreRules.FILTER_PROJECT_TRANSPOSE);
    }

    public RuleSetManager build() {
      return new RuleSetManager(rules, convention);
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
