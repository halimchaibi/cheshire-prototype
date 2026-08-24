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

import java.util.Locale;
import java.util.Objects;

record TypedSourceAdapterProvider(String type, SourceAdapter adapter)
    implements SourceAdapterProvider {

  TypedSourceAdapterProvider {
    Objects.requireNonNull(type, "Source adapter type is required");
    Objects.requireNonNull(adapter, "Source adapter is required");
    type = type.toLowerCase(Locale.ROOT);
  }
}
