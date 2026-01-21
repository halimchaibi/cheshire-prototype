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

/**
 * Configuration interface for query engines.
 *
 * <p>Implementations provide type-safe access to engine configuration. Configs should be immutable
 * after construction and validate themselves.
 *
 * <h2>Example Implementation</h2>
 *
 * <pre>{@code
 * public record JdbcQueryEngineConfig(String name, List<String> sources)
 *         implements QueryEngineConfig {
 *
 *     @Override
 *     public boolean validate() throws QueryEngineException {
 *         if (name == null || name.isBlank()) {
 *             throw new QueryEngineConfigurationException("Engine name is required");
 *         }
 *         return true;
 *     }
 *
 *     @Override
 *     public Map<String, Object> asMap() {
 *         return Map.of("name", name, "sources", sources);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0
 */
public interface QueryEngineConfig {

  /**
   * Returns the unique name of the query engine.
   *
   * @return the engine name, never {@code null}
   */
  String name();

  /**
   * Returns an immutable map representation of this configuration.
   *
   * @return a map of configuration properties, never {@code null}
   */
  default Map<String, Object> asMap() {
    return Map.of();
  }

  /**
   * Validates this configuration.
   *
   * @return {@code true} if valid
   * @throws io.cheshire.spi.query.exception.QueryEngineConfigurationException if validation fails
   * @throws QueryEngineException if any other error occurs
   */
  boolean validate() throws QueryEngineException;
}
