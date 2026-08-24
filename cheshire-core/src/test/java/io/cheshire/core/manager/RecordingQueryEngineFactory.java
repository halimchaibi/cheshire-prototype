/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.manager;

import io.cheshire.spi.query.engine.QueryEngine;
import io.cheshire.spi.query.engine.QueryEngineConfig;
import io.cheshire.spi.query.engine.QueryEngineConfigAdapter;
import io.cheshire.spi.query.engine.QueryEngineFactory;
import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.request.LogicalQuery;
import io.cheshire.spi.query.request.QueryEngineContext;
import io.cheshire.spi.query.result.QueryEngineResult;
import java.util.List;
import java.util.Map;

public final class RecordingQueryEngineFactory
    implements QueryEngineFactory<RecordingQueryEngineFactory.Config> {

  static Map<String, Object> lastAdaptedConfig;

  @Override
  public Class<Config> configType() {
    return Config.class;
  }

  @Override
  public QueryEngine<?> create(Config config) {
    return new RecordingQueryEngine(config.name());
  }

  @Override
  public QueryEngineConfigAdapter<Config> adapter() {
    return config -> {
      lastAdaptedConfig = Map.copyOf(config);
      return new Config((String) config.get("name"), Map.copyOf(config));
    };
  }

  @Override
  public void validate(Config config) throws QueryEngineException {
    config.validate();
  }

  public record Config(String name, Map<String, Object> raw) implements QueryEngineConfig {
    @Override
    public boolean validate() {
      return name != null && !name.isBlank();
    }

    @Override
    public Map<String, Object> asMap() {
      return raw;
    }
  }

  private record RecordingQueryEngine(String name) implements QueryEngine<LogicalQuery> {
    @Override
    public void open() {}

    @Override
    public QueryEngineResult execute(LogicalQuery query, QueryEngineContext ctx) {
      return new QueryEngineResult(List.of(), List.of());
    }

    @Override
    public String explain(LogicalQuery query) {
      return "";
    }

    @Override
    public boolean validate(LogicalQuery query) {
      return true;
    }

    @Override
    public boolean supportsStreaming() {
      return false;
    }

    @Override
    public boolean isOpen() {
      return true;
    }

    @Override
    public void close() {}
  }
}
