/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core;

import io.cheshire.common.exception.CheshireException;
import io.cheshire.core.capability.Capability;
import io.cheshire.core.config.CheshireConfig;
import io.cheshire.core.constant.Key;
import io.cheshire.core.manager.CapabilityManager;
import io.cheshire.core.manager.QueryEngineManager;
import io.cheshire.core.manager.SourceProviderManager;
import io.cheshire.core.server.ResponseEntity;
import io.cheshire.core.server.TaskContext;
import io.cheshire.spi.pipeline.CanonicalInput;
import io.cheshire.spi.pipeline.CanonicalOutput;
import io.cheshire.spi.pipeline.PipelineProcessor;
import io.cheshire.spi.pipeline.exception.PipelineException;
import io.cheshire.spi.query.engine.QueryEngine;
import io.cheshire.spi.source.SourceProvider;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 *
 *
 * <h1>CheshireSession</h1>
 *
 * <p>The <b>Orchestration Hub</b> and primary runtime container for the Cheshire framework. This
 * class manages the initialization and shutdown lifecycles, maintains access to system-wide
 * registries, and coordinates the execution of tasks through registered capabilities.
 *
 * <h3>Lifecycle Management</h3>
 *
 * <p>The session uses an {@link java.util.concurrent.atomic.AtomicBoolean} to ensure thread-safe
 * start/stop transitions. It supports <b>Lifecycle Hooks</b>:
 *
 * <ul>
 *   <li><b>Init Hooks:</b> Executed in order during {@link #start()}. Useful for resource warming
 *       or registry discovery.
 *   <li><b>Shutdown Hooks:</b> Executed in <b>reverse order</b> during {@link #stop()} to ensure
 *       that dependencies are torn down in a safe, stack-like manner.
 * </ul>
 *
 * <h3>Task Execution Flow</h3>
 *
 * <p>The session acts as a high-level router. When {@link #execute(SessionTask, SessionContext)} is
 * called, it:
 *
 * <ol>
 *   <li>Validates mandatory routing fields ({@code capability} and {@code action}).
 *   <li>Resolves the target {@code Capability} from the internal registry.
 *   <li>Constructs an {@code Context} with cancellation and timeout support.
 *   <li>Delegates logic to the associated {@code PipelineProcessor}.
 *   <li>Wraps the result in a {@code TaskResult} (Success or Failure) for consistent downstream
 *       handling.
 * </ol>
 *
 * <h3>Design Invariants</h3>
 *
 * <ul>
 *   <li><b>Fail-Fast:</b> Attempting to execute tasks before calling {@code start()} results in an
 *       {@link IllegalStateException}.
 *   <li><b>Resilience:</b> The {@code execute} method captures {@code Throwable} types,
 *       transforming internal pipeline crashes into stable {@link
 *       io.cheshire.core.server.ResponseEntity.Status} categories.
 * </ul>
 *
 * @author Cheshire Framework
 * @since 1.0.0
 */
@Builder(builderClassName = "Builder")
@Slf4j
public class CheshireSession {

  @NonNull private final CheshireConfig config;

  @NonNull private final QueryEngineManager queryEngines;
  @NonNull private final CapabilityManager capabilities;
  @NonNull private final SourceProviderManager sourceProviders;

  @NonNull @Default private final List<Runnable> initHooks = new ArrayList<>();

  @NonNull @Default private final List<Runnable> shutdownHooks = new ArrayList<>();

  private final AtomicBoolean started = new AtomicBoolean(false);

  /**
   * Activates the session and triggers all registered initialization hooks.
   *
   * <p>Safe to call multiple times; subsequent calls will log a warning and return true.
   *
   * @return {@code true} if the session is active.
   */
  public boolean start() {
    if (!started.compareAndSet(false, true)) {
      log.warn("Session already started.");
      return true;
    }

    log.info("Starting Cheshire Session...");
    initHooks.forEach(
        hook -> {
          try {
            hook.run();
          } catch (Exception e) {
            log.error("Session Init hook failed", e);
            // Optional: set started back to false if a hook is critical
          }
        });
    return started.get();
  }

  /** Gracefully terminates the session and triggers all shutdown hooks in reverse order. */
  public void stop() {
    if (!started.compareAndSet(true, false)) {
      log.warn("Session not running.");
      return;
    }

    log.info("Stopping Cheshire Session...");
    // Run shutdown hooks in reverse order for safety
    for (int i = shutdownHooks.size() - 1; i >= 0; i--) {
      try {
        shutdownHooks.get(i).run();
      } catch (Exception e) {
        log.error("Shutdown hook failed", e);
      }
    }
  }

  public boolean isStarted() {
    return started.get();
  }

  /**
   * Internal routing logic that resolves capabilities and executes the pipeline.
   *
   * @throws PipelineException if the pipeline execution fails.
   * @throws IllegalStateException if called before the session is started.
   */
  public TaskResult execute(SessionTask task, SessionContext ctx) throws PipelineException {

    try {
      log.debug("Executing session {}", task);
      if (!isStarted()) {
        throw new IllegalStateException("Session is not started");
      }

      Capability capability =
          capabilities.get(task.requireMetaAs(Key.CAPABILITY.key(), String.class));

      log.debug("Resolved capability {}", capability.name());

      PipelineProcessor<CanonicalInput<?>, CanonicalOutput<?>> pipeline =
          capability.pipelines().get(task.requireMetaAs(Key.ACTION.key(), String.class));

      CanonicalInput<?> input = toCanonicalInput(capability, pipeline, task);

      log.debug("Resolved pipeline {} for capability {} ", pipeline.name(), capability.name());

      CanonicalOutput<?> result = pipeline.execute(input, toTaskContext(ctx));

      return new TaskResult.Success(result.data(), result.metadata());

    } catch (IllegalArgumentException e) {
      log.info("Client request rejected: {}", e.getMessage());
      return TaskResult.Failure.of(ResponseEntity.Status.BAD_REQUEST, e);

    } catch (SecurityException e) {
      log.warn("Security violation blocked: {}", e.getMessage());
      return TaskResult.Failure.of(ResponseEntity.Status.UNAUTHORIZED, e);

    } catch (Exception t) {
      log.error("Pipeline execution failed unexpectedly: ", t);
      return TaskResult.Failure.of(ResponseEntity.Status.EXECUTION_FAILED, t);
    }
  }

  public CheshireConfig config() {
    return config;
  }

  public CapabilityManager capabilities() {
    return capabilities;
  }

  public SourceProviderManager sources() {
    return sourceProviders;
  }

  public QueryEngineManager engines() {
    return queryEngines;
  }

  /** Appends a task to be run during the session startup phase. */
  public void addInitHook(Runnable hook) {
    initHooks.add(hook);
  }

  /** Appends a task to be run during the session shutdown phase. */
  public void addShutdownHook(Runnable hook) {
    shutdownHooks.add(hook);
  }

  // TODO: Place holder for very basic future admission control logic
  public AdmissionDecision admissionDecision(Object requestMeta) {
    return AdmissionDecision.ACCEPT;
  }

  private TaskContext toTaskContext(SessionContext ctx) {
    TaskContext tCtx =
        new TaskContext(
            ctx.sessionId(),
            ctx.userId(),
            ctx.traceId(),
            ctx.securityContext(),
            ctx.attributes(),
            Instant.now(),
            ctx.deadline());
    tCtx.putIfAbsent("session-task-arrived-at", Instant.now().toString());
    return tCtx;
  }

  // =======================================================================================
  // Private Helpers
  // =======================================================================================

  private CanonicalInput<?> toCanonicalInput(
      Capability capability, PipelineProcessor<?, ?> pipeline, SessionTask task)
      throws CheshireException {

    QueryEngine<?> engine = queryEngines.get(capability.engine());

    Map<String, SourceProvider<?>> sources =
        capability.sources().stream().collect(Collectors.toMap(sp -> sp, sourceProviders::get));

    Class<?> inputClass = pipeline.inputClass();

    Map<String, Object> metadata =
        new LinkedHashMap<>(
            Map.of(
                "session-task-at",
                Instant.now().toString(),
                Key.ENGINE.key(),
                engine,
                Key.SOURCES.key(),
                sources,
                Key.CAPABILITY.key(),
                capability.name()));

    try {
      CanonicalInput<?> prototype = (CanonicalInput<?>) inputClass.getConstructor().newInstance();
      return prototype.copy(task.data(), metadata);

    } catch (Exception e) {
      throw new CheshireException("Failed to initialize input type: " + inputClass.getName(), e);
    }
  }

  public enum AdmissionDecision {
    ACCEPT,
    DEFER,
    REJECT
  }
}
