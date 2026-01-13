/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.server;

import java.util.function.Function;

/**
 * <h1>ConfigAdapter</h1>
 *
 * <p>
 * A <b>Schema Translator</b> that maps unstructured configuration data into strongly-typed configuration objects. This
 * interface acts as the "Decoder" in the Cheshire configuration pipeline.
 * </p>
 *
 *
 *
 * <h3>Architectural Intent</h3>
 * <p>
 * Modern configuration often arrives as a raw {@code Map<String, Object>} (from YAML, JSON-RPC, or System Properties).
 * The {@code ConfigAdapter} provides a type-safe way to extract and validate this data before it is injected into a
 * {@code CheshireServer}.
 * </p>
 *
 * <h3>Functional Composition</h3>
 * <p>
 * Because this interface extends {@link java.util.function.Function}, adapters can be easily chained or reused in
 * streams. The static {@link #of(Function)} factory allows for quick creation using <b>Lambda expressions</b> or
 * <b>Method References</b>.
 * </p>
 *
 * <pre>{@code
 * // 1. Definition via Lambda
 * ConfigAdapter<McpConfig> mcpAdapter = ConfigAdapter
 *         .of(raw -> new McpConfig((String) raw.get("server_name"), (int) raw.get("timeout")));
 *
 * // 2. Usage in the bootloader
 * Map<String, Object> rawData = configLoader.loadYaml("config.yaml");
 * McpConfig config = mcpAdapter.apply(rawData);
 * }</pre>
 *
 * @param <T>
 *            The target configuration type (e.g., {@code HttpConfig}, {@code SecurityConfig}).
 * @author Cheshire Framework
 * @since 1.0.0
 */
@FunctionalInterface
public interface ConfigAdapter<T> extends java.util.function.Function<java.util.Map<String, Object>, T> {

    /**
     * Specialized factory to wrap a standard Function as a named ConfigAdapter.
     * <p>
     * This facilitates the use of Lambda expressions while keeping the code semantically aligned with the configuration
     * domain.
     * </p>
     *
     * @param f
     *            The mapping logic (typically a constructor call or property extractor).
     * @param <T>
     *            The target configuration type.
     * @return A new ConfigAdapter instance.
     */
    static <T> ConfigAdapter<T> of(java.util.function.Function<java.util.Map<String, Object>, T> f) {
        return f::apply;
    }
}
