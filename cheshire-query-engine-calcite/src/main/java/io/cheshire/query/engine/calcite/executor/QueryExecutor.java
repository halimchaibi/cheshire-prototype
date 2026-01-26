/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.executor;

import io.cheshire.spi.query.exception.QueryExecutionException;
import java.sql.ResultSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.RelRunners;

/**
 * Executes optimized RelNode plans using Calcite's RelRunners.
 *
 * <p>This executor converts the optimized relational plan into an enumerable that can be iterated
 * to retrieve query results.
 */
@Slf4j
public class QueryExecutor {

  private final FrameworkConfig frameworkConfig;

  /**
   * Creates a new QueryExecutor with the given FrameworkConfig.
   *
   * @param frameworkConfig the Calcite framework configuration
   */
  public QueryExecutor(FrameworkConfig frameworkConfig) {
    this.frameworkConfig = frameworkConfig;
  }

  /**
   * Executes the optimized relational plan and returns an enumerator of result rows.
   *
   * @param optimizedPlan the optimized RelNode plan to execute
   * @return an enumerator of Object[] arrays representing result rows
   * @throws QueryExecutionException if execution fails
   */
  public ResultSet execute(RelNode optimizedPlan) throws QueryExecutionException {
    if (optimizedPlan == null) {
      throw new QueryExecutionException("Cannot execute null plan");
    }

    try {
      log.debug("Executing optimized plan: {}", optimizedPlan.getClass().getSimpleName());

      // Use Calcite's RelRunners to execute the plan
      // This converts the RelNode to an Enumerable and executes it
      return RelRunners.run(optimizedPlan).executeQuery();

    } catch (Exception e) {
      log.error("Failed to execute plan", e);
      throw new QueryExecutionException("Query execution failed: " + e.getMessage(), e);
    }
  }
}
