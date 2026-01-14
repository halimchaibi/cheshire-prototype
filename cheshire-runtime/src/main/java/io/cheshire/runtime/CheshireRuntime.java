/*-
 * #%L
 * Cheshire :: Runtime
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.runtime;

import io.cheshire.core.CheshireSession;
import io.cheshire.core.capability.Capability;
import io.cheshire.core.config.CheshireConfig;
import io.cheshire.core.server.CheshireDispatcher;
import io.cheshire.core.server.CheshireServer;
import io.cheshire.core.server.CheshireServerFactory;
import io.cheshire.runtime.monitoring.RuntimeHealth;
import io.cheshire.runtime.monitoring.RuntimeMetrics;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * The operational runtime container for Cheshire framework.
 * <p>
 * CheshireRuntime bridges the logical layer ({@link CheshireSession}) with the network transport layer
 * ({@link CheshireServer}), managing the complete lifecycle of protocol exposures (REST, MCP, GraphQL, WebSockets).
 * <p>
 * <strong>Runtime Architecture:</strong>
 *
 * <pre>
 * CheshireRuntime (Operational Container)
 *   ├── CheshireSession (Business Logic)
 *   ├── CheshireServer[] (Protocol Exposures)
 *   │   ├── REST API (Jetty/HTTP)
 *   │   ├── MCP stdio (Standard I/O)
 *   │   └── MCP HTTP (WebSocket/SSE)
 *   ├── RuntimeHealth (Health monitoring)
 *   └── RuntimeMetrics (Performance metrics)
 * </pre>
 * <p>
 * <strong>State Machine:</strong>
 *
 * <pre>
 * NEW → STARTING → RUNNING → STOPPING → STOPPED
 *         ↓          ↓
 *       FAILED ←——————
 * </pre>
 * <p>
 * <strong>Startup Sequence:</strong>
 * <ol>
 * <li>State: NEW → STARTING</li>
 * <li>Load capabilities from session</li>
 * <li>Create servers via {@link CheshireServerFactory}</li>
 * <li>Start servers concurrently using {@link StructuredTaskScope}</li>
 * <li>Register shutdown hooks</li>
 * <li>State: STARTING → RUNNING</li>
 * </ol>
 * <p>
 * <strong>Shutdown Sequence:</strong>
 * <ol>
 * <li>State: RUNNING → STOPPING</li>
 * <li>Stop accepting new requests (servers)</li>
 * <li>Drain in-flight requests (graceful timeout)</li>
 * <li>Stop session (cleanup resources)</li>
 * <li>Execute shutdown hooks in reverse order</li>
 * <li>State: STOPPING → STOPPED</li>
 * </ol>
 * <p>
 * <strong>Thread Safety:</strong> All state transitions use atomic operations. The runtime uses:
 * <ul>
 * <li>{@link AtomicReference} for state management</li>
 * <li>{@link ReentrantLock} for terminal state coordination</li>
 * <li>{@link java.util.concurrent.CopyOnWriteArrayList} for concurrent server/listener management</li>
 * <li>Virtual threads for all async operations</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 *
 * <pre>{@code
 * CheshireSession session = CheshireBootstrap.fromClasspath("config").build();
 *
 * CheshireRuntime runtime = CheshireRuntime.expose(session).start();
 *
 * // Wait for termination signal
 * runtime.awaitTermination();
 * }</pre>
 *
 * @see CheshireSession
 * @see CheshireServer
 * @see RuntimeHealth
 * @see RuntimeMetrics
 * @since 1.0.0
 */
@Slf4j
public final class CheshireRuntime implements AutoCloseable {

    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ReentrantLock terminalLock = new ReentrantLock();
    private final Condition isTerminal = terminalLock.newCondition();
    private final List<Consumer<RuntimeHealth.Status>> statusListeners = new CopyOnWriteArrayList<>();

    private final List<CheshireServer> servers = new CopyOnWriteArrayList<>();

    private final RuntimeHealth health = new RuntimeHealth();
    private final RuntimeMetrics metrics = new RuntimeMetrics();

    private final CheshireSession session;

    private CheshireRuntime(CheshireSession session) {
        this.session = session;
    }

