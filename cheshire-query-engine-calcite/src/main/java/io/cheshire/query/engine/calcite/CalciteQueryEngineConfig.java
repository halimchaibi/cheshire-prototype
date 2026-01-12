package io.cheshire.query.engine.calcite;

import io.cheshire.core.config.CheshireConfig;
import io.cheshire.spi.query.engine.QueryEngineConfig;

import java.util.List;
import java.util.Map;

/**
 * Configuration for the Calcite query engine.
 *
 * <p>This configuration specifies:
 * <ul>
 *   <li>The engine name/identifier</li>
 *   <li>List of source names to register as schemas</li>
 * </ul>
 * </p>
 *
 * <p>Sources listed here must be registered in the SourceProviderManager
 * before the engine is opened.</p>
 *
 * @param name    the unique name/identifier for this query engine instance
 * @param sources the list of source names to register as Calcite schemas
 * @author Cheshire Framework
 * @since 1.0.0
 */
public record CalciteQueryEngineConfig(String name, List<String> sources) implements QueryEngineConfig {

    /**
     * Creates a new CalciteQueryEngineConfig.
     *
     * @param name    the engine name, must not be null or blank
     * @param sources the list of source names, may be null (treated as empty list)
     * @throws IllegalArgumentException if name is null or blank
     */
    public CalciteQueryEngineConfig {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Engine name cannot be null or blank");
        }
        // Normalize sources to immutable list
        sources = sources != null ? List.copyOf(sources) : List.of();
    }

    /**
     * Creates a CalciteQueryEngineConfig from a CheshireConfig QueryDefinition.
     *
     * @param name     the engine name
     * @param queryDef the query definition from CheshireConfig
     * @return a new CalciteQueryEngineConfig instance
     * @throws IllegalArgumentException if name is null or blank
     */
    public static CalciteQueryEngineConfig from(String name, CheshireConfig.QueryEngine queryDef) {
        return new CalciteQueryEngineConfig(name, queryDef.getSources());
    }

    @Override
    public Map<String, Object> asMap() {
        return Map.of("name", name, "sources", sources);
    }

    @Override
    public boolean validate() {
        return name != null && !name.isBlank();
    }
}

