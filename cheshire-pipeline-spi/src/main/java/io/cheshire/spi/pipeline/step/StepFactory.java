/*-
 * #%L
 * Cheshire :: Pipeline :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.pipeline.step;

import io.cheshire.spi.pipeline.exception.PipelineException;
import java.util.Map;

/**
 * Base factory interface for all step providers. Implementations are discovered via ServiceLoader.
 */
public sealed interface StepFactory<S extends Step<?, ?>>
    /**
     * Loads all available PreProcessorFactory implementations using ServiceLoader
     *
     * @return List of all discovered PreProcessorFactory instances
     */
    permits PreProcessorFactory, ExecutorFactory, PostProcessorFactory {

  /**
   * Loads all available ExecutorFactory implementations using ServiceLoader
   *
   * @return List of all discovered ExecutorFactory instances
   */
  /** Human-readable name */
  String name();

  StepType type();

  /**
   * Description of what this step does /** Loads all available PostProcessorFactory implementations
   * using ServiceLoader
   *
   * @return List of all discovered PostProcessorFactory instances
   */
  String description();

  /** Create an instance of the step with given configuration */
  S create(Map<String, Object> config) throws PipelineException;

  /**
   * Finds a specific PreProcessorFactory by its ID
   *
   * @param id The unique identifier of the PreProcessorFactory to find
   * @return The matching PreProcessorFactory instance
   * @throws PipelineException if no factory with the given ID is found
   */

  /** Configuration schema or expected keys */
  default Map<String, Class<?>> config() {
    return Map.of();
  }
}
