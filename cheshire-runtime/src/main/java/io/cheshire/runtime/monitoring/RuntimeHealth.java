package io.cheshire.runtime.monitoring;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Health monitoring system for Cheshire Runtime with state machine validation.
 * <p>
 * <strong>Purpose:</strong>
 * <p>
 * Tracks runtime health status, records health events, validates state transitions,
 * and provides health snapshots for monitoring systems.
 * <p>
 * <strong>State Machine:</strong>
 * <pre>
 * STOPPED → STARTING → RUNNING ⇄ DEGRADED → STOPPING → STOPPED
 *     ↓         ↓          ↓          ↓          ↓
 *     └─────────┴──────────┴──────────┴──────────→ FAILED → STOPPED
 * </pre>
 * <p>
 * <strong>Health Events:</strong>
 * <ul>
 *   <li><strong>INFO:</strong> Normal operational events</li>
 *   <li><strong>WARNING:</strong> Minor issues that don't affect functionality</li>
 *   <li><strong>ERROR:</strong> Issues that may cause DEGRADED state</li>
 *   <li><strong>CRITICAL:</strong> Fatal errors that trigger FAILED state</li>
 * </ul>
 * <p>
 * <strong>Java 21 Features:</strong>
 * <ul>
 *   <li><strong>Sequenced Collections:</strong> {@code addLast()}, {@code removeFirst()}, {@code reversed()}</li>
 *   <li><strong>Pattern Matching:</strong> Automatic severity escalation for VirtualMachineError</li>
 *   <li><strong>Records:</strong> Immutable HealthEvent, HealthSnapshot, ComponentHealth</li>
 *   <li><strong>Exhaustive Switches:</strong> State transition validation</li>
 * </ul>
 * <p>
 * <strong>Thread Safety:</strong>
 * <p>
 * Uses {@link ReentrantReadWriteLock} for state transitions and {@link AtomicReference}
 * for status reads. {@link CopyOnWriteArrayList} ensures safe concurrent event recording.
 * <p>
 * <strong>Event History:</strong>
 * <p>
 * Maintains last 1000 health events with automatic eviction of oldest events.
 * Events are stored in chronological order with efficient reversed access.
 * <p>
 * <strong>TODO:</strong> Integration testing hack for late RUNNING signals from zombie threads.
 *
 * @see RuntimeMetrics
 * @see io.cheshire.runtime.CheshireRuntime
 * @since 1.0.0
 */
public class RuntimeHealth {

    private final AtomicReference<Status> currentStatus = new AtomicReference<>(Status.STOPPED);
    private final AtomicReference<String> statusMessage = new AtomicReference<>("Runtime not started");
    private final AtomicReference<Instant> lastStatusChange = new AtomicReference<>(Instant.now());

    // Java 21: Using SequencedCollection interface for better history management
    private final List<HealthEvent> healthEvents = new CopyOnWriteArrayList<>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final List<ComponentHealth> componentHealth = new ArrayList<>();

    public RuntimeHealth() {
        recordEvent("Runtime health monitor initialized", Severity.INFO);
    }

