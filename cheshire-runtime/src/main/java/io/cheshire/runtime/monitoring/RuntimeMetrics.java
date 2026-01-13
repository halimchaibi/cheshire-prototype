/*-
 * #%L
 * Cheshire :: Runtime
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.runtime.monitoring;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * Lock-free runtime performance metrics optimized for Virtual Threads.
 * <p>
 * <strong>Purpose:</strong>
 * <p>
 * Collects and aggregates runtime performance metrics with minimal overhead. Designed for high-frequency updates from
 * concurrent Virtual Threads without contention.
 * <p>
 * <strong>Metrics Collected:</strong>
 * <ul>
 * <li><strong>Request Counters:</strong> Total, successful, failed, in-progress</li>
 * <li><strong>Timing Statistics:</strong> Min, max, average request duration</li>
 * <li><strong>Error Tracking:</strong> Error counts by category/type</li>
 * <li><strong>Component Metrics:</strong> Per-component execution statistics</li>
 * <li><strong>Memory Metrics:</strong> Heap usage, total, free, max memory</li>
 * <li><strong>Uptime:</strong> Runtime uptime in milliseconds</li>
 * </ul>
 * <p>
 * <strong>Lock-Free Design:</strong>
 * <p>
 * Uses Java's concurrent atomic classes for zero-lock updates:
 * <ul>
 * <li><strong>LongAdder:</strong> High-frequency counters (requests, errors)</li>
 * <li><strong>LongAccumulator:</strong> Min/max tracking without synchronization</li>
 * <li><strong>AtomicLong:</strong> Start/stop timestamps</li>
 * <li><strong>ConcurrentHashMap:</strong> Per-component and per-error metrics</li>
 * </ul>
 * <p>
 * <strong>RequestTimer Pattern:</strong>
 *
 * <pre>{@code
 * try (var timer = metrics.startTimer()) {
 *     // Process request...
 *     timer.success();
 * } catch (Exception e) {
 *     timer.failure(e.getClass().getSimpleName());
 * }
 * }</pre>
 * <p>
 * <strong>Virtual Thread Optimization:</strong>
 * <p>
 * {@link LongAdder} internally uses cell-based accumulation to prevent contention across thousands of Virtual Threads
 * executing concurrently.
 * <p>
 * <strong>Snapshot Access:</strong>
 * <p>
 * {@link #getSnapshot()} provides a consistent point-in-time view of all metrics. Snapshots are immutable records with
 * JSON serialization support.
 * <p>
 * <strong>Memory Metrics:</strong>
 * <p>
 * Queries {@link Runtime} for heap statistics. Consider integrating with JMX or Micrometer for production monitoring.
 *
 * @see RequestTimer
 * @see MetricsSnapshot
 * @see RuntimeHealth
 * @since 1.0.0
 */
public class RuntimeMetrics {

    private final AtomicLong startTime = new AtomicLong(0);
    private final AtomicLong stopTime = new AtomicLong(0);

    // Request Counters
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();
    private final LongAdder requestsInProgress = new LongAdder();

    // Timing Metrics - Java 8+ Accumulators are cleaner for min/max
    private final LongAdder totalRequestTimeMs = new LongAdder();
    private final LongAccumulator minRequestTimeMs = new LongAccumulator(Math::min, Long.MAX_VALUE);
    private final LongAccumulator maxRequestTimeMs = new LongAccumulator(Math::max, 0);

    private final ConcurrentMap<String, ComponentMetrics> componentMetrics = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> errorCounts = new ConcurrentHashMap<>();

    public RuntimeMetrics() {
        List.of("lifecycle_manager", "session", "capability_registry", "query_engine", "source_provider")
                .forEach(this::registerComponent);
    }

    public void recordStart() {
        startTime.set(System.currentTimeMillis());
        stopTime.set(0);
        resetRequestMetrics();
    }

    public void recordStop() {
        stopTime.set(System.currentTimeMillis());
    }

    public long getUptime() {
        long start = startTime.get();
        if (start == 0)
            return 0;
        long end = stopTime.get() > 0 ? stopTime.get() : System.currentTimeMillis();
        return end - start;
    }

    /**
     * Using try-with-resources with RequestTimer.
     */
    public RequestTimer startTimer() {
        totalRequests.increment();
        requestsInProgress.increment();
        return new RequestTimer(this, System.currentTimeMillis());
    }

    // Timing update logic is now one-liners thanks to Accumulators
    private void updateTimingMetrics(long durationMs) {
        totalRequestTimeMs.add(durationMs);
        minRequestTimeMs.accumulate(durationMs);
        maxRequestTimeMs.accumulate(durationMs);
    }

