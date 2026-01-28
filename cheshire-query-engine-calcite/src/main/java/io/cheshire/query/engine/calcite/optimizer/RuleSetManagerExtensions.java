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

/**
 * Extension methods for RuleSetManager.Builder to add safe rule sets that avoid problematic rule
 * combinations.
 *
 * <p>Add these methods to your RuleSetManager.Builder class:
 */
public class RuleSetManagerExtensions {

  /** Add to RuleSetManager.Builder class: */
  public static void extendBuilder() {
    // This is a template showing what methods to add to Builder class
  }

  /*
  // Add these methods to RuleSetManager.Builder:

  public Builder withSafeFilterRules() {
    // Exclude FILTER_INTO_JOIN which can cause infinite loops
    return addRules(
        CoreRules.FILTER_MERGE,
        CoreRules.FILTER_AGGREGATE_TRANSPOSE,
        CoreRules.FILTER_PROJECT_TRANSPOSE);
  }

  public Builder withSafeProjectionRules() {
    // Exclude PROJECT_JOIN_TRANSPOSE and PROJECT_FILTER_TRANSPOSE
    // which can conflict and cause loops
    return addRules(
        CoreRules.PROJECT_MERGE,
        CoreRules.PROJECT_REMOVE);
  }

  public Builder withSafeJoinRules() {
    // Exclude JOIN_COMMUTE which can cause infinite loops
    return addRules(
        CoreRules.JOIN_CONDITION_PUSH,
        CoreRules.JOIN_EXTRACT_FILTER);
  }

  public Builder withSafeSortRules() {
    // Only include SORT_REMOVE, exclude transpose rules
    return addRules(CoreRules.SORT_REMOVE);
  }

  public Builder withSafeDefaultRules() {
    // A complete set of safe rules that work well together
    return withSafeFilterRules()
        .withSafeProjectionRules()
        .withSafeJoinRules()
        .withAggregateRules()
        .withSafeSortRules();
  }

  public Builder withAggressiveFilterRules() {
    // All filter rules including potentially problematic ones
    // Use only when you know your query structure
    return addRules(
        CoreRules.FILTER_INTO_JOIN,  // Can cause loops
        CoreRules.FILTER_MERGE,
        CoreRules.FILTER_AGGREGATE_TRANSPOSE,
        CoreRules.FILTER_PROJECT_TRANSPOSE);
  }

  public Builder withAggressiveJoinRules() {
    // All join rules including potentially problematic ones
    return addRules(
        CoreRules.JOIN_CONDITION_PUSH,
        CoreRules.JOIN_EXTRACT_FILTER,
        CoreRules.JOIN_PUSH_TRANSITIVE_PREDICATES,
        CoreRules.JOIN_COMMUTE);  // Can cause loops
  }
  */
}