    public void setStatus(Status status, String message) {
        lock.writeLock().lock();
        try {
            Status previous = currentStatus.get();
            //TODO: The hack is for integration testing, to be reviewed.
            // If we are already STOPPED or STOPPING, ignore late "RUNNING" signals from zombie threads
            if ((previous == Status.STOPPED || previous == Status.STOPPING) && status == Status.RUNNING) {
                return;
            }
            if (!isValidTransition(previous, status)) {
                String errorMsg = "Invalid state transition: %s -> %s".formatted(previous, status);
                recordEvent(errorMsg, Severity.ERROR);
                throw new IllegalStateException(errorMsg);
            }

            currentStatus.set(status);
            statusMessage.set(Objects.requireNonNullElse(message, getDefaultMessage(status)));
            lastStatusChange.set(Instant.now());

            recordEvent("Status changed: %s -> %s%s".formatted(
                    previous, status, message != null ? " (" + message + ")" : ""), Severity.INFO);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Overload for recording events without a specific Throwable cause.
     * Functional approach: delegates to the primary method with an empty context.
     */
    public void recordEvent(String message, Severity severity) {
        recordEvent(message, severity, null);
    }

    /**
     * Primary health recording logic.
     * Uses Java 21 Pattern Matching and Sequenced Collections.
     */
    public void recordEvent(String message, Severity severity, Throwable cause) {
        // 1. Determine severity using Pattern Matching (Scala-style guard/match)
        // This allows us to escalate "OutOfMemoryError" even if the user passed Severity.INFO
        Severity actualSeverity = switch (cause) {
            case VirtualMachineError e -> Severity.CRITICAL;
            case null -> severity;
            default -> severity;
        };

        // 2. Immutability & Sequenced Collections
        var event = new HealthEvent(Instant.now(), message, actualSeverity, cause);
        healthEvents.addLast(event); // Explicitly Sequenced

        if (healthEvents.size() > 1000) {
            healthEvents.removeFirst();
        }

        applyHealthSideEffects(actualSeverity, message);
    }

    private void applyHealthSideEffects(Severity severity, String msg) {
        lock.writeLock().lock();
        try {
            Status current = currentStatus.get();

            // Java 21 Exhaustive Switch as a Statement
            switch (severity) {
                case CRITICAL -> {
                    if (current != Status.FAILED) setStatus(Status.FAILED, "Critical: " + msg);
                }
                case ERROR -> {
                    if (current == Status.RUNNING) setStatus(Status.DEGRADED, "Error: " + msg);
                }
                default -> {
                } // No-op for INFO/WARNING
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public HealthSnapshot getHealthSnapshot() {
        lock.readLock().lock();
        try {
            return new HealthSnapshot(
                    currentStatus.get(),
                    statusMessage.get(),
                    lastStatusChange.get(),
                    Instant.now(),
                    calculateOverallHealth(),
                    getRecentEvents(10)
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the current status of the runtime.
     * Pure, thread-safe read of the atomic reference.
     */
    public Status getStatus() {
        // In Java 21, accessing an AtomicReference is a standard way
        // to ensure visibility across Virtual Threads without locking.
        return currentStatus.get();
    }

    public long getUptimeMillis() {
        if (getStatus() == Status.STOPPED) return 0;

        return healthEvents.reversed().stream() // Java 21: Efficient reversed view
                .filter(e -> e.message().contains("-> RUNNING"))
                .findFirst()
                .map(e -> java.time.Duration.between(e.timestamp(), Instant.now()).toMillis())
                .orElse(0L);
    }

    private boolean isValidTransition(Status from, Status to) {
        if (to == Status.FAILED) return true;

        // Java 21: Exhaustive switch expression
        return switch (from) {
            case STOPPED -> to == Status.STARTING;
            case STARTING -> to == Status.RUNNING || to == Status.FAILED;
            case RUNNING -> to == Status.STOPPING || to == Status.DEGRADED || to == Status.FAILED;
            case DEGRADED -> to == Status.RUNNING || to == Status.STOPPING || to == Status.FAILED;
            case STOPPING -> to == Status.STOPPED || to == Status.FAILED;
            case FAILED -> to == Status.STOPPED || to == Status.STARTING;
        };
    }

    private String getDefaultMessage(Status status) {
        return switch (status) {
            case STARTING -> "Runtime is starting up";
            case RUNNING -> "Runtime is operational";
            case STOPPING -> "Runtime is shutting down";
            case FAILED -> "Runtime encountered a fatal error";
            case STOPPED -> "Runtime is stopped";
            case DEGRADED -> "Runtime is running with degraded performance";
        };
    }

    private boolean calculateOverallHealth() {
        Status status = currentStatus.get();
        // A system is overall healthy only if it's RUNNING (or DEGRADED)
        // AND all registered components report healthy.
        if (status == Status.RUNNING || status == Status.DEGRADED) {
            return componentHealth.isEmpty() ||
                    componentHealth.stream().allMatch(ComponentHealth::healthy);
        }
        return false;
    }

    private List<HealthEvent> getRecentEvents(int count) {
        // healthEvents is a CopyOnWriteArrayList, which implements SequencedCollection
        return healthEvents.reversed() // Start from the newest
                .stream()
                .limit(count)          // Take the first 10
                .toList();             // Collect into an immutable list
    }

    public enum Status {STARTING, RUNNING, STOPPING, FAILED, STOPPED, DEGRADED}

    public enum Severity {INFO, WARNING, ERROR, CRITICAL}

    /**
     * Java 21 Record for immutable health events.
     */
    public record HealthEvent(Instant timestamp, String message, Severity severity, Throwable cause) {
        @Override
        public String toString() {
            return "[%s] %s: %s".formatted(timestamp, severity, message);
        }
    }

    public record HealthSnapshot(
            Status status,
            String message,
            Instant lastStatusChange,
            Instant timestamp,
            boolean healthy,
            List<HealthEvent> recentEvents
    ) {
        public String toJson() {
            return """
                    {
                      "status": "%s",
                      "message": "%s",
                      "healthy": %b,
                      "uptimeMs": %d,
                      "timestamp": "%s"
                    }
                    """.formatted(status, message.replace("\"", "\\\""), healthy,
                    java.time.Duration.between(lastStatusChange, timestamp).toMillis(), timestamp);
        }
    }

    public record ComponentHealth(String name, boolean healthy, String details, Instant lastUpdate) {
    }
}
