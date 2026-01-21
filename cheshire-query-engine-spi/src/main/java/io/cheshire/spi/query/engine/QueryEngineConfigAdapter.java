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
import java.util.Map;

@FunctionalInterface
public interface QueryEngineConfigAdapter<C extends QueryEngineConfig> {
  C adapt(Map<String, Object> config) throws QueryEngineException;
}
