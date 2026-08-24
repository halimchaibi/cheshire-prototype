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

import io.cheshire.core.constant.Key;
import io.cheshire.core.pipeline.MaterializedInput;
import io.cheshire.core.pipeline.MaterializedOutput;
import io.cheshire.query.engine.calcite.query.SqlQuery;
import io.cheshire.spi.pipeline.Context;
import io.cheshire.spi.pipeline.exception.PipelineException;
import io.cheshire.spi.pipeline.step.Executor;
import io.cheshire.spi.query.engine.QueryEngine;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.request.QueryEngineContext;
import io.cheshire.spi.query.result.QueryEngineResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CalciteSqlExecutor implements Executor<MaterializedInput, MaterializedOutput> {

  private final String name;
  private final String sql;

  public CalciteSqlExecutor(final Map<String, Object> config) {
    this.name = configValue(config, "name").orElse("calciteSqlExecutor");
    this.sql =
        configValue(config, "template")
            .filter(value -> !value.isBlank())
            .orElseThrow(() -> new IllegalArgumentException("Calcite SQL template is required"));
  }

  @Override
  public MaterializedOutput apply(final MaterializedInput input, final Context ctx)
      throws PipelineException {
    final QueryEngine<SqlQuery> engine = queryEngine(input);
    final SqlQuery query = new SqlQuery(sql, parameters(input));

    try {
      ensureOpen(engine);
      final QueryEngineResult result = engine.execute(query, queryContext(ctx));
      return outputFrom(result);
    } catch (QueryEngineException e) {
      throw new PipelineException("Calcite SQL execution failed for step: " + name, e);
    }
  }

  private Optional<String> configValue(final Map<String, Object> config, final String key) {
    return Optional.ofNullable(config.get(key)).map(Object::toString);
  }

  @SuppressWarnings("unchecked")
  private QueryEngine<SqlQuery> queryEngine(final MaterializedInput input)
      throws PipelineException {
    final Object engine = input.metadata().get(Key.ENGINE.key());

    if (engine instanceof QueryEngine<?> queryEngine) {
      return (QueryEngine<SqlQuery>) queryEngine;
    }

    throw new PipelineException("Query engine metadata is required for Calcite SQL execution");
  }

  private void ensureOpen(final QueryEngine<?> engine) throws QueryEngineException {
    if (!engine.isOpen()) {
      engine.open();
    }
  }

  private Map<String, Object> parameters(final MaterializedInput input) {
    final Object payloadParameters = input.data().get(Key.PAYLOAD_PARAMETERS.key());

    if (payloadParameters instanceof Map<?, ?> parameters) {
      return parameters.entrySet().stream()
          .collect(
              LinkedHashMap::new,
              (result, entry) -> result.put(entry.getKey().toString(), entry.getValue()),
              LinkedHashMap::putAll);
    }

    return Map.of();
  }

  private QueryEngineContext queryContext(final Context ctx) {
    return new QueryEngineContext(
        null, null, null, Map.of(), List.of(), ctx.attributes(), null, null);
  }

  private MaterializedOutput outputFrom(final QueryEngineResult result) {
    final LinkedHashMap<String, Object> data = new LinkedHashMap<>();
    data.put("columns", columns(result));
    data.put("rows", result.rows());

    final LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("executor-name", name);
    metadata.put("row-count", result.rowCount());

    return MaterializedOutput.of(data, metadata);
  }

  private List<Map<String, Object>> columns(final QueryEngineResult result) {
    return result.columns().stream().map(this::column).toList();
  }

  private Map<String, Object> column(final QueryEngineResult.Column column) {
    final LinkedHashMap<String, Object> serialized = new LinkedHashMap<>();
    serialized.put("name", column.name());
    serialized.put("type", column.type());
    serialized.put("nullable", column.nullable());
    return Map.copyOf(serialized);
  }
}
