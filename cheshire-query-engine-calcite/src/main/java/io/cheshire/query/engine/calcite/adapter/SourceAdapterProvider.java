/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.adapter;

/**
 * SPI entry for registering a Calcite {@link SourceAdapter} by config {@code type} string.
 *
 * <p>Built-in adapters are loaded from {@link BuiltInSourceAdapters}; third-party modules add
 * {@code META-INF/services/io.cheshire.query.engine.calcite.adapter.SourceAdapterProvider}.
 */
public interface SourceAdapterProvider {

  /** Lowercase config key, e.g. {@code jdbc}, {@code arrow}. Must be unique across providers. */
  String type();

  SourceAdapter adapter();
}
