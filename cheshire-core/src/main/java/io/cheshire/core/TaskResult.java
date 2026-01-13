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

import io.cheshire.common.utils.MapUtils;
import io.cheshire.core.constant.Key;
import io.cheshire.core.server.ResponseEntity.Status;

import java.util.Map;

/**
 * <h1>TaskResult</h1>
 *
 * <p>
 * A <b>Sealed Algebraic Data Type (ADT)</b> representing the final outcome of a {@code CheshireSession} execution. It
 * encapsulates either a successful pipeline output or a categorized failure.
 * </p>
 *
 *
 *
 * <h3>The Success Record</h3>
 * <p>
 * The {@link Success} variant carries the primary domain {@code output}. It provides Scala-like convenience methods for
 * type-safe casting and metadata extraction, mirroring the API style found in {@code SessionTask}.
 * </p>
 *
 * <h3>The Failure Record</h3>
 * <p>
 * The {@link Failure} variant captures why a task could not be completed. It bridges the internal pipeline exceptions
 * with the standardized {@link Status} codes, allowing the orchestrator to decide how to report the error to the end
 * user.
 * </p>
 *
 * <h3>Design Invariants</h3>
 * <ul>
 * <li><b>Immutability:</b> Both records use {@link java.util.Map#copyOf(Map)} in their canonical constructors to ensure
 * that metadata cannot be mutated after the task completes.</li>
 * <li><b>Exhaustiveness:</b> Being {@code sealed}, this interface allows developers to use Java 21+ switch expressions
 * to handle results without a {@code default} clause.</li>
 * </ul>
 *
 * <pre>{@code
 * TaskResult result = session.execute(myTask);
 *
 * switch (result) {
 * case TaskResult.Success(var out, var meta) -> log.info("Task completed: {}", out);
 * case TaskResult.Failure(var status, var cause, var meta) -> log.error("Task failed with status: {}", status);
 * }
 * }</pre>
 *
 * @author Cheshire Framework
 * @since 1.0.0
 */

public sealed interface TaskResult permits TaskResult.Failure, TaskResult.Success {

    /**
     * Returns the immutable metadata associated with this result.
     *
     * @return A map of operational context.
     */
    java.util.Map<String, Object> metadata();

    /**
     * Represents a successful execution.
     *
     * @param output
     *            The primary data produced by the pipeline.
     * @param metadata
     *            Operational metadata (e.g., execution time, trace IDs).
     */
    record Success(Object output, java.util.Map<String, Object> metadata) implements TaskResult {
        public Success {
            metadata = java.util.Map.copyOf(metadata != null ? metadata : java.util.Map.of("empty-metadata", true));
        }

        /**
         * Casts the main output to a specific domain type.
         *
         * @param clazz
         *            The target class.
         * @return The output cast to the desired type.
         * @throws ClassCastException
         *             if the output is not compatible.
         */
        public <T> T outputAs(Class<T> clazz) {
            return clazz.cast(output);
        }

        /**
         * Safely retrieves a metadata value with type checking.
         *
         * @param key
         *            The metadata key.
         * @param clazz
         *            The expected type.
         * @return An Optional containing the casted value if present.
         */
        public <T> java.util.Optional<T> metaAs(String key, Class<T> clazz) {
            return java.util.Optional.ofNullable(metadata.get(key)).filter(clazz::isInstance).map(clazz::cast);
        }

        /**
         * "Fail-fast" metadata extraction. Use this for mandatory metadata like 'traceId' or 'tenantId'.
         */
        public <T> T requireMeta(String key, Class<T> clazz) {
            return metaAs(key, clazz).orElseThrow(() -> new IllegalArgumentException(
                    String.format("Missing or invalid metadata: '%s' (expected %s)", key, clazz.getSimpleName())));
        }

        public Success withDebugInfo(String key, Object value) {
            java.util.Map<String, Object> meta = new java.util.HashMap<>(this.metadata);
            MapUtils.putNested(meta, Key.DEBUG.key(), key, value);
            return new Success(this.output, meta);
        }
    }

    /**
     * Represents an execution failure.
     *
     * @param status
     *            The logical category of the error (e.g., BAD_REQUEST).
     * @param cause
     *            The underlying exception that triggered the failure.
     * @param metadata
     *            Optional context about the failure state.
     */
    record Failure(Status status, Throwable cause, java.util.Map<String, Object> metadata) implements TaskResult {
        public Failure {
            metadata = java.util.Map.copyOf(metadata != null ? metadata : java.util.Map.of());
        }

        /**
         * Static factory to create a standard failure without additional metadata.
         *
         * @param status
         *            The failure category.
         * @param cause
         *            The exception/cause.
         * @return A new Failure instance.
         */
        public static Failure of(Status status, Throwable cause) {
            return new Failure(status, cause, java.util.Map.of());
        }
    }
}