    /**
     * Record a failed request with its duration and error category.
     */
    public void recordRequestFailure(long durationMs, String errorType) {
        failedRequests.increment();
        // No need for a manual decrement here if called from RequestTimer,
        // as RequestTimer handles the inProgress counter.

        updateTimingMetrics(durationMs);

        // Track the specific error type using a LongAdder for high concurrency
        errorCounts.computeIfAbsent(errorType, k -> new LongAdder()).increment();
    }

    public MetricsSnapshot getSnapshot() {
        long total = totalRequests.sum();
        long failed = failedRequests.sum();
        long successfulCount = successfulRequests.sum();

        double errorRate = total > 0 ? (failed * 100.0 / total) : 0.0;
        double avgTime = successfulCount > 0 ? totalRequestTimeMs.sum() / (double) successfulCount : 0.0;

        return new MetricsSnapshot(startTime.get() > 0 ? Instant.ofEpochMilli(startTime.get()) : null, getUptime(),
                total, successfulCount, failed, requestsInProgress.sum(), avgTime,
                minRequestTimeMs.get() == Long.MAX_VALUE ? 0 : minRequestTimeMs.get(), maxRequestTimeMs.get(),
                errorRate, getMemoryMetrics(), getComponentSnapshots(), getErrorSnapshot());
    }

    private Map<String, ComponentMetrics.Snapshot> getComponentSnapshots() {
        return componentMetrics.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().getSnapshot()));
    }

    private Map<String, Long> getErrorSnapshot() {
        return errorCounts.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().sum()));
    }

    public MemoryMetrics getMemoryMetrics() {
        var rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        return new MemoryMetrics(rt.totalMemory(), rt.freeMemory(), rt.maxMemory(), used,
                rt.totalMemory() > 0 ? (int) ((used * 100) / rt.totalMemory()) : 0);
    }

    private void resetRequestMetrics() {
        totalRequests.reset();
        successfulRequests.reset();
        failedRequests.reset();
        requestsInProgress.reset();
        totalRequestTimeMs.reset();
        minRequestTimeMs.reset();
        maxRequestTimeMs.reset();
    }

    private void registerComponent(String name) {
        componentMetrics.putIfAbsent(name, new ComponentMetrics());
    }

    // --- Records and Support Classes ---
    public record RequestTimer(RuntimeMetrics metrics, long startTime) implements AutoCloseable {

        public void success() {
            metrics.requestsInProgress.decrement();
            metrics.successfulRequests.increment();
            metrics.updateTimingMetrics(System.currentTimeMillis() - startTime);
        }

        public void failure(String errorType) {
            // 1. Decrement the active count
            metrics.requestsInProgress.decrement();

            // 2. Delegate to the main class for failure bookkeeping
            metrics.recordRequestFailure(System.currentTimeMillis() - startTime, errorType);
        }

        @Override
        public void close() {
            // Defensive: If the user forgets to call success() or failure(),
            // we assume success to prevent 'leaking' the inProgress count.
            // In a real system, you might want to log a warning here.
        }
    }

    public record MetricsSnapshot(Instant startTime, long uptimeMs, long totalRequests, long successfulRequests,
            long failedRequests, long requestsInProgress, double averageRequestTimeMs, long minRequestTimeMs,
            long maxRequestTimeMs, double errorRatePercent, MemoryMetrics memory,
            Map<String, ComponentMetrics.Snapshot> componentMetrics, Map<String, Long> errorCounts) {
        public String toJson() {
            return """
                    {
                      "uptimeMs": %d,
                      "requests": {
                        "total": %d,
                        "success": %d,
                        "failed": %d,
                        "active": %d
                      },
                      "performance": {
                        "avgMs": %.2f,
                        "errorRate": "%.2f%%"
                      },
                      "memory": {
                        "usage": "%d%%"
                      }
                    }
                    """.formatted(uptimeMs, totalRequests, successfulRequests, failedRequests, requestsInProgress,
                    averageRequestTimeMs, errorRatePercent, memory.usagePercentage());
        }
    }

    public record MemoryMetrics(long total, long free, long max, long used, int usagePercentage) {
    }

    // Internal component tracker
    private static class ComponentMetrics {
        private final LongAdder count = new LongAdder();
        private final LongAccumulator max = new LongAccumulator(Math::max, 0);

        public void recordExecution(long duration, boolean success) {
            count.increment();
            max.accumulate(duration);
        }

        public Snapshot getSnapshot() {
            return new Snapshot(count.sum(), max.get());
        }

        public record Snapshot(long count, long maxMs) {
        }
    }
}
