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

/**
 * <h1>RequestHandlerException</h1> *
 * <p>
 * Thrown by implementations of {@link RequestHandler} when a functional or operational failure occurs during the
 * execution of business logic. This exception represents a <b>Logic Boundary</b> failure within the Cheshire core.
 * </p>
 * * *
 * <h3>Contextual Usage</h3>
 * <p>
 * This exception should be used to signal that the request was technically valid at the protocol level, but could not
 * be completed due to internal constraints, such as:
 * </p>
 * <ul>
 * <li><b>Validation Failures:</b> Business rules were violated (e.g., "Insufficient funds").</li>
 * <li><b>Dependency Failures:</b> An internal service, database, or LLM returned an error.</li>
 * <li><b>Execution Timeouts:</b> The logic took longer than the allocated deadline.</li>
 * </ul>
 * *
 * <h3>Integration with ResponseEntity</h3>
 * <p>
 * The {@code CheshireDispatcher} typically catches this exception and maps it to a
 * {@link io.cheshire.core.server.ResponseEntity.Failure} with a status of {@code EXECUTION_FAILED} or
 * {@code BAD_REQUEST}, depending on the root cause.
 * </p>
 * *
 *
 * <pre>{@code
 * @Override
 * public ResponseEntity handle(RequestEnvelope env) throws RequestHandlerException {
 *     try {
 *         var result = tool.execute(env.payload().parameters());
 *         return ResponseEntity.ok(result);
 *     } catch (Exception e) {
 *         // Wrap internal errors to maintain the RequestHandler contract
 *         throw new RequestHandlerException("Tool execution failed: " + e.getMessage(), e);
 *     }
 * }
 * }</pre>
 *
 * * @author Cheshire Framework
 *
 * @since 1.0.0
 */
public class RequestHandlerException extends Exception {

    /**
     * Constructs a new exception with a specific error message. * @param message A human-readable description of the
     * logic failure.
     */
    public RequestHandlerException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a message and the underlying cause.
     * <p>
     * Use this constructor when the failure is triggered by a third-party library (e.g., a JDBC {@code SQLException} or
     * an AI SDK error) to preserve the stack trace.
     * </p>
     * * @param message A descriptive summary of the failure.
     *
     * @param cause
     *            The underlying exception that caused the logic to fail.
     */
    public RequestHandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