    /**
     * Creates a runtime instance from an initialized session.
     * <p>
     * This is the primary factory method for creating runtime instances. It bridges the business logic layer
     * ({@link CheshireSession}) with the network transport layer.
     * <p>
     * <strong>Design Rationale:</strong> The separation between Session and Runtime allows:
     * <ul>
     * <li>Testing business logic without network layer</li>
     * <li>Multiple runtimes from same session (different ports/protocols)</li>
     * <li>Runtime replacement without session reconstruction</li>
     * </ul>
     *
     * @param session
     *            initialized and optionally started CheshireSession
     * @return runtime instance ready to be started
     * @throws NullPointerException
     *             if session is null
     */
    public static CheshireRuntime expose(CheshireSession session) {
        return new CheshireRuntime(session);
    }

    private static void printBanner(CheshireConfig config) {
        var info = config.getInfo();
        log.info("""
                -------------------------------------------
                  {} (v{})
                  Domain: {} | Org: {}
                -------------------------------------------
                """, info.getName().toUpperCase(), info.getVersion(), info.getDomain(), info.getOrganization());
    }

    /**
     * Starts the runtime with all configured capabilities.
     * <p>
     * <strong>Startup Process:</strong>
     * <ol>
     * <li>Print application banner with version info</li>
     * <li>Record start metrics</li>
     * <li>Load all capabilities from session</li>
     * <li>Create servers for each capability concurrently</li>
     * <li>Start all servers using structured concurrency</li>
     * <li>Transition to RUNNING state</li>
     * <li>Notify status listeners</li>
     * </ol>
     * <p>
     * <strong>Concurrent Server Startup:</strong> Uses {@link StructuredTaskScope} for fail-fast parallel
     * initialization. If any server fails to start, all servers are stopped and runtime transitions to FAILED state.
     * <p>
     * <strong>Idempotency:</strong> Safe to call multiple times. Subsequent calls after successful start are no-ops
     * returning the existing instance.
     *
     * @return this runtime instance for method chaining
     * @throws CheshireRuntimeError
     *             if server initialization fails
     */
    public CheshireRuntime start() {
        if (!state.compareAndSet(State.NEW, State.STARTING))
            return this;
        printBanner(this.session.config());
        metrics.recordStart();

        try (var scope = StructuredTaskScope.open()) {
            log.info("Starting Cheshire Servers...");

            session.capabilities().all().forEach(capability -> scope.fork(() -> {
                CheshireServer server = CheshireServerFactory.load(capability.transport().getFactory()).create(
                        capability, capability.exposure().getBinding(), CheshireDispatcher.from(capability, session));

                server.start();
                servers.add(server);
                return null;
            }));

            scope.join();

            state.set(State.RUNNING);
            health.setStatus(RuntimeHealth.Status.RUNNING, "System is operational");
            notifyStatusChange(RuntimeHealth.Status.RUNNING);

            log.info("Runtime is now RUNNING");
        } catch (Throwable e) {
            state.set(State.FAILED);
            handleStartupFailure(e);
        }
        return this;
    }

    /**
     * Starts the runtime with a single specific capability.
     * <p>
     * This method provides fine-grained control for scenarios where only one capability should be exposed, such as:
     * <ul>
     * <li>Microservice deployments (one capability per service)</li>
     * <li>Testing specific capabilities in isolation</li>
     * <li>Gradual rollout of new capabilities</li>
     * </ul>
     * <p>
     * <strong>Note:</strong> Unlike {@link #start()}, this method does not use structured concurrency since only one
     * server is created.
     *
     * @param capability
     *            the specific capability to expose
     * @return this runtime instance for method chaining
     * @throws CheshireRuntimeError
     *             if server initialization fails
     */
    public CheshireRuntime start(Capability capability) {
        if (!state.compareAndSet(State.NEW, State.STARTING))
            return this;

        metrics.recordStart();
        try {
            printBanner(session.config());
            CheshireServer server = CheshireServerFactory.load(capability.transport().getFactory()).create(capability,
                    capability.exposure().getBinding(), CheshireDispatcher.from(capability, session));

            servers.add(server);

            Thread.ofVirtual().start(() -> {
                try {
                    server.start();
                } catch (Exception e) {
                    log.error("Server failed", e);
                }
            });

            health.setStatus(RuntimeHealth.Status.RUNNING, "System is operational");
            state.set(State.RUNNING);
            log.info("Runtime is now RUNNING with capability {}", capability.name());

        } catch (Throwable e) {
            state.set(State.FAILED);
            handleStartupFailure(e);
        }
        return this;
    }

    /**
     * Initiates graceful shutdown of the runtime.
     * <p>
     * Delegates to {@link #close()} for consistent shutdown behavior. This method exists for API clarity and
     * AutoCloseable compatibility.
     *
     * @see #close()
     */
    public void stop() {
        this.close();
    }

