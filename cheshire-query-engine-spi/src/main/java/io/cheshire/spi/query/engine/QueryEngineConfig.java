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

import java.util.Map;

/**
 * Configuration interface for query engines.
 *
 * <p>
 * Implementations of this interface provide configuration data for query engine instances. Configuration typically
 * includes:
 * <ul>
 * <li>Engine name/identifier</li>
 * <li>Data source references</li>
 * <li>Engine-specific settings</li>
 * <li>Performance tuning parameters</li>
 * </ul>
 * </p>
 *
 * <p>
 * Configuration objects should be immutable and validated before use.
 * </p>
 *
 * @author Cheshire Framework
 * @since 1.0.0
 */
public interface QueryEngineConfig {

    /**
     * Returns the unique name/identifier for this query engine instance.
     *
     * @return the engine name, must not be null or empty
     */
    String name();

    /**
     * Returns the configuration as a map of key-value pairs.
     *
     * <p>
     * This method provides a generic representation of the configuration, useful for serialization, logging, and
     * debugging.
     * </p>
     *
     * @return an immutable map containing all configuration properties
     */
    default Map<String, Object> asMap() {
        return Map.of();
    }

    /**
     * Validates the configuration for correctness and completeness.
     *
     * <p>
     * This method should check:
     * <ul>
     * <li>Required fields are present and non-null</li>
     * <li>Values are within acceptable ranges</li>
     * <li>References to other resources are valid</li>
     * <li>No conflicting settings</li>
     * </ul>
     * </p>
     *
     * @return true if the configuration is valid, false otherwise
     */
    boolean validate();
}
