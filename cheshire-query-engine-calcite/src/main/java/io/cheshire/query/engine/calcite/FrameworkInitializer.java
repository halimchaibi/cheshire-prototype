/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite;

import io.cheshire.query.engine.calcite.optimizer.CalciteDataContext;
import io.cheshire.query.engine.calcite.optimizer.PlannerContext;
import io.cheshire.query.engine.calcite.schema.SchemaManager;
import io.cheshire.spi.query.exception.QueryEngineInitializationException;
import java.util.List;
import java.util.Map;
import org.apache.calcite.DataContext;
import org.apache.calcite.config.Lex;
import org.apache.calcite.plan.*;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.rex.RexExecutor;
import org.apache.calcite.rex.RexExecutorImpl;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.util.SqlOperatorTables;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Program;
import org.apache.calcite.tools.Programs;

public class FrameworkInitializer {
  private final SchemaManager schemaManager;
  private FrameworkConfig config;

  public FrameworkInitializer(SchemaManager schemaManager) {
    this.schemaManager = schemaManager;
    ;
  }

  public void initialize(Map<String, Object> sources) throws QueryEngineInitializationException {
    try {
      this.config = createFrameworkConfig();
    } catch (Exception e) {
      throw new QueryEngineInitializationException("Failed to initialize framework", e);
    }
  }

  public FrameworkConfig config() {
    return this.config;
  }

  // -----------------------------
  // Helper methods (stubs)
  // -----------------------------

  private FrameworkConfig createFrameworkConfig() {

    SqlOperatorTable operatorTable = buildOperatorTable();

    List<Program> programs = buildPrograms();
    List<RelTraitDef> traitDefs = buildTraitDefs();
    RelDataTypeSystem typeSystem = RelDataTypeSystem.DEFAULT;

    // TODO: Use default cost factory, needed if special cost model (e.g., pushdown-aware,
    // adapter-aware).
    RelOptCostFactory costFactory = RelOptCostImpl.FACTORY;

    DataContext dataContext = new CalciteDataContext(schemaManager);
    Context plannerContext = new PlannerContext(dataContext);
    RexExecutor executor = new RexExecutorImpl(dataContext);

    return Frameworks.newConfigBuilder()
        .parserConfig(
            SqlParser.config().withLex(Lex.JAVA).withConformance(SqlConformanceEnum.DEFAULT))
        .defaultSchema(schemaManager.rootSchema())
        .operatorTable(operatorTable)
        .programs(programs)
        .traitDefs(traitDefs)
        .typeSystem(typeSystem)
        .costFactory(costFactory)
        .context(plannerContext)
        .executor(executor)
        .build();
  }

  private SqlOperatorTable buildOperatorTable() {
    // merge ANSI + adapter functions
    return SqlOperatorTables.chain(/* ANSI + adapter */ );
  }

  private List<Program> buildPrograms() {
    // TODO; Using the standard programs
    return List.of(
        Programs.standard() // VolcanoPlanner, cost-based
        );
  }

  // TODO; This had be redefined in Query Optimizer, requires to set booudaries
  private List<RelTraitDef> buildTraitDefs() {
    return List.of(
        ConventionTraitDef.INSTANCE, RelCollationTraitDef.INSTANCE
        // Optional:
        // RelDistributionTraitDef.INSTANCE //TODO: Kept as reference for advanced use cases(engine
        // that support distribution)
        );
  }
}
