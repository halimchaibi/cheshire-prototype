package io.cheshire.core.server.protocol;

import java.util.Map;

/**
 * <h1>RequestPayload</h1>
 *
 * <p>A <b>Standardized Data Container</b> that decouples the physical message format (e.g., JSON, XML)
 * from the semantic business logic arguments. It acts as the "Body" of the {@code RequestEnvelope}.</p>
 * *
 *
 * <h3>The Dual-Access Pattern</h3>
 * <p>This record provides two ways to consume data:</p>
 * <ul>
 * <li><b>Raw Access:</b> Via {@link #data()}, representing the fully parsed object (e.g., a POJO or JsonNode).</li>
 * <li><b>Key-Value Access:</b> Via {@link #parameters()}, facilitating quick access to flattened
 * arguments, essential for MCP Tool calls and Form data.</li>
 * </ul>
 *
 * <h3>Design Invariants</h3>
 * <ul>
 * <li><b>Null Safety:</b> Use the {@link #of(PayloadType, Object, Map, Map)} factory to ensure
 * null fields are replaced with {@link #NO_DATA} or empty collections.</li>
 * <li><b>Defensive Storage:</b> Internal maps are defensively copied into {@link java.util.HashMap}
 * during construction.</li>
 * </ul>
 *
 * <pre>{@code
 * // Accessing a specific parameter with type safety
 * Optional<String> query = payload.getParameter("sql_query", String.class);
 * * // Accessing the raw data object
 * MyCustomDto dto = payload.getDataAs(MyCustomDto.class);
 * }</pre>
 *
 * @param type       The {@link PayloadType} indicating how the source was encoded.
 * @param data       The primary object payload; defaults to {@link #NO_DATA}.
 * @param parameters An unmodifiable view of flattened arguments for the Action.
 * @param metadata   Contextual hints or schema information (e.g., validation rules).
 * @author Cheshire Framework
 * @since 1.0.0
 */
public record RequestPayload(
        PayloadType type,
        Object data,
        java.util.Map<String, Object> parameters,
        java.util.Map<String, Object> metadata
) {

    /**
     * Sentinel object used to signify the absence of data, avoiding null-checks.
     */
    public static final Object NO_DATA = new Object() {
        @Override
        public String toString() {
            return "NO_DATA";
        }
    };

    /**
     * Canonical constructor with defensive copying.
     */
    public RequestPayload(PayloadType type, Object data,
                          java.util.Map<String, Object> parameters,
                          java.util.Map<String, Object> metadata) {
        this.type = type;
        this.data = data;
        this.parameters = new java.util.HashMap<>(parameters != null ? parameters : java.util.Map.of());
        this.metadata = new java.util.HashMap<>(metadata != null ? metadata : java.util.Map.of());
    }

    /**
     * High-level factory to ensure record invariants.
     */
    public static RequestPayload of(PayloadType type, Object data,
                                    java.util.Map<String, Object> parameters,
                                    java.util.Map<String, Object> metadata) {
        return new RequestPayload(
                type != null ? type : PayloadType.UNKNOWN,
                data != null ? data : NO_DATA,
                parameters,
                metadata
        );
    }

    /**
     * Specialized factory for parameter-heavy calls (like MCP) where a raw body may be absent.
     */
    public static RequestPayload of(java.util.Map<String, Object> parameters,
                                    java.util.Map<String, Object> metadata) {
        return of(PayloadType.EMPTY, NO_DATA, parameters, metadata);
    }

    /**
     * Casts the raw data to a specific type.
     * * @param clazz The target class to cast to.
     *
     * @param <T> The expected return type.
     * @return The data casted to T.
     * @throws ClassCastException if the data is not compatible with the target class.
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataAs(Class<T> clazz) {
        return clazz.cast(data);
    }

    /**
     * Retrieves a parameter by key with explicit type casting.
     * * @param key  The parameter name.
     *
     * @param type The class of the expected type.
     * @return An {@link java.util.Optional} containing the casted value if present.
     */
    public <T> java.util.Optional<T> getParameter(String key, Class<T> type) {
        return java.util.Optional.ofNullable(parameters.get(key))
                .map(type::cast);
    }

    /**
     * Enumeration of supported payload formats.
     * Ranges from structured data (JSON/XML) to RPC protocols and low-level binary.
     */
    public enum PayloadType {
        JSON, XML, YAML, FORM_DATA, MULTIPART, RPC, JSON_RPC, GRPC,
        PROTOBUF, AVRO, GRAPHQL_QUERY, BINARY, TEXT, HTML, EMPTY, UNKNOWN
    }
}
