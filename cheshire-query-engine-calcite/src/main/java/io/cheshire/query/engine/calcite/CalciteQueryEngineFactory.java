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

import io.cheshire.query.engine.calcite.config.CalciteQueryEngineConfig;
import io.cheshire.spi.query.engine.QueryEngineFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CalciteQueryEngineFactory implements QueryEngineFactory<CalciteQueryEngineConfig> {

  @Override
  public Class<CalciteQueryEngineConfig> configType() {
    return CalciteQueryEngineConfig.class;
  }

  @Override
  public CalciteQueryEngine create(CalciteQueryEngineConfig config) {
    return new CalciteQueryEngine(config);
  }

  @Override
  public CalciteQueryEngineConfigAdapter adapter() {
    return new CalciteQueryEngineConfigAdapter();
  }

  @Override
  public void validate(CalciteQueryEngineConfig config) {
    // TODO: implement validation logic
    log.debug("Validating query engine config");
  }
}
