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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.cheshire.core.constant.Key;
import io.cheshire.core.pipeline.MaterializedInput;
import io.cheshire.core.pipeline.MaterializedOutput;
import io.cheshire.query.engine.calcite.query.SqlQuery;
import io.cheshire.spi.pipeline.Context;
import io.cheshire.spi.pipeline.exception.PipelineException;
import io.cheshire.spi.query.engine.QueryEngine;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.request.QueryEngineContext;
import io.cheshire.spi.query.result.QueryEngineResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;

class CalciteSqlExecutorTest {

  @Test
  void executesConfiguredSqlAgainstEngineMetadata() throws Exception {
    final RecordingQueryEngine engine =
        new RecordingQueryEngine(
            new QueryEngineResult(
                List.of(new QueryEngineResult.Column("host", "VARCHAR", false)),
                List.of(Map.of("host", "workstation"))));
    final CalciteSqlExecutor executor =
        new CalciteSqlExecutor(
            Map.of("name", "listSystemInfo", "template", "SELECT * FROM os.system_info"));
    final MaterializedInput input =
        new MaterializedInput(
            Map.of(Key.PAYLOAD_DATA.key(), Map.of()), Map.of(Key.ENGINE.key(), engine));

    final MaterializedOutput output = executor.apply(input, new TestContext());

    assertThat(engine.lastQuery().query()).isEqualTo("SELECT * FROM os.system_info");
    assertThat(output.data())
        .containsEntry("rows", List.of(Map.of("host", "workstation")))
        .containsKey("columns");
    assertThat(output.metadata())
        .containsEntry("executor-name", "listSystemInfo")
        .containsEntry("row-count", 1);
  }

  @Test
  void passesPayloadParametersToSqlQuery() throws Exception {
    final RecordingQueryEngine engine =
        new RecordingQueryEngine(new QueryEngineResult(List.of(), List.of()));
    final CalciteSqlExecutor executor =
        new CalciteSqlExecutor(
            Map.of("name", "listProcesses", "template", "SELECT * FROM os.ps WHERE pid = ?"));
    final MaterializedInput input =
        new MaterializedInput(
            Map.of(Key.PAYLOAD_PARAMETERS.key(), Map.of("pid", 1001)),
            Map.of(Key.ENGINE.key(), engine));

    executor.apply(input, new TestContext());

    assertThat(engine.lastQuery().parameters()).containsEntry("pid", 1001);
  }

  @Test
  void opensClosedEngineBeforeExecuting() throws Exception {
    final RecordingQueryEngine engine =
        new RecordingQueryEngine(new QueryEngineResult(List.of(), List.of()));
    engine.close();
    final CalciteSqlExecutor executor =
        new CalciteSqlExecutor(
            Map.of("name", "listJavaInfo", "template", "SELECT * FROM os.java_info"));
    final MaterializedInput input =
        new MaterializedInput(
            Map.of(Key.PAYLOAD_DATA.key(), Map.of()), Map.of(Key.ENGINE.key(), engine));

    executor.apply(input, new TestContext());

    assertThat(engine.opened()).isTrue();
  }

  @Test
  void rejectsInputWithoutQueryEngineMetadata() {
    final CalciteSqlExecutor executor =
        new CalciteSqlExecutor(Map.of("name", "listSystemInfo", "template", "SELECT 1"));

    assertThatThrownBy(() -> executor.apply(MaterializedInput.empty(), new TestContext()))
        .isInstanceOf(PipelineException.class)
        .hasMessageContaining("Query engine metadata is required");
  }

  private record TestContext(ConcurrentMap<String, Object> attributes) implements Context {
    private TestContext() {
      this(new ConcurrentHashMap<>());
    }
  }

  private static final class RecordingQueryEngine implements QueryEngine<SqlQuery> {
    private final QueryEngineResult result;
    private SqlQuery lastQuery;
    private boolean opened = true;

    private RecordingQueryEngine(final QueryEngineResult result) {
      this.result = result;
    }

    @Override
    public String name() {
      return "calcite";
    }

    @Override
    public QueryEngineResult execute(final SqlQuery query, final QueryEngineContext ctx) {
      if (!opened) {
        throw new IllegalStateException("engine is closed");
      }
      this.lastQuery = query;
      return result;
    }

    @Override
    public String explain(final SqlQuery query) {
      return query.query();
    }

    @Override
    public boolean validate(final SqlQuery query) {
      return true;
    }

    @Override
    public boolean supportsStreaming() {
      return false;
    }

    @Override
    public boolean isOpen() {
      return opened;
    }

    @Override
    public void open() throws QueryEngineException {
      opened = true;
    }

    @Override
    public void close() {
      opened = false;
    }

    private SqlQuery lastQuery() {
      return lastQuery;
    }

    private boolean opened() {
      return opened;
    }
  }
}