    /**
     * Performs graceful shutdown of all runtime components.
     * <p>
     * <strong>Shutdown Sequence:</strong>
     * <ol>
     * <li>Transition to STOPPING state</li>
     * <li>Stop all servers (stop accepting new requests)</li>
     * <li>Wait for in-flight requests to complete (30s timeout)</li>
     * <li>Stop the session (cleanup resources)</li>
     * <li>Shutdown executor service</li>
     * <li>Transition to STOPPED state</li>
     * <li>Signal threads waiting in {@link #awaitTermination()}</li>
     * </ol>
     * <p>
     * <strong>Graceful Degradation:</strong> If shutdown doesn't complete within 30 seconds, forces termination.
     * Shutdown hook failures are logged but don't prevent other hooks from running.
     * <p>
     * <strong>Thread Safety:</strong> Uses lock to ensure only one shutdown sequence executes even if called from
     * multiple threads.
     * <p>
     * <strong>Idempotency:</strong> Safe to call multiple times. Subsequent calls after shutdown initiated are no-ops.
     */
    @Override
    public void close() {
        if (!state.compareAndSet(State.RUNNING, State.STOPPING) && !state.compareAndSet(State.STARTING, State.STOPPING))
            return;

        //TODO: Remove the preview feature usage when stable
        log.info("Initiating graceful shutdown...");
        terminalLock.lock();
        try {
            // Use a virtual thread with timeout for shutdown to prevent hanging
            Thread shutdownThread = Thread.ofVirtual().start(() -> {
                try (var scope = StructuredTaskScope.open()) {
                    // Shut down network listeners first, then the core engine
                    servers.forEach(s -> scope.fork(() -> {
                        s.stop();
                        return null;
                    }));
                    scope.fork(() -> {
                        session.stop();
                        return null;
                    });
                    scope.join();

                } catch (InterruptedException e) {
                    log.warn("Shutdown interrupted due to timeout");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("Error during parallel shutdown", e);
                }
            });

            try {
                // Wait for shutdown to complete with 30-second timeout
                if (!shutdownThread.join(Duration.ofSeconds(30))) {
                    log.warn("Shutdown timeout exceeded (30s), forcing termination");
                    shutdownThread.interrupt();
                    // Give it a brief moment to respond to interrupt
                    shutdownThread.join(Duration.ofSeconds(2));
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for shutdown", e);
                Thread.currentThread().interrupt();
            } finally {
                finalizeShutdown();
            }
        } finally {
            terminalLock.unlock();
        }
    }

    /**
     * Blocks the calling thread until the runtime reaches a terminal state.
     * <p>
     * This method is essential for keeping the JVM alive in main() methods:
     *
     * <pre>{@code
     * public static void main(String[] args) {
     *     CheshireRuntime runtime = CheshireRuntime.expose(session).start();
     *
     *     // Block here until shutdown signal
     *     runtime.awaitTermination();
     * }
     * }</pre>
     * <p>
     * <strong>Terminal States:</strong>
     * <ul>
     * <li>{@code STOPPED}: Normal shutdown completed</li>
     * <li>{@code FAILED}: Startup or runtime failure occurred</li>
     * </ul>
     * <p>
     * <strong>Interruption Handling:</strong> If the calling thread is interrupted, the method returns immediately and
     * preserves the interrupt status.
     * <p>
     * <strong>Coordination Mechanism:</strong> Uses a {@link Condition} variable that is signaled when the runtime
     * transitions to STOPPED or FAILED state.
     */
    public void awaitTermination() {
        terminalLock.lock();
        try {
            while (state.get() != CheshireRuntime.State.STOPPED && state.get() != CheshireRuntime.State.FAILED) {
                isTerminal.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            terminalLock.unlock();
        }
    }

    /**
     * Checks if the runtime is in RUNNING state.
     * <p>
     * Returns {@code true} only when the runtime has successfully started and has not yet begun shutdown.
     *
     * @return {@code true} if runtime is operational, {@code false} otherwise
     */
    public boolean isRunning() {
        return state.get() == CheshireRuntime.State.RUNNING;
    }

    /**
     * Registers a callback to execute when the runtime becomes operational.
     * <p>
     * <strong>Execution Timing:</strong>
     * <ul>
     * <li>If already RUNNING: executes immediately</li>
     * <li>If not yet RUNNING: executes when state transitions to RUNNING</li>
     * <li>Never executes if runtime fails to start</li>
     * </ul>
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     * <li>Warmup caches after server starts</li>
     * <li>Register service with discovery system</li>
     * <li>Start health check endpoints</li>
     * <li>Send "ready" notification to orchestration platform</li>
     * </ul>
     * <p>
     * <strong>Thread Safety:</strong> Callbacks execute on the thread that transitions the runtime to RUNNING state.
     * Multiple callbacks are supported.
     * <p>
     * <strong>One-Time Execution:</strong> Each callback executes exactly once and is automatically removed after
     * execution.
     *
     * @param callback
     *            the action to execute when runtime is ready
     */
    public void onReady(Runnable callback) {
        if (state.get() == State.RUNNING) {
            callback.run();
            return;
        }

        final Consumer<RuntimeHealth.Status> listener = new Consumer<>() {
            private final AtomicBoolean executed = new AtomicBoolean(false);

            @Override
            public void accept(RuntimeHealth.Status status) {
                if (status == RuntimeHealth.Status.RUNNING && executed.compareAndSet(false, true)) {
                    callback.run();
                    statusListeners.remove(this);
                }
            }
        };

        statusListeners.add(listener);

        if (state.get() == State.RUNNING) {
            listener.accept(RuntimeHealth.Status.RUNNING);
        }
    }

    /**
     * Provides access to runtime health monitoring.
     * <p>
     * The health object tracks:
     * <ul>
     * <li>Current operational status</li>
     * <li>Startup/shutdown events</li>
     * <li>Error conditions and severity</li>
     * <li>Component health checks</li>
     * </ul>
     *
     * @return runtime health monitor instance
     * @see RuntimeHealth
     */
    public RuntimeHealth getHealth() {
        return health;
    }

    // =========================================================================
    // Inner classes for runtime events
    // =========================================================================

    /**
     * Provides access to runtime performance metrics.
     * <p>
     * The metrics object tracks:
     * <ul>
     * <li>Request counts and rates</li>
     * <li>Response times and latencies</li>
     * <li>Error rates and types</li>
     * <li>Resource utilization</li>
     * </ul>
     *
     * @return runtime metrics collector instance
     * @see RuntimeMetrics
     */
    public RuntimeMetrics getMetrics() {
        return metrics;
    }

    private void finalizeShutdown() {
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            state.set(State.STOPPED);
            terminalLock.lock();
            try {
                isTerminal.signalAll();
            } finally {
                terminalLock.unlock();
            }
            log.info("Runtime Stopped");
        }
    }

    private void notifyStatusChange(RuntimeHealth.Status newStatus) {
        statusListeners.forEach(listener -> listener.accept(newStatus));
    }

    private void handleStartupFailure(Throwable e) {
        state.set(State.FAILED);
        Throwable rootCause = e;
        while (rootCause.getCause() != null && rootCause != rootCause.getCause()) {
            rootCause = rootCause.getCause();
        }

        String detailedMessage = String.format(
                "Runtime Bootstrap Failed! %n" + "  Error Type: %s%n" + "  Message: %s%n" + "  Root Cause: %s%n"
                        + "  Root Message: %s",
                e.getClass().getName(), e.getMessage(), rootCause.getClass().getName(), rootCause.getMessage());

        log.error("CRITICAL: Cheshire Runtime failed to start. Shutdown initiated.", e);

        health.recordEvent(detailedMessage, RuntimeHealth.Severity.CRITICAL, e);

        try {
            this.stop();
        } catch (Exception stopEx) {
            log.error("Secondary failure during emergency shutdown: {}", stopEx.getMessage());
        }

        if (e instanceof CheshireRuntimeError cre) {
            throw cre;
        }
        throw new CheshireRuntimeError(detailedMessage, e);
    }

    // =========================================================================
    // Inner helpers classes and enums
    // =========================================================================

    private enum State {
        NEW, STARTING, RUNNING, STOPPING, STOPPED, FAILED
    }

    public sealed interface RuntimeEvent
            permits CheshireRuntime.RuntimeStarted, CheshireRuntime.RuntimeStopped, CheshireRuntime.RuntimeError {
        Instant timestamp();
    }

    public record RuntimeStarted(Instant timestamp, String version) implements CheshireRuntime.RuntimeEvent {
    }

    public record RuntimeStopped(Instant timestamp, Duration uptime) implements CheshireRuntime.RuntimeEvent {
    }

    public record RuntimeError(Instant timestamp, Throwable cause) implements CheshireRuntime.RuntimeEvent {
    }
}
