/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.query;

/** Detailed characteristics of a query used for rule selection */
public class QueryCharacteristics {

  private final boolean hasAggregations;
  private final boolean hasJoins;
  private final boolean hasSubqueries;
  private final boolean hasWindowFunctions;
  private final boolean hasComplexFilters;
  private final int joinCount;
  private final int tableCount;
  private final int estimatedRowCount;

  public QueryCharacteristics() {
    this(false, false, false, false, false, 0, 0, -1);
  }

  private QueryCharacteristics(
      boolean hasAggregations,
      boolean hasJoins,
      boolean hasSubqueries,
      boolean hasWindowFunctions,
      boolean hasComplexFilters,
      int joinCount,
      int tableCount,
      int estimatedRowCount) {
    this.hasAggregations = hasAggregations;
    this.hasJoins = hasJoins;
    this.hasSubqueries = hasSubqueries;
    this.hasWindowFunctions = hasWindowFunctions;
    this.hasComplexFilters = hasComplexFilters;
    this.joinCount = joinCount;
    this.tableCount = tableCount;
    this.estimatedRowCount = estimatedRowCount;
  }

  public boolean hasAggregations() {
    return hasAggregations;
  }

  public boolean hasJoins() {
    return hasJoins;
  }

  public boolean hasSubqueries() {
    return hasSubqueries;
  }

  public boolean hasWindowFunctions() {
    return hasWindowFunctions;
  }

  public boolean hasComplexFilters() {
    return hasComplexFilters;
  }

  public int getJoinCount() {
    return joinCount;
  }

  public int getTableCount() {
    return tableCount;
  }

  public int getEstimatedRowCount() {
    return estimatedRowCount;
  }

  public boolean isComplex() {
    return joinCount > 3 || hasSubqueries || hasWindowFunctions;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private boolean hasAggregations = false;
    private boolean hasJoins = false;
    private boolean hasSubqueries = false;
    private boolean hasWindowFunctions = false;
    private boolean hasComplexFilters = false;
    private int joinCount = 0;
    private int tableCount = 0;
    private int estimatedRowCount = -1;

    public Builder withAggregations(boolean hasAggregations) {
      this.hasAggregations = hasAggregations;
      return this;
    }

    public Builder withJoins(boolean hasJoins) {
      this.hasJoins = hasJoins;
      return this;
    }

    public Builder withSubqueries(boolean hasSubqueries) {
      this.hasSubqueries = hasSubqueries;
      return this;
    }

    public Builder withWindowFunctions(boolean hasWindowFunctions) {
      this.hasWindowFunctions = hasWindowFunctions;
      return this;
    }

    public Builder withComplexFilters(boolean hasComplexFilters) {
      this.hasComplexFilters = hasComplexFilters;
      return this;
    }

    public Builder withJoinCount(int joinCount) {
      this.joinCount = joinCount;
      return this;
    }

    public Builder withTableCount(int tableCount) {
      this.tableCount = tableCount;
      return this;
    }

    public Builder withEstimatedRowCount(int estimatedRowCount) {
      this.estimatedRowCount = estimatedRowCount;
      return this;
    }

    public QueryCharacteristics build() {
      return new QueryCharacteristics(
          hasAggregations,
          hasJoins,
          hasSubqueries,
          hasWindowFunctions,
          hasComplexFilters,
          joinCount,
          tableCount,
          estimatedRowCount);
    }
  }

  @Override
  public String toString() {
    return "QueryCharacteristics{"
        + "hasAggregations="
        + hasAggregations
        + ", hasJoins="
        + hasJoins
        + ", hasSubqueries="
        + hasSubqueries
        + ", hasWindowFunctions="
        + hasWindowFunctions
        + ", joinCount="
        + joinCount
        + ", tableCount="
        + tableCount
        + ", estimatedRows="
        + estimatedRowCount
        + '}';
  }
}
