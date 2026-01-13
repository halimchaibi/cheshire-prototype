/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.pipeline;

import io.cheshire.spi.pipeline.CanonicalOutput;

import java.util.*;

/**
 * <h1>MaterializedOutput</h1>
 * <p>
 * Immutable output container for pipeline operations, carrying both the resulting data and associated metadata from
 * pipeline stages.
 * </p>
 *
 * <h3>Core Responsibilities</h3>
 * <ul>
 * <li><b>Result Transport:</b> Carries the processed output from pipeline transformations.</li>
 * <li><b>Metadata Propagation:</b> Maintains processing information (e.g., execution time, stage metrics, errors).</li>
 * <li><b>Immutability:</b> Ensures thread-safe consumption of pipeline results.</li>
 * <li><b>Functional Transformation:</b> Supports copy-on-write pattern via {@link #copy(Map, Map)}.</li>
 * </ul>
 *
 * <h3>Mutability Model</h3>
 * <p>
 * This record follows a <b>fully immutable</b> design:
 * </p>
 * <ul>
 * <li><b>Defensive Copying:</b> Maps are copied via {@link Map#copyOf(Map)} in the constructor.</li>
 * <li><b>Unmodifiable Views:</b> All accessors return unmodifiable maps.</li>
 * <li><b>Functional Updates:</b> Use {@link #withData(String, Object)} or {@link #withMetadata(String, Object)} to
 * create new instances with modifications.</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 *
 * <pre>{@code
 * // Create pipeline output
 * MaterializedOutput output = MaterializedOutput.of(Map.of("result", "processed text", "confidence", 0.95),
 *         Map.of("stage", "completion", "duration_ms", 150L));
 *
 * // Safe data access
 * output.getData("result", String.class).ifPresent(result -> logger.info("Result: {}", result));
 *
 * // Functional enrichment
 * MaterializedOutput enriched = output.withData("validated", true).withMetadata("validator", "schema_v2");
 *
 * // Type-safe metadata access
 * Long duration = output.getMetadata("duration_ms", Long.class).orElse(0L);
 *
 * // Check for errors
 * if (output.hasMetadata("error")) {
 *     output.getMetadata("error", String.class).ifPresent(error -> logger.error("Pipeline error: {}", error));
 * }
 * }</pre>
 *
 * <h3>MaterializedOutput vs MaterializedInput</h3>
 * <table border="1">
 * <tr>
 * <th>Aspect</th>
 * <th>MaterializedOutput</th>
 * <th>MaterializedInput</th>
 * </tr>
 * <tr>
 * <td>Purpose</td>
 * <td>Pipeline results</td>
 * <td>Pipeline inputs</td>
 * </tr>
 * <tr>
 * <td>Data Content</td>
 * <td>Processed/transformed data</td>
 * <td>Raw/source data</td>
 * </tr>
 * <tr>
 * <td>Metadata Content</td>
 * <td>Processing metrics, errors</td>
 * <td>Context, source info</td>
 * </tr>
 * <tr>
 * <td>Usage</td>
 * <td>Returned from stages</td>
 * <td>Passed to stages</td>
 * </tr>
 * </table>
 *
 * @param data
 *            Immutable map containing the pipeline result data.
 * @param metadata
 *            Immutable map containing processing metadata and metrics.
 * @author Cheshire Framework
 * @since 1.0.0
 */
