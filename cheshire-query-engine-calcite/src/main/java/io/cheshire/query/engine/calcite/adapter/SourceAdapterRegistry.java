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

import io.cheshire.spi.query.exception.QueryEngineInitializationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

/** Resolves config {@code type} strings to {@link SourceAdapter} instances. */
public final class SourceAdapterRegistry {

  private final Map<String, SourceAdapter> adaptersByType;

  private SourceAdapterRegistry(final Map<String, SourceAdapter> adaptersByType) {
    this.adaptersByType = adaptersByType;
  }

  public static SourceAdapterRegistry load() {
    final List<SourceAdapterProvider> providers = new ArrayList<>(BuiltInSourceAdapters.providers());
    ServiceLoader.load(SourceAdapterProvider.class).stream()
        .map(ServiceLoader.Provider::get)
        .forEach(providers::add);
    return of(providers);
  }

  public static SourceAdapterRegistry of(final Collection<SourceAdapterProvider> providers) {
    final Map<String, SourceAdapter> adapters = new LinkedHashMap<>();
    providers.forEach(
        provider -> {
          final String type = normalizeType(provider.type());
          if (adapters.containsKey(type)) {
            throw new IllegalStateException("Duplicate source adapter type: " + type);
          }
          adapters.put(type, provider.adapter());
        });
    return new SourceAdapterRegistry(Map.copyOf(adapters));
  }

  public SourceAdapter adapterFor(final String type) throws QueryEngineInitializationException {
    final SourceAdapter adapter = adaptersByType.get(normalizeType(type));
    if (adapter == null) {
      throw new QueryEngineInitializationException(
          "Unknown source type: "
              + type
              + ". Registered types: "
              + List.copyOf(adaptersByType.keySet()));
    }
    return adapter;
  }

  public List<String> registeredTypes() {
    return List.copyOf(adaptersByType.keySet());
  }

  private static String normalizeType(final String type) {
    return type.toLowerCase(Locale.ROOT);
  }
}
