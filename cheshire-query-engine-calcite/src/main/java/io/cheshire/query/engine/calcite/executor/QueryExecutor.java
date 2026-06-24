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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.interpreter.Bindables;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelHomogeneousShuttle;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttle;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.logical.LogicalTableScan;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.RelRunner;

// TODO (EXECUTION): QueryExecutor currently relies on Calcite's local JVM
// enumerable execution path.
//
// Limitations:
//  - QueryRuntimeContext (timezone, locale, cancel flag) is not propagated
//  - No execution strategy selection (local vs pushdown vs federated)
//  - No timeout, cancellation, or observability hooks
//
// Target design:
//  - Make execution context-aware (DataContext binding)
//  - Allow multiple execution strategies
//  - Delegate execution to adapters when full pushdown is possible

/**
 * Executes optimized RelNode plans using Calcite's local enumerable path.
 *
 * <p>This executor prepares the optimized relational plan against a Calcite execution connection
 * with the configured source schemas registered under their source names.
 */
@Slf4j
public class QueryExecutor {

  private final Map<String, Schema> schemas;

  /**
   * Creates a new QueryExecutor with the given FrameworkConfig.
   *
   * @param frameworkConfig the Calcite framework configuration
   */
  public QueryExecutor(FrameworkConfig frameworkConfig) {
    this(frameworkConfig, Map.of());
  }

  /**
   * Creates a new QueryExecutor with schemas that must be registered on the execution connection.
   *
   * @param frameworkConfig the Calcite framework configuration
   * @param schemas schemas keyed by their Calcite registration name
   */
  public QueryExecutor(FrameworkConfig frameworkConfig, Map<String, Schema> schemas) {
    Objects.requireNonNull(frameworkConfig, "frameworkConfig");
    this.schemas = schemas == null ? Map.of() : Map.copyOf(schemas);
  }

  /**
   * Executes the optimized relational plan and returns a result set.
   *
   * @param optimizedPlan the optimized RelNode plan to execute
   * @return a result set backed by the execution statement and connection
   * @throws QueryExecutionException if execution fails
   */
  public ResultSet execute(RelNode optimizedPlan) throws QueryExecutionException {
    if (optimizedPlan == null) {
      throw new QueryExecutionException("Cannot execute null plan");
    }

    Connection connection = null;
    PreparedStatement statement = null;

    try {
      log.debug("Executing optimized plan: {}", optimizedPlan.getClass().getSimpleName());

      connection = DriverManager.getConnection("jdbc:calcite:");
      CalciteConnection calciteConnection = connection.unwrap(CalciteConnection.class);
      registerConfiguredSchemas(calciteConnection);

      RelRunner runner = connection.unwrap(RelRunner.class);
      statement = runner.prepareStatement(asRunnablePlan(optimizedPlan));
      return closeResourcesWith(statement.executeQuery(), statement, connection);

    } catch (Exception e) {
      closeQuietly(statement, connection);
      log.error("Failed to execute plan", e);
      throw new QueryExecutionException("Query execution failed: " + e.getMessage(), e);
    }
  }

  private void registerConfiguredSchemas(CalciteConnection connection) {
    if (schemas.isEmpty()) {
      return;
    }

    SchemaPlus connectionRoot = connection.getRootSchema();
    for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
      String schemaName = entry.getKey();
      if (connectionRoot.getSubSchema(schemaName) == null) {
        connectionRoot.add(schemaName, entry.getValue());
      }
    }
  }

  private RelNode asRunnablePlan(RelNode rel) {
    RelShuttle shuttle =
        new RelHomogeneousShuttle() {
          @Override
          public RelNode visit(TableScan scan) {
            RelOptTable table = scan.getTable();
            if (scan instanceof LogicalTableScan && Bindables.BindableTableScan.canHandle(table)) {
              return Bindables.BindableTableScan.create(scan.getCluster(), table);
            }
            return super.visit(scan);
          }
        };
    return rel.accept(shuttle);
  }

  private ResultSet closeResourcesWith(
      ResultSet resultSet, PreparedStatement statement, Connection connection) {
    return (ResultSet)
        Proxy.newProxyInstance(
            ResultSet.class.getClassLoader(),
            new Class<?>[] {ResultSet.class},
            (proxy, method, args) -> {
              if ("close".equals(method.getName()) && method.getParameterCount() == 0) {
                closeAll(resultSet, statement, connection);
                return null;
              }

              try {
                return method.invoke(resultSet, args);
              } catch (InvocationTargetException e) {
                throw e.getCause();
              }
            });
  }

  private void closeAll(ResultSet resultSet, PreparedStatement statement, Connection connection)
      throws SQLException {
    SQLException failure = null;

    try {
      resultSet.close();
    } catch (SQLException e) {
      failure = e;
    }

    try {
      statement.close();
    } catch (SQLException e) {
      if (failure == null) {
        failure = e;
      } else {
        failure.addSuppressed(e);
      }
    }

    try {
      connection.close();
    } catch (SQLException e) {
      if (failure == null) {
        failure = e;
      } else {
        failure.addSuppressed(e);
      }
    }

    if (failure != null) {
      throw failure;
    }
  }

  private void closeQuietly(PreparedStatement statement, Connection connection) {
    try {
      if (statement != null) {
        statement.close();
      }
    } catch (SQLException e) {
      log.debug("Failed to close prepared statement after execution failure", e);
    }

    try {
      if (connection != null) {
        connection.close();
      }
    } catch (SQLException e) {
      log.debug("Failed to close connection after execution failure", e);
    }
  }
}
