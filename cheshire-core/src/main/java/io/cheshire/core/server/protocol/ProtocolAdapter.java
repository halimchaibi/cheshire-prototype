package io.cheshire.core.server.protocol;

import io.cheshire.core.server.ResponseEntity;

/**
 * <h1>ProtocolAdapter</h1>
 *
 * <p>The <b>Translation Bridge</b> between external wire protocols and the Cheshire Internal Core.
 * This interface is responsible for the bidirectional mapping of data, ensuring that the
 * {@code CheshireDispatcher} remains agnostic of the transport layer.</p>
 * *
 *
 * <h3>The Bidirectional Contract</h3>
 * <ul>
 * <li><b>Ingress (toRequestEnvelope):</b> Transforms a protocol-specific request
 * (e.g., {@code HttpServletRequest} or an MCP {@code CallToolRequest}) into the standardized
 * {@link RequestEnvelope}.</li>
 * <li><b>Egress (fromProcessingResult):</b> Transforms the internal {@link ResponseEntity}
 * back into a format the client understands (e.g., a JSON-RPC response or a REST JSON body).</li>
 * </ul>
 *
 * <h3>Error Handling</h3>
 * <p>Implementations must catch protocol-specific parsing or validation errors and rethrow
 * them as {@link ProtocolAdapterException}. This allows the jetty infrastructure to handle
 * mapping failures consistently across different transports.</p>
 *
 * <h3>Implementation Guidelines</h3>
 * <pre>{@code
 * // Example: An MCP implementation of this interface
 * public class McpProtocolAdapter implements ProtocolAdapter<CallToolRequest, CallToolResult> {
 * @Override
 * public RequestEnvelope toRequestEnvelope(CallToolRequest req) {
 * // Map MCP JSON-RPC to internal Envelope
 * }
 *
 * @Override
 * public CallToolResult fromProcessingResult(ResponseEntity result) {
 * // Map ResponseEntity Success/Failure to MCP tool result
 * }
 * }
 * }</pre>
 *
 * @param <I> The Inbound request type provided by the transport layer.
 * @param <O> The Outbound response type expected by the transport layer.
 *            * @author Cheshire Framework
 * @since 1.0.0
 */
public interface ProtocolAdapter<I, O> {

    /**
     * Maps an external protocol request to the standardized internal {@link RequestEnvelope}.
     *
     * @param request The raw request from the transport provider.
     * @return A fully populated internal request aggregate.
     * @throws ProtocolAdapterException if the transformation fails due to malformed input
     *                                  or incompatible schema.
     */
    RequestEnvelope toRequestEnvelope(I request, String capability) throws ProtocolAdapterException;

    /**
     * Transforms a backend {@link ResponseEntity} into a protocol-compliant result.
     * <p>
     * This method uses type inference to determine the appropriate return type (O)
     *
     * @param <O>    The expected result type.
     * @param req    The original request context used to guide the mapping logic.
     * @param result The internal response entity received from the dispatcher.
     * @return The formatted response object (O) ready to be serialized for the client.
     * @throws ProtocolAdapterException if the mapping fails or the request type is unsupported.
     */
    <O> O fromProcessingResult(I req, ResponseEntity result) throws ProtocolAdapterException;

    /**
     * Identifies the protocol type this adapter is designed to handle.
     * * @return A string identifier (e.g., "MCP", "REST", "GRPC").
     */
    String getProtocolType();

    /**
     * Defines the MIME types or content types supported by this adapter.
     * Defaults to {@code application/json}.
     *
     * @return An array of supported content type strings.
     */
    default String[] getSupportedContentTypes() {
        return new String[]{"application/json"};
    }
}
