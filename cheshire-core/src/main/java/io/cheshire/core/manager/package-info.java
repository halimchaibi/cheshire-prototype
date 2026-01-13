/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

/**
 * Manager classes responsible for lifecycle, configuration, and registry management.
 * <p>
 * <strong>Package Overview:</strong>
 * <p>
 * This package contains the core management infrastructure of the Cheshire Framework:
 * <ul>
 * <li><strong>ConfigurationManager</strong> - YAML loading, resolution, and validation</li>
 * <li><strong>LifecycleManager</strong> - Three-phase initialization and LIFO shutdown</li>
 * <li><strong>CapabilityManager</strong> - Capability registration and pipeline factory</li>
 * <li><strong>SourceProviderManager</strong> - Data source lifecycle and SPI loading</li>
 * <li><strong>QueryEngineManager</strong> - Query engine lifecycle and SPI loading</li>
 * </ul>
 * <p>
 * <strong>Lifecycle Management:</strong>
 * <p>
 * The {@link io.cheshire.core.manager.LifecycleManager} orchestrates a three-phase initialization:
 *
 * <pre>
 * Phase 1: Initialize Source Providers (SPI discovery via ServiceLoader)
 *    ↓
 * Phase 2: Initialize Query Engines (SPI discovery via ServiceLoader)
 *    ↓
 * Phase 3: Initialize Capabilities (Pipeline factory creation)
 * </pre>
 * <p>
 * Shutdown follows reverse order (LIFO: Capabilities → Engines → Sources).
 * <p>
 * <strong>Configuration Management:</strong>
 * <p>
 * The {@link io.cheshire.core.manager.ConfigurationManager} handles:
 * <ul>
 * <li>Loading main configuration (cheshire.yaml or custom)</li>
 * <li>Recursive loading of actions and pipelines files</li>
 * <li>Configuration validation and consistency checks</li>
 * <li>Support for both classpath and filesystem sources</li>
 * </ul>
 * <p>
 * <strong>SPI Integration:</strong>
 * <p>
 * Manager classes integrate with Service Provider Interface (SPI) for extensibility:
 *
 * <pre>{@code
 * // Source providers discovered via SPI
 * SourceProviderManager sourceManager = new SourceProviderManager();
 * sourceManager.initialize(config.getSources());
 * // Automatically loads JdbcDataSourceProviderFactory, etc.
 *
 * // Query engines discovered via SPI
 * QueryEngineManager engineManager = new QueryEngineManager();
 * engineManager.initialize(config.getQueryEngines());
 * // Automatically loads JdbcQueryEngineFactory, etc.
 * }</pre>
 * <p>
 * <strong>Registry Pattern:</strong>
 * <p>
 * All managers maintain thread-safe registries for their respective components, providing consistent lookup and
 * lifecycle management interfaces.
 * <p>
 * <strong>Design Patterns:</strong>
 * <ul>
 * <li><strong>Manager Pattern:</strong> Centralized lifecycle and registry management</li>
 * <li><strong>Service Locator:</strong> Registry-based component lookup</li>
 * <li><strong>Factory Method:</strong> SPI-based component instantiation</li>
 * <li><strong>Template Method:</strong> Consistent initialization/shutdown flow</li>
 * </ul>
 *
 * @see io.cheshire.core.manager.ConfigurationManager
 * @see io.cheshire.core.manager.LifecycleManager
 * @see io.cheshire.core.manager.CapabilityManager
 * @see io.cheshire.spi
 * @since 1.0.0
 */
package io.cheshire.core.manager;
