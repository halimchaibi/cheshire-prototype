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
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.tools.FrameworkConfig;

/**
 * Query optimizer that applies optimization rules based on query characteristics and schema
 * metadata. Supports both heuristic (HepPlanner) and cost-based (VolcanoPlanner) optimization
 * strategies.
 */
public class QueryOptimizer {

  private static final int DEFAULT_MAX_ITERATIONS = 1000;
  private static final int COMPLEX_QUERY_MAX_ITERATIONS = 2000;

  private QueryOptimizer() {
    // Force users to use the builder:
    // QueryOptimizer.builder().withFrameworkConfig(...).build();
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

  public static QueryOptimizer.Builder builder() {
    return new QueryOptimizer.Builder();
  }

  public static class Builder {
    private FrameworkConfig frameworkConfig;

    public QueryOptimizer.Builder withFrameworkConfig(FrameworkConfig config) {
      this.frameworkConfig = config;
      return this;
    }

    public QueryOptimizer build() {
      return new QueryOptimizer();
    }
  }
}
