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

@FunctionalInterface
public interface ConfigAdapter<T> {
    QueryEngineConfig adapt(String name, T queryEngineConfig);
}