public record MaterializedOutput(Map<String, Object> data,
        Map<String, Object> metadata) implements CanonicalOutput<MaterializedOutput> {

    public MaterializedOutput() {
        this(new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    /**
     * Compact constructor of unmodifiable views of LinkedHashMaps to preserve insertion order ensuring immutability and
     * null safety.
     * <p>
     * Both {@code data} and {@code metadata} are defensively copied using {@link Map#copyOf(Map)}, which creates
     * immutable maps. Null inputs are converted to empty immutable maps.
     * </p>
     *
     * @throws NullPointerException
     *             if either map contains null keys or values
     */
    public MaterializedOutput {
        data = ensureOrderedImmutable(data);
        metadata = ensureOrderedImmutable(metadata);
    }

    /**
     * Creates a new MaterializedOutput with the provided data and metadata.
     *
     * @param data
     *            The data map
     * @param metadata
     *            The metadata map
     * @return A new MaterializedOutput instance
     */
    public static MaterializedOutput of(LinkedHashMap<String, Object> data, LinkedHashMap<String, Object> metadata) {
        return new MaterializedOutput(data, metadata);
    }

    // ==================== Data Access Methods ====================

    /**
     * Creates a new MaterializedOutput with data only (empty metadata).
     *
     * @param data
     *            The data map
     * @return A new MaterializedOutput instance with empty metadata
     */
    public static MaterializedOutput ofData(LinkedHashMap<String, Object> data) {
        return new MaterializedOutput(data, new LinkedHashMap<>());
    }

    /**
     * Creates a MaterializedOutput representing an error condition.
     *
     * @param errorMessage
     *            The error message
     * @return A new MaterializedOutput with empty data and error metadata
     */
    public static MaterializedOutput error(String errorMessage) {
        return new MaterializedOutput(new LinkedHashMap<>(), new LinkedHashMap<>(Map.of("error", errorMessage)));
    }

    /**
     * Creates a MaterializedOutput representing an error with exception details.
     *
     * @param errorMessage
     *            The error message
     * @param exception
     *            The exception that occurred
     * @return A new MaterializedOutput with error metadata
     */
    public static MaterializedOutput error(String errorMessage, Throwable exception) {
        LinkedHashMap<String, Object> errorMeta = new LinkedHashMap<>(4);
        errorMeta.put("error", errorMessage);
        errorMeta.put("exception_type", exception.getClass().getName());
        errorMeta.put("exception_message", Objects.requireNonNullElse(exception.getMessage(), ""));

        return new MaterializedOutput(Collections.emptyMap(), errorMeta);
    }

    /**
     * Creates an empty MaterializedOutput.
     *
     * @return A new MaterializedOutput with empty data and metadata
     */
    public static MaterializedOutput empty() {
        return new MaterializedOutput(new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    // ==================== Metadata Access Methods ====================

    /**
     * Creates a functional copy with new data and metadata maps.
     * <p>
     * This method is required by {@link CanonicalOutput} and enables pipeline stages to create modified results without
     * mutating the original.
     * </p>
     *
     * @param data
     *            New data map (will be copied)
     * @param metadata
     *            New metadata map (will be copied)
     * @return A new MaterializedOutput instance with the provided maps
     */
    @Override
    public MaterializedOutput copy(Map<String, Object> data, Map<String, Object> metadata) {
        return new MaterializedOutput(data, metadata);
    }

    /**
     * Retrieves a data value wrapped in an {@link Optional}.
     *
     * @param key
     *            The data key
     * @return Optional containing the value if present, empty otherwise
     */
    public Optional<Object> getData(String key) {
        return Optional.ofNullable(data.get(key));
    }

    /**
     * Retrieves a typed data value.
     *
     * @param key
     *            The data key
     * @param type
     *            The expected type
     * @param <T>
     *            The data type
     * @return Optional containing the typed value if present and of correct type
     */
    public <T> Optional<T> getData(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * Retrieves a data value or returns a default.
     *
     * @param key
     *            The data key
     * @param defaultValue
     *            Value to return if key is absent
     * @param <T>
     *            The data type
     * @return The data value, or defaultValue if absent
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataOrDefault(String key, T defaultValue) {
        Object value = data.get(key);
        return value != null ? (T) value : defaultValue;
    }

    // ==================== Error Handling Utilities ====================

    /**
     * Checks if data contains the specified key.
     *
     * @param key
     *            The data key
     * @return true if the key exists in data
     */
    public boolean hasData(String key) {
        return data.containsKey(key);
    }

    /**
     * Retrieves a metadata value wrapped in an {@link Optional}.
     *
     * @param key
     *            The metadata key
     * @return Optional containing the value if present, empty otherwise
     */
    public Optional<Object> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    /**
     * Retrieves a typed metadata value.
     *
     * @param key
     *            The metadata key
     * @param type
     *            The expected type
     * @param <T>
     *            The metadata type
     * @return Optional containing the typed value if present and of correct type
     */
    public <T> Optional<T> getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    // ==================== Functional Mutation Methods ====================

    /**
     * Retrieves a metadata value or returns a default.
     *
     * @param key
     *            The metadata key
     * @param defaultValue
     *            Value to return if key is absent
     * @param <T>
     *            The metadata type
     * @return The metadata value, or defaultValue if absent
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadataOrDefault(String key, T defaultValue) {
        Object value = metadata.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Checks if metadata contains the specified key.
     *
     * @param key
     *            The metadata key
     * @return true if the key exists in metadata
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    /**
     * Checks if this output represents an error condition.
     * <p>
     * Conventionally, errors are indicated by an "error" key in metadata.
     * </p>
     *
     * @return true if metadata contains an "error" key
     */
    public boolean isError() {
        return hasMetadata("error");
    }

    /**
     * Retrieves the error message if present.
     *
     * @return Optional containing the error message, empty if no error
     */
    public Optional<String> getError() {
        return getMetadata("error", String.class);
    }

    /**
     * Checks if this output represents a successful operation.
     *
     * @return true if no error is present
     */
    public boolean isSuccess() {
        return !isError();
    }

    /**
     * Creates a new MaterializedOutput with an additional data entry.
     * <p>
     * This method implements functional mutation by creating a new instance with all existing data plus the new entry.
     * If the key exists, it will be replaced in the new instance.
     * </p>
     *
     * @param key
     *            The data key
     * @param value
     *            The data value
     * @return A new MaterializedOutput with the updated data
     */
    public MaterializedOutput withData(String key, Object value) {
        LinkedHashMap<String, Object> newData = new LinkedHashMap<>(this.data.size() + 1);
        newData.putAll(this.data);
        newData.put(key, value);
        return new MaterializedOutput(newData, this.metadata);
    }

    /**
     * Creates a new MaterializedOutput with additional data entries.
     *
     * @param additionalData
     *            Map of data entries to add
     * @return A new MaterializedOutput with the merged data
     */
    public MaterializedOutput withData(Map<String, Object> additionalData) {
        var newData = new LinkedHashMap<>(this.data);
        newData.putAll(additionalData);
        return new MaterializedOutput(newData, this.metadata);
    }

    /**
     * Creates a new MaterializedOutput with an additional metadata entry.
     * <p>
     * If the key exists, it will be replaced in the new instance.
     * </p>
     *
     * @param key
     *            The metadata key
     * @param value
     *            The metadata value
     * @return A new MaterializedOutput with the updated metadata
     */
    public MaterializedOutput withMetadata(String key, Object value) {
        var newMetadata = new LinkedHashMap<>(this.metadata);
        newMetadata.put(key, value);
        return new MaterializedOutput(this.data, newMetadata);
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a new MaterializedOutput with additional metadata entries.
     *
     * @param additionalMetadata
     *            Map of metadata entries to add
     * @return A new MaterializedOutput with the merged metadata
     */
    public MaterializedOutput withMetadata(Map<String, Object> additionalMetadata) {
        var newMetadata = new LinkedHashMap<>(this.metadata);
        newMetadata.putAll(additionalMetadata);
        return new MaterializedOutput(this.data, newMetadata);
    }

    /**
     * Creates a new MaterializedOutput with a data entry removed.
     *
     * @param key
     *            The data key to remove
     * @return A new MaterializedOutput without the specified data entry
     */
    public MaterializedOutput withoutData(String key) {
        var newData = new LinkedHashMap<>(this.data);
        newData.remove(key);
        return new MaterializedOutput(newData, this.metadata);
    }

    /**
     * Creates a new MaterializedOutput with a metadata entry removed.
     *
     * @param key
     *            The metadata key to remove
     * @return A new MaterializedOutput without the specified metadata entry
     */
    public MaterializedOutput withoutMetadata(String key) {
        var newMetadata = new LinkedHashMap<>(this.metadata);
        newMetadata.remove(key);
        return new MaterializedOutput(this.data, newMetadata);
    }

    /**
     * Creates a new MaterializedOutput with an error recorded in metadata.
     *
     * @param errorMessage
     *            The error message
     * @return A new MaterializedOutput with the error metadata
     */
    public MaterializedOutput withError(String errorMessage) {
        return withMetadata("error", errorMessage);
    }

    /**
     * Creates a new MaterializedOutput with an error and exception recorded.
     *
     * @param errorMessage
     *            The error message
     * @param exception
     *            The exception that occurred
     * @return A new MaterializedOutput with error metadata
     */
    public MaterializedOutput withError(String errorMessage, Throwable exception) {
        return withMetadata(Map.of("error", errorMessage, "exception_type", exception.getClass().getName(),
                "exception_message", exception.getMessage() != null ? exception.getMessage() : ""));
    }

    /**
     * Checks if both data and metadata are empty.
     *
     * @return true if both maps are empty
     */
    public boolean isEmpty() {
        return data.isEmpty() && metadata.isEmpty();
    }

    /**
     * Returns the number of data entries.
     *
     * @return The size of the data map
     */
    public int dataSize() {
        return data.size();
    }

    /**
     * Returns the number of metadata entries.
     *
     * @return The size of the metadata map
     */
    public int metadataSize() {
        return metadata.size();
    }

    private static Map<String, Object> ensureOrderedImmutable(Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }
        // If it's already an unmodifiable wrapper, we might still want to
        // ensure it's a LinkedHashMap internally to guarantee order.
        return Collections.unmodifiableMap(new LinkedHashMap<>(input));
    }
}
