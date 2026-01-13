/*-
 * #%L
 * Cheshire :: Runtime
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

/**
 * Runtime monitoring components for health tracking and performance metrics.
 * <p>
 * <strong>Package Overview:</strong>
 * <p>
 * This package provides production-ready monitoring infrastructure:
 * <ul>
 * <li><strong>RuntimeHealth</strong> - State machine-based health tracking</li>
 * <li><strong>RuntimeMetrics</strong> - Lock-free performance metrics</li>
 * </ul>
 * <p>
 * <strong>Health Monitoring:</strong>
 * <p>
 * The {@link io.cheshire.runtime.monitoring.RuntimeHealth} class tracks runtime state with validation:
 *
 * <pre>
 * State Machine:
 *   NEW → STARTING → RUNNING → STOPPING → STOPPED
 *
 * Valid Transitions:
 *   NEW → STARTING
 *   STARTING → RUNNING
 *   RUNNING → STOPPING
 *   STOPPING → STOPPED
 *
 * Invalid transitions throw IllegalStateException
 * </pre>
 * <p>
 * <strong>Example Usage:</strong>
 *
 * <pre>{@code
 * RuntimeHealth health = new RuntimeHealth();
 *
 * // Record state transitions
 * health.setState(RuntimeHealth.Status.STARTING);
 * health.setState(RuntimeHealth.Status.RUNNING);
 *
 * // Record events
 * health.recordEvent("Server started successfully");
 *
 * // Check health
 * if (health.isHealthy()) {
 *     // System operational
 * }
 *
 * // Get health report
 * Map<String, Object> report = health.healthReport();
 * }</pre>
 * <p>
 * <strong>Performance Metrics:</strong>
 * <p>
 * The {@link io.cheshire.runtime.monitoring.RuntimeMetrics} class provides lock-free metrics optimized for Virtual
 * Threads:
 * <ul>
 * <li>Request counts (total, success, failure)</li>
 * <li>Timing (start time, uptime)</li>
 * <li>Memory usage (heap, non-heap)</li>
 * <li>Component-specific metrics</li>
 * </ul>
 * <p>
 * <strong>Lock-Free Design:</strong>
 * <p>
 * Uses {@link java.util.concurrent.atomic.LongAdder} and {@link java.util.concurrent.atomic.LongAccumulator} for
 * zero-contention updates in Virtual Thread environments:
 *
 * <pre>{@code
 * RuntimeMetrics metrics = new RuntimeMetrics();
 *
 * // Lock-free increment (safe from 10,000+ Virtual Threads)
 * metrics.incrementRequests();
 * metrics.recordSuccess();
 *
 * // Atomic timing
 * long start = System.nanoTime();
 * // ... operation ...
 * metrics.recordDuration(System.nanoTime() - start);
 *
 * // Get snapshot (linearizable reads)
 * Map<String, Object> snapshot = metrics.snapshot();
 * }</pre>
 * <p>
 * <strong>Integration with Monitoring Systems:</strong>
 * <p>
 * Metrics can be exported to external monitoring systems:
 *
 * <pre>{@code
 * // Prometheus-style metrics
 * cheshire_requests_total{status="success"} 1000
 * cheshire_requests_total{status="failure"} 10
 * cheshire_uptime_seconds 3600
 * cheshire_memory_used_bytes{type="heap"} 104857600
 * }</pre>
 * <p>
 * <strong>Health Check Endpoints:</strong>
 * <p>
 * Health and metrics can be exposed via HTTP endpoints:
 *
 * <pre>{@code
 * GET /health
 * {
 *   "status": "RUNNING",
 *   "healthy": true,
 *   "uptime": "1h 30m",
 *   "components": {
 *     "database": "healthy",
 *     "cache": "healthy"
 *   }
 * }
 *
 * GET /metrics
 * {
 *   "requests": {"total": 1010, "success": 1000, "failure": 10},
 *   "uptime": 5400,
 *   "memory": {"heap": 104857600, "nonHeap": 52428800}
 * }
 * }</pre>
 * <p>
 * <strong>Design Patterns:</strong>
 * <ul>
 * <li><strong>State Pattern:</strong> Health state machine with validation</li>
 * <li><strong>Observer:</strong> Event recording and notification</li>
 * <li><strong>Strategy:</strong> Pluggable metric collectors</li>
 * <li><strong>Snapshot:</strong> Consistent metrics snapshots</li>
 * </ul>
 * <p>
 * <strong>Performance Considerations:</strong>
 * <ul>
 * <li>Lock-free operations for Virtual Thread scalability</li>
 * <li>Minimal allocation (reuse collections)</li>
 * <li>Fast-path optimizations for common operations</li>
 * <li>Linearizable reads despite concurrent writes</li>
 * </ul>
 *
 * @see io.cheshire.runtime.monitoring.RuntimeHealth
 * @see io.cheshire.runtime.monitoring.RuntimeMetrics
 * @see io.cheshire.runtime.CheshireRuntime
 * @since 1.0.0
 */
package io.cheshire.runtime.monitoring;
