/**
 * Core framework components providing the foundation for Cheshire's capability-driven architecture.
 * <p>
 * <strong>Package Overview:</strong>
 * <p>
 * This is the heart of the Cheshire Framework, containing:
 * <ul>
 *   <li><strong>CheshireBootstrap</strong> - Framework initialization entry point</li>
 *   <li><strong>CheshireSession</strong> - Runtime orchestration hub and primary container</li>
 *   <li><strong>SessionTask</strong> - Execution context for business logic</li>
 *   <li><strong>TaskResult</strong> - Sealed outcome ADT (Success/Failure)</li>
 *   <li><strong>Key</strong> - Framework-wide metadata constants</li>
 * </ul>
 * <p>
 * <strong>Core Concepts:</strong>
 * <p>
 * The core module implements several key architectural patterns:
 * <ul>
 *   <li><strong>Capability-Driven Design:</strong> Resources exposed as self-contained capabilities</li>
 *   <li><strong>Three-Stage Pipeline:</strong> PreProcessor → Executor → PostProcessor</li>
 *   <li><strong>SPI Integration:</strong> Plugin-based extensibility via ServiceLoader</li>
 *   <li><strong>Manager Pattern:</strong> Lifecycle, configuration, and registry management</li>
 *   <li><strong>Sealed Interfaces:</strong> Type-safe result handling with exhaustive matching</li>
 * </ul>
 * <p>
 * <strong>Typical Usage:</strong>
 * <pre>{@code
 * // Initialize framework
 * CheshireSession session = CheshireBootstrap
 *     .fromClasspath("config")
 *     .build();
 *
 * // Execute task
 * SessionTask task = SessionTask.builder()
 *     .capability("my-capability")
 *     .action("my-action")
 *     .arguments(Map.of("param", "value"))
 *     .build();
 *
 * TaskResult result = session.execute(task);
 * }</pre>
 * <p>
 * <strong>Sub-packages:</strong>
 * <ul>
 *   <li>{@link io.cheshire.core.manager} - Lifecycle and registry managers</li>
 *   <li>{@link io.cheshire.core.config} - Configuration data structures</li>
 *   <li>{@link io.cheshire.core.capability} - Capability abstractions</li>
 *   <li>{@link io.cheshire.core.server} - Server infrastructure interfaces</li>
 *   <li>{@link io.cheshire.core.registry} - Generic registry implementations</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *   <li>Immutability by default (records, final fields)</li>
 *   <li>No nulls (Optional&lt;T&gt; for optional values)</li>
 *   <li>Sealed interfaces for exhaustive pattern matching</li>
 *   <li>Builder pattern for complex object construction</li>
 *   <li>Dependency injection via constructors</li>
 * </ul>
 *
 * @see io.cheshire.core.CheshireBootstrap
 * @see io.cheshire.core.CheshireSession
 * @see io.cheshire.core.manager
 * @see io.cheshire.spi
 * @since 1.0.0
 */
package io.cheshire.core;
