/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.pipeline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.cheshire.query.engine.calcite.CalcitePlanner;
import io.cheshire.query.engine.calcite.FrameworkInitializer;
import io.cheshire.query.engine.calcite.executor.QueryExecutor;
import io.cheshire.query.engine.calcite.optimizer.QueryOptimizer;
import io.cheshire.query.engine.calcite.optimizer.QueryRuntimeContext;
import io.cheshire.query.engine.calcite.optimizer.RuleSetBuilder;
import io.cheshire.query.engine.calcite.optimizer.RuleSetManager;
import io.cheshire.query.engine.calcite.pipeline.model.LogicalPlan;
import io.cheshire.query.engine.calcite.pipeline.model.OptimizedPlan;
import io.cheshire.query.engine.calcite.pipeline.model.ParsedQuery;
import io.cheshire.query.engine.calcite.pipeline.model.PlanningContext;
import io.cheshire.query.engine.calcite.pipeline.model.RuleBoundPlan;
import io.cheshire.query.engine.calcite.pipeline.model.SqlInput;
import io.cheshire.query.engine.calcite.pipeline.model.ValidatedQuery;
import io.cheshire.query.engine.calcite.pipeline.stage.BuildContextStage;
import io.cheshire.query.engine.calcite.pipeline.stage.ConvertStage;
import io.cheshire.query.engine.calcite.pipeline.stage.OptimizeStage;
import io.cheshire.query.engine.calcite.pipeline.stage.ParseStage;
import io.cheshire.query.engine.calcite.pipeline.stage.SelectRulesStage;
import io.cheshire.query.engine.calcite.pipeline.stage.ValidateStage;
import io.cheshire.query.engine.calcite.query.SqlQuery;
import io.cheshire.query.engine.calcite.schema.SchemaManager;
import io.cheshire.query.engine.calcite.transformer.ResultTransformer;
import io.cheshire.spi.query.request.QueryEngineContext;
import io.cheshire.spi.query.result.QueryEngineResult;
import java.util.List;
import java.util.Map;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QueryPipelineStageTest {

  private SchemaManager schemaManager;
  private FrameworkConfig frameworkConfig;
  private QueryExecutor executor;
  private ResultTransformer resultTransformer;
  private QueryOptimizer optimizer;
  private StageContext stageContext;

  @BeforeEach
  void setUp() throws Exception {
    final Map<String, Object> h2ConnectionConfig =
        Map.of(
            "url",
            "jdbc:h2:mem:pipeline_stages;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "driver",
            "org.h2.Driver",
            "username",
            "sa",
            "password",
            "");
    final Map<String, Object> h2InnerConfig =
        Map.of("type", "jdbc", "schema", "PUBLIC", "connection", h2ConnectionConfig);
    final Map<String, Object> sourceEntry =
        Map.of("name", "mem", "type", "jdbc", "config", h2InnerConfig);
    final Map<String, Object> sources = Map.of("mem", sourceEntry);

    schemaManager =
        SchemaManager.builder()
            .withRootSchema(Frameworks.createRootSchema(true))
            .addSources(sources)
            .build();
    frameworkConfig =
        FrameworkInitializer.builder().withSchemaManager(schemaManager).buildBaseConfig();
    executor = new QueryExecutor(frameworkConfig, schemaManager.schemas());
    resultTransformer = new ResultTransformer();
    optimizer = QueryOptimizer.builder().withFrameworkConfig(frameworkConfig).build();
    stageContext = newStageContext("SELECT 1 AS x");
  }

  @AfterEach
  void tearDown() throws Exception {
    if (stageContext != null) {
      stageContext.close();
    }
    if (schemaManager != null) {
      schemaManager.close();
    }
  }

  @Test
  @DisplayName("ParseStage produces a SqlNode")
  void parseStageProducesSqlNode() throws Exception {
    final ParsedQuery parsed = new ParseStage().apply(new SqlInput("SELECT 1 AS x"), stageContext);
    assertNotNull(parsed.node());
  }

  @Test
  @DisplayName("ValidateStage accepts a parsed query")
  void validateStageAcceptsParsedQuery() throws Exception {
    final ParsedQuery parsed = new ParseStage().apply(new SqlInput("SELECT 1 AS x"), stageContext);
    final ValidatedQuery validated = new ValidateStage().apply(parsed, stageContext);
    assertNotNull(validated.node());
  }

  @Test
  @DisplayName("ConvertStage produces a RelNode")
  void convertStageProducesRelNode() throws Exception {
    final LogicalPlan logical = convert("SELECT 1 AS x");
    assertNotNull(logical.node());
  }

  @Test
  @DisplayName("OptimizeStage runs Hep over a non-empty rule set")
  void optimizeStageRunsHep() throws Exception {
    final LogicalPlan logical = convert("SELECT * FROM (VALUES (1)) AS t(x) WHERE x = 1 AND x = 1");
    final PlanningContext planning = new BuildContextStage().apply(logical, stageContext);
    final RuleBoundPlan bound = new SelectRulesStage().apply(planning, stageContext);
    assertFalse(bound.ruleSet().getRules().isEmpty(), "rule set should not be empty");

    final OptimizedPlan optimized = new OptimizeStage().apply(bound, stageContext);
    assertNotNull(optimized.node());
    assertNotNull(RelOptUtil.toString(optimized.node()));
  }

  @Test
  @DisplayName("QueryPipeline validate / explain / execute cutoffs work")
  void queryPipelineCutoffs() throws Exception {
    final QueryPipeline pipeline = new QueryPipeline();
    pipeline.validate("SELECT 1 AS x", stageContext);

    stageContext.close();
    stageContext = newStageContext("SELECT 1 AS x");
    final String plan = pipeline.explain("SELECT 1 AS x", stageContext);
    assertNotNull(plan);
    assertFalse(plan.isBlank());

    stageContext.close();
    stageContext = newStageContext("SELECT 1 AS x");
    final QueryEngineResult result = pipeline.execute("SELECT 1 AS x", stageContext);
    assertNotNull(result);
  }

  private LogicalPlan convert(final String sql) throws Exception {
    final ParsedQuery parsed = new ParseStage().apply(new SqlInput(sql), stageContext);
    final ValidatedQuery validated = new ValidateStage().apply(parsed, stageContext);
    return new ConvertStage().apply(validated, stageContext);
  }

  private StageContext newStageContext(final String sql) {
    final QueryEngineContext engineContext = QueryEngineContext.empty();
    final QueryRuntimeContext runtime =
        QueryRuntimeContext.fromQuery(new SqlQuery(sql, Map.of()), engineContext).build();
    final List<String> sourceNames = List.copyOf(schemaManager.getSourceNames());
    final RuleSetManager ruleSet =
        RuleSetBuilder.forSources(sourceNames).withSchemaManager(schemaManager).build();
    final FrameworkConfig queryConfig =
        FrameworkInitializer.builder()
            .withSchemaManager(schemaManager)
            .buildQueryConfig(frameworkConfig, runtime, ruleSet);
    final Planner planner = new CalcitePlanner(queryConfig);
    return new StageContext(
        planner,
        schemaManager,
        runtime,
        engineContext,
        ruleSet,
        sourceNames,
        executor,
        resultTransformer,
        optimizer);
  }
}
