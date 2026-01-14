/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.server;

import java.util.Map;

/**
 * <h1>ResponseEntity</h1> *
 * <p>
 * A <b>Sealed Algebraic Data Type (ADT)</b> representing the immutable outcome of a Cheshire operation. This interface
 * is the standard return type for the {@code CheshireDispatcher} and all {@code Action} implementations.
 * </p>
 * *
 * <h3>Architectural Intent</h3>
 * <p>
 * By leveraging Java's {@code sealed} interface and {@code record} features, this type ensures <b>exhaustive error
 * handling</b>. Callers are forced by the compiler to handle both the {@link Success} and {@link Failure} cases,
 * eliminating the risk of unhandled runtime exceptions or "null-check" programming styles.
 * </p>
 * *
 * <h3>Usage Examples</h3> *
 *
 * <pre>{@code
 * // 1. Creating a successful response
 * ResponseEntity response = ResponseEntity.ok(myData);
 * * // 2. Creating a failure response
 * ResponseEntity error = ResponseEntity.error(Status.NOT_FOUND, "Entity not found");
 * * // 3. Exhaustive Pattern Matching (Java 21+)
 * String result = switch (response) {
 * case ResponseEntity.Success(var data, var meta) -> "Success: " + data;
 * case ResponseEntity.Failure(var status, var err, var msg) -> "Error: " + msg;
 * };
 * }</pre>
 *
 * @author Cheshire Framework
 * @since 1.0.0
 */
public sealed interface ResponseEntity permits ResponseEntity.Success, ResponseEntity.Failure {

    /**
     * Factory method to create a {@link Success} response with empty metadata. * @param data The operational payload.
     *
     * @return A Success instance.
     */
    static ResponseEntity ok(Object data) {
        return new Success(data, java.util.Map.of());
    }

    /**
     * Factory method to create a {@link Success} response with custom metadata.
     *
     * @param data
     *            The operational payload.
     * @param metadata
     *            Map of non-domain context (e.g., pagination, trace IDs).
     * @return A Success instance.
     */
    static ResponseEntity ok(Object data, java.util.Map<String, Object> metadata) {
        return new Success(data, metadata);
    }

    /**
     * Factory method to create a {@link Failure} response including the original Throwable. * @param status The logical
     * error category.
     *
     * @param error
     *            The cause of the failure (can be null).
     * @return A Failure instance.
     */
    static ResponseEntity error(Status status, Throwable error) {
        return new Failure(status, error, error != null ? error.getMessage() : "Unknown error");
    }

    /**
     * Factory method to create a {@link Failure} response with a custom message. * @param status The logical error
     * category.
     *
     * @param message
     *            Human-readable error details.
     * @return A Failure instance.
     */
    static ResponseEntity error(Status status, String message) {
        return new Failure(status, null, message);
    }

    /**
     * Defines the logical categories for Cheshire operations. These are mapped to protocol-specific codes (HTTP or MCP
     * JSON-RPC) by the adapter layer.
     */
    enum Status {
        /**
         * Operation completed successfully.
         */
        SUCCESS,
        /**
         * Invalid input or malformed request parameters.
         */
        BAD_REQUEST,
        /**
         * Identity could not be verified.
         */
        UNAUTHORIZED,
        /**
         * Permission denied for the requested resource.
         */
        FORBIDDEN,
        /**
         * The requested entity or tool does not exist.
         */
        NOT_FOUND,
        /**
         * The logic crashed during execution (e.g., DB or LLM error).
         */
        EXECUTION_FAILED,
        /**
         * An external dependency is currently unreachable.
         */
        SERVICE_UNAVAILABLE
    }

    /**
     * Represents a successful operation outcome. * @param data The primary payload produced by the Action.
     *
     * @param metadata
     *            Contextual key-value pairs (e.g., execution metrics).
     */
    record Success(
            Object data,
            Map<String, Object> metadata) implements ResponseEntity {
    }

    /**
     * Represents a failed operation outcome. * @param status The category of failure for routing and logic.
     *
     * @param error
     *            The captured exception, if any.
     * @param message
     *            A descriptive summary of why the operation failed.
     */
    record Failure(
            Status status,
            Throwable error,
            String message) implements ResponseEntity {
    }
}
