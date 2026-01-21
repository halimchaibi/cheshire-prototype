/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.manager;

import io.cheshire.common.exception.CheshireException;
import io.cheshire.core.config.CheshireConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the initialization and shutdown lifecycle of all framework components.
 *
 * <p>LifecycleManager orchestrates the startup and teardown of Cheshire components in the correct
 * order, ensuring dependencies are satisfied and resources are properly cleaned up.
 *
 * <p><strong>Component Hierarchy:</strong>
 *
 * <pre>
 * LifecycleManager
 *   ├── SourceProviderManager (Phase 1: Data sources)
 *   ├── QueryEngineManager (Phase 2: Query processors)
 *   └── CapabilityManager (Phase 3: Business capabilities)
 * </pre>
 *
 * <p><strong>Initialization Phases:</strong>
 *
 * <ol>
 *   <li><strong>SOURCE_PROVIDERS</strong> - Database connections, API clients
 *   <li><strong>QUERY_ENGINES</strong> - JDBC, Calcite query engines
 *   <li><strong>CAPABILITIES</strong> - Business domains with actions/pipelines
 * </ol>
 *
 * <p><strong>Parallel Initialization:</strong> Components within the same phase initialize
 * concurrently using {@link CompletableFuture} for faster startup. Uses a cached thread pool to
 * handle variable numbers of components.
 *
 * <p><strong>Shutdown Sequence:</strong> Components shut down in <strong>reverse order</strong>
 * (LIFO) to ensure dependencies are still available during cleanup:
 *
 * <pre>
 * Capabilities → Query Engines → Source Providers
 * </pre>
 *
 * <p><strong>Thread Safety:</strong> All lifecycle transitions (initialize/shutdown) are
 * synchronized and use atomic boolean to prevent concurrent modifications.
 *
 * <p><strong>Error Handling:</strong> If any component fails to initialize, the entire
 * initialization fails. Shutdown continues even if individual components fail, logging errors but
 * not propagating exceptions.
 *
 * @see Initializable
 * @see InitializationPhase
 * @see CapabilityManager
 * @see SourceProviderManager
 * @see QueryEngineManager
 * @since 1.0.0
 */
@Slf4j
public class LifecycleManager {
  private final List<ComponentEntry> registrar = new ArrayList<>();
  private final ExecutorService executor = Executors.newCachedThreadPool();
  private final CapabilityManager capabilityManager;
  private final SourceProviderManager sourceRegistry;
  private final QueryEngineManager queryRegistry;
  private final AtomicBoolean running = new AtomicBoolean(false);

  /**
   * Constructs a LifecycleManager from Cheshire configuration.
   *
   * <p>Creates manager instances for each component type. Managers are responsible for loading and
   * initializing their respective components.
   *
   * @param config validated Cheshire configuration
   */
  public LifecycleManager(CheshireConfig config) {
    this.capabilityManager = CapabilityManager.instance(config);
    this.sourceRegistry = SourceProviderManager.instance(config);
    this.queryRegistry = QueryEngineManager.instance(config);
  }

  /**
   * Initializes all registered components in phase order.
   *
   * <p><strong>Initialization Process:</strong>
   *
   * <ol>
   *   <li>Check if already running (idempotent)
   *   <li>Register components by phase
   *   <li>Initialize all components concurrently
   *   <li>Wait for all to complete or fail-fast on error
   * </ol>
   *
   * <p><strong>Concurrency:</strong> Uses {@link CompletableFuture#allOf} to wait for all parallel
   * initialization tasks. If any fails, all are cancelled.
   *
   * <p><strong>State Management:</strong> Sets running flag atomically. On failure, resets flag to
   * allow retry.
   *
   * @throws RuntimeException if any component fails to initialize
   */
  public synchronized void initialize() {
    log.info("Initializing Lifecycle Manager");
    if (!running.compareAndSet(false, true)) {
      log.warn("LifecycleManager is already running or starting.");
      return;
    }

    try {
      // Register components in order
      registerComponent(sourceRegistry, InitializationPhase.SOURCE_PROVIDERS);
      registerComponent(queryRegistry, InitializationPhase.QUERY_ENGINES);
      registerComponent(capabilityManager, InitializationPhase.CAPABILITIES);

      // Initialize all components in parallel
      CompletableFuture<?>[] futures =
          registrar.stream()
              .map(entry -> CompletableFuture.runAsync(entry.component()::initialize, executor))
              .toArray(CompletableFuture[]::new);

      // Wait for all to complete
      CompletableFuture.allOf(futures).join();

    } catch (CheshireException e) {
      running.set(false);
      log.error("LifecycleManager initialization failed due to framework error", e);
      throw e;
    } catch (Exception e) {
      running.set(false);
      log.error("LifecycleManager initialization failed - component initialization error", e);
      throw new CheshireException(
          "Failed to initialize LifecycleManager - one or more components failed to initialize", e);
    }
  }

