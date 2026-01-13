/*-
 * #%L
 * Cheshire :: Source Provider :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.source;

@FunctionalInterface
public interface ConfigAdapter<T> {
    SourceConfig adapt(String name, T sourceDef);
}
