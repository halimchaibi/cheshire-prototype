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
import org.apache.calcite.DataContext;
import org.apache.calcite.config.Lex;
import org.apache.calcite.plan.*;
import org.apache.calcite.plan.ConventionTraitDef;
import org.apache.calcite.plan.RelOptCostFactory;
import org.apache.calcite.plan.RelOptCostImpl;
import org.apache.calcite.plan.RelTraitDef;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.rex.RexExecutor;
import org.apache.calcite.rex.RexExecutorImpl;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
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
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to create a {@link FrameworkConfig} and optionally initialize a {@link
   * FrameworkInitializer} (injecting the built config directly).
   */
  public static final class Builder {
    private SchemaManager schemaManager;

    private SqlOperatorTable operatorTable;
    private List<Program> programs;
    private List<RelTraitDef> traitDefs;
    private RelDataTypeSystem typeSystem;
    private RelOptCostFactory costFactory;
    private DataContext dataContext;
    private Context plannerContext;
    private RexExecutor executor;
    private SqlParser.Config parserConfig;
    private SchemaPlus defaultSchema;

    public Builder withSchemaManager(SchemaManager schemaManager) {
      this.schemaManager = java.util.Objects.requireNonNull(schemaManager, "schemaManager");
      return this;
    }

    public Builder withOperatorTable(SqlOperatorTable operatorTable) {
      this.operatorTable = operatorTable;
      return this;
    }

    public Builder withPrograms(List<Program> programs) {
      this.programs = programs;
      return this;
    }

    public Builder withTraitDefs(List<RelTraitDef> traitDefs) {
      this.traitDefs = traitDefs;
      return this;
    }

    public Builder withTypeSystem(RelDataTypeSystem typeSystem) {
      this.typeSystem = typeSystem;
      return this;
    }

    public Builder withCostFactory(RelOptCostFactory costFactory) {
      this.costFactory = costFactory;
      return this;
    }

    public Builder withDataContext(DataContext dataContext) {
      this.dataContext = dataContext;
      return this;
    }

    public Builder withPlannerContext(Context plannerContext) {
      this.plannerContext = plannerContext;
      return this;
    }

    public Builder withRexExecutor(RexExecutor executor) {
      this.executor = executor;
      return this;
    }

    public Builder withParserConfig(SqlParser.Config parserConfig) {
      this.parserConfig = parserConfig;
      return this;
    }

    public Builder withDefaultSchema(SchemaPlus defaultSchema) {
      this.defaultSchema = defaultSchema;
      return this;
    }

    public FrameworkConfig build() throws QueryEngineInitializationException {
      if (this.schemaManager == null) {
        throw new QueryEngineInitializationException("SchemaManager is required");
      }
      // defaults if not provided
      SqlParser.Config finalParserConfig =
          this.parserConfig != null ? this.parserConfig : buildParserConfig();

      SqlOperatorTable finalOperatorTable =
          this.operatorTable != null ? this.operatorTable : buildOperatorTable();

      List<Program> finalPrograms = this.programs != null ? this.programs : buildPrograms();

      List<RelTraitDef> finalTraitDefs = this.traitDefs != null ? this.traitDefs : buildTraitDefs();

      // TODO: This is supposed to be query-scoped
      DataContext finalDataContext =
          this.dataContext != null ? this.dataContext : new CalciteDataContext(schemaManager);

      Context finalPlannerContext =
          this.plannerContext != null ? this.plannerContext : new PlannerContext(finalDataContext);

      RexExecutor finalExecutor =
          this.executor != null ? this.executor : new RexExecutorImpl(finalDataContext);

      SchemaPlus finalDefaultSchema =
          this.defaultSchema != null ? this.defaultSchema : schemaManager.rootSchema();

      RelDataTypeSystem typeSystem =
          this.typeSystem != null ? this.typeSystem : RelDataTypeSystem.DEFAULT;

      RelOptCostFactory costFactory =
          this.costFactory != null ? this.costFactory : RelOptCostImpl.FACTORY;

      return Frameworks.newConfigBuilder()
          .parserConfig(finalParserConfig)
          .defaultSchema(finalDefaultSchema)
          .operatorTable(finalOperatorTable)
          .programs(finalPrograms)
          .traitDefs(finalTraitDefs)
          .typeSystem(typeSystem)
          .costFactory(costFactory)
          .context(finalPlannerContext)
          .executor(finalExecutor)
          .build();
    }
  }

  // -----------------------------
  // Helper methods (stubs)
  // -----------------------------

  private static SqlParser.Config buildParserConfig() {
    return SqlParser.config().withLex(Lex.JAVA).withConformance(SqlConformanceEnum.DEFAULT);
  }

  private static SqlOperatorTable buildOperatorTable() {
    return SqlOperatorTables.chain(
        SqlStdOperatorTable.instance()
        // TODO: Add operators here, merge ANSI + adapter functions
        );
  }

  private static List<Program> buildPrograms() {
    // TODO; Using the standard programs
    return List.of(
        Programs.standard() // VolcanoPlanner, cost-based
        );
  }

  // TODO; This had be redefined in Query Optimizer, requires to set boundaries
  private static List<RelTraitDef> buildTraitDefs() {
    return List.of(
        ConventionTraitDef.INSTANCE, RelCollationTraitDef.INSTANCE
        // Optional:
        // TODO: Kept as reference for advanced use cases(engine         // that support
        // distribution)
        // RelDistributionTraitDef.INSTANCE
        );
  }
}
