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

/** Classification of query types for optimization purposes */
public enum QueryType {
  /** Online Analytical Processing - complex aggregations, large scans */
  OLAP,

  /** Online Transaction Processing - simple queries, fast lookups */
  OLTP,

  /** Queries spanning multiple data sources */
  FEDERATED,

  /** Simple SELECT with filters and projections */
  SIMPLE_SELECT,

  /** Complex queries with multiple joins */
  COMPLEX_JOIN,

  /** Queries with window functions */
  WINDOW_QUERY,

  /** Queries with CTEs or subqueries */
  NESTED_QUERY,

  /** Query type not yet determined */
  UNKNOWN;

  public boolean isComplex() {
    return this == OLAP || this == COMPLEX_JOIN || this == WINDOW_QUERY || this == NESTED_QUERY;
  }

  public boolean requiresAggregation() {
    return this == OLAP;
  }
}
