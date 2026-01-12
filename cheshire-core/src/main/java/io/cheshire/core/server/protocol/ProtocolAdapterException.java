package io.cheshire.core.server.protocol;

/**
 * <h1>ProtocolAdapterException</h1>
 * * <p>Thrown by implementations of {@link ProtocolAdapter} when a bidirectional mapping
 * operation fails. This exception acts as a boundary marker between external transport
 * protocols and the Cheshire internal core.</p>
 * * <h3>Usage Scenarios</h3>
 * <ul>
 * <li><b>Ingress Failure:</b> Occurs when the inbound wire format (e.g., a JSON-RPC 2.0 request)
 * cannot be mapped to a {@link RequestEnvelope} due to missing mandatory fields or schema violations.</li>
 * <li><b>Egress Failure:</b> Occurs when an internal {@link io.cheshire.core.server.ResponseEntity}
 * contains data that cannot be serialized into the target protocol format.</li>
 * </ul>
 * *
 * * <h3>Strategic Importance</h3>
 * <p>By wrapping protocol-specific exceptions (like {@code JacksonException} or {@code MessagingException})
 * into this checked exception, the {@code ProtocolAdapter} ensures that the {@code CheshireDispatcher}
 * does not need to catch transport-specific errors, maintaining strict architectural decoupling.</p>
 * * @author Cheshire Framework
 *
 * @since 1.0.0
 */
public class ProtocolAdapterException extends Exception {

    /**
     * Constructs a new exception with a specific error message.
     * * @param message A descriptive summary of the mapping failure.
     */
    public ProtocolAdapterException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a message and the underlying cause.
     * <p>Use this constructor to wrap low-level parsing exceptions (e.g., from Jackson or Gson)
     * while preserving the original stack trace for debugging.</p>
     * * @param message A descriptive summary of the mapping failure.
     *
     * @param cause The original exception that triggered the failure.
     */
    public ProtocolAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