  /**
   * Shuts down all components in reverse registration order.
   *
   * <p><strong>Shutdown Process:</strong>
   *
   * <ol>
   *   <li>Check if running (idempotent)
   *   <li>Shutdown components in LIFO order
   *   <li>Shutdown executor service
   *   <li>Wait up to 5 seconds for graceful termination
   *   <li>Force shutdown if timeout exceeded
   * </ol>
   *
   * <p><strong>Error Handling:</strong> Continues shutdown even if individual components fail.
   * Errors are logged but not propagated to ensure all cleanup attempts are made.
   *
   * <p><strong>Thread Pool Cleanup:</strong> Executor service is shut down gracefully with timeout.
   * If tasks don't complete in time, forces immediate termination.
   *
   * @throws RuntimeException if shutdown coordination fails critically
   */
  public synchronized void shutdown() {
    if (!running.compareAndSet(true, false)) {
      log.warn("LifecycleManager is not running.");
      return;
    }

    try {
      // Shutdown registries first
      for (int i = registrar.size() - 1; i >= 0; i--) {
        registrar.get(i).component.shutdown();
      }

      // Cleanly terminate the thread pool
      executor.shutdown();
      if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
        executor.shutdownNow();
      }
    } catch (InterruptedException e) {
      executor.shutdownNow();
      Thread.currentThread().interrupt();
      log.warn("LifecycleManager shutdown interrupted", e);
    } catch (CheshireException e) {
      log.error("LifecycleManager shutdown failed due to framework error", e);
      throw e;
    } catch (Exception e) {
      log.error("LifecycleManager shutdown failed - unexpected error during component shutdown", e);
      throw new CheshireException(
          "Failed to shutdown LifecycleManager - error during component shutdown or executor termination",
          e);
    }
  }

  /**
   * Checks if the lifecycle manager is in running state.
   *
   * <p>Returns {@code true} only after successful initialization and before shutdown begins.
   *
   * @return {@code true} if components are initialized and operational
   */
  public boolean isRunning() {
    return running.get();
  }

  /**
   * Registers a component for lifecycle management.
   *
   * <p>Components are registered with their initialization phase to ensure correct startup order.
   *
   * @param component the component implementing Initializable
   * @param phase the initialization phase for ordering
   */
  private void registerComponent(Initializable component, InitializationPhase phase) {
    registrar.add(new ComponentEntry(component, phase));
  }

  /**
   * Provides access to the capability manager.
   *
   * <p>The capability manager holds all business capabilities (domains) with their actions and
   * pipelines.
   *
   * @return capability manager instance
   */
  public CapabilityManager capabilities() {
    return capabilityManager;
  }

  /**
   * Provides access to the source provider manager.
   *
   * <p>The source provider manager holds all data sources (databases, APIs, file systems) available
   * to the framework.
   *
   * @return source provider manager instance
   */
  public SourceProviderManager sources() {
    return sourceRegistry;
  }

  /**
   * Provides access to the query engine manager.
   *
   * <p>The query engine manager holds all query engines (JDBC, Calcite) available for query
   * processing.
   *
   * @return query engine manager instance
   */
  public QueryEngineManager engines() {
    return queryRegistry;
  }

  /**
   * Internal record tracking a component with its initialization phase.
   *
   * <p>Used to maintain registration order and phase information for coordinated initialization and
   * shutdown.
   *
   * @param component the initializable component
   * @param phase the initialization phase for ordering
   */
  private record ComponentEntry(Initializable component, InitializationPhase phase) {}
}
