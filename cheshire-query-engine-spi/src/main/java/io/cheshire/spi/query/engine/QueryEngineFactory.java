/*-
 * #%L
 * Cheshire :: Query Engine :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.query.engine;

import io.cheshire.spi.query.exception.QueryEngineException;

public interface QueryEngineFactory<C extends QueryEngineConfig> {

  Class<C> configType();

  QueryEngine<?> create(C config) throws QueryEngineException;

  QueryEngineConfigAdapter<C> adapter();

  void validate(C config) throws QueryEngineException;
}
