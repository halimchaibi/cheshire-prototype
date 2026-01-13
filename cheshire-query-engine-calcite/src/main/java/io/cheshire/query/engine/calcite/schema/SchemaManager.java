/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.schema;

import io.cheshire.core.manager.SourceProviderManager;
import io.cheshire.spi.source.SourceProvider;
import io.cheshire.spi.source.SourceProviderException;

import java.util.Map;
import java.util.Set;

/**
 * Manages multiple schemas and source providers for the Calcite query engine.
 * <p>
 * This class serves as the context type for CalciteQueryEngine, providing access to source providers by name and
 * managing schema registration.
 */
public class SchemaManager {

    private final Map<String, SourceProvider<?, ?>> sourceProviders;

    public SchemaManager(SourceProviderManager registrar) throws SourceProviderException {
        this.sourceProviders = registrar.all();
    }

    public SchemaManager(SourceProvider<?, ?> source) throws SourceProviderException {
        this.sourceProviders = Map.of(source.config().name(), source);
    }

    /**
     * Gets a source provider by name.
     *
     * @param name
     *            the source name
     * @return the source provider
     * @throws SourceProviderException
     *             if not found
     */
    public SourceProvider<?, ?> getSourceProvider(String name) throws SourceProviderException {
        SourceProvider<?, ?> provider = sourceProviders.get(name);
        if (provider == null) {
            throw new SourceProviderException("Source provider not found: " + name);
        }
        return provider;
    }

    /**
     * Returns all registered source provider names.
     *
     * @return list of source names
     */
    public Set<String> getSourceNames() {
        return sourceProviders.keySet();
    }

    /**
     * Returns all source providers.
     *
     * @return map of source name to provider
     */
    public Map<String, SourceProvider<?, ?>> getAllSourceProviders() {
        return Map.copyOf(sourceProviders);
    }

    /**
     * Checks if a source provider is registered.
     *
     * @param name
     *            the source name
     * @return true if registered, false otherwise
     */
    public boolean hasSourceProvider(String name) {
        return sourceProviders.containsKey(name);
    }
}
