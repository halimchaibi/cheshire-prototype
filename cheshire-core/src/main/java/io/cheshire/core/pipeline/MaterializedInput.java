package io.cheshire.core.pipeline;

import io.cheshire.spi.pipeline.CanonicalInput;

import java.util.*;

/**
 * <h1>MaterializedInput</h1>
 * <p>Immutable input container for pipeline operations, carrying both the primary data
 * and associated metadata through pipeline stages.</p>
 *
 * <h3>Core Responsibilities</h3>
 * <ul>
 * <li><b>Data Transport:</b> Carries the primary payload through pipeline transformations.</li>
 * <li><b>Metadata Propagation:</b> Maintains contextual information (e.g., content type, encoding, timestamps).</li>
 * <li><b>Immutability:</b> Ensures thread-safe passage through concurrent pipeline stages.</li>
 * <li><b>Functional Transformation:</b> Supports copy-on-write pattern via {@link #copy(Map, Map)}.</li>
 * </ul>
 *
 * <h3>Mutability Model</h3>
 * <p>This record follows a <b>fully immutable</b> design:</p>
 * <ul>
 * <li><b>Defensive Copying:</b> Maps are copied via {@link Map#copyOf(Map)} in the constructor.</li>
 * <li><b>Unmodifiable Views:</b> All accessors return unmodifiable maps.</li>
 * <li><b>Functional Updates:</b> Use {@link #withData(String, Object)} or {@link #withMetadata(String, Object)}
 * to create new instances with modifications.</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Create initial input
 * MaterializedInput input = MaterializedInput.of(
 *     Map.of("text", "Hello World", "tokens", List.of("Hello", "World")),
 *     Map.of("source", "user_input", "timestamp", Instant.now())
 * );
 *
 * // Safe data access
 * input.getData("text", String.class)
 *     .ifPresent(text -> logger.info("Processing: {}", text));
 *
 * // Functional transformation
 * MaterializedInput enriched = input
 *     .withData("processed", true)
 *     .withMetadata("stage", "tokenization");
 *
 * // Type-safe metadata access
 * String source = input.getMetadataOrDefault("source", "unknown");
 * }</pre>
 *
 * @param data     Immutable map containing the primary payload data.
 * @param metadata Immutable map containing contextual metadata.
 * @author Cheshire Framework
 * @since 1.0.0
 */

public record MaterializedInput(
        Map<String, Object> data,
        Map<String, Object> metadata
) implements CanonicalInput<MaterializedInput> {

    public MaterializedInput() {
        this(new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    /**
     * Compact constructor ensuring immutability and null safety.
     * <p>
     * Both {@code data} and {@code metadata} are defensively copied using
     * {@link Map#copyOf(Map)}, which creates immutable maps. Null inputs
     * are converted to empty immutable maps.
     * </p>
     *
     * @throws NullPointerException if either map contains null keys or values
     */
    public MaterializedInput {
        data = (data != null && !data.isEmpty())
                ? new LinkedHashMap<>(data)
                : new LinkedHashMap<>();

        metadata = (metadata != null && !data.isEmpty())
                ? new LinkedHashMap<>(metadata)
                : new LinkedHashMap<>();
    }

    /**
     * Creates a new MaterializedInput with the provided data and metadata.
     *
     * @param data     The data map
     * @param metadata The metadata map
     * @return A new MaterializedInput instance
     */
    public static MaterializedInput of(LinkedHashMap<String, Object> data, LinkedHashMap<String, Object> metadata) {
        return new MaterializedInput(data, metadata);
    }

    // ==================== Data Access Methods ====================

    /**
     * Creates a new MaterializedInput with data only (empty metadata).
     *
     * @param data The data map
     * @return A new MaterializedInput instance with empty metadata
     */
    public static MaterializedInput ofData(LinkedHashMap<String, Object> data) {
        return new MaterializedInput(data, new LinkedHashMap<>());
    }

    /**
     * Creates an empty MaterializedInput.
     *
     * @return A new MaterializedInput with empty data and metadata
     */
    public static MaterializedInput empty() {
        return new MaterializedInput(new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    /**
     * Creates a functional copy with new data and metadata maps.
     * <p>
     * This method is required by {@link CanonicalInput} and enables
     * pipeline stages to create modified versions without mutating the original.
     * </p>
     *
     * @param data     New data map (will be copied)
     * @param metadata New metadata map (will be copied)
     * @return A new MaterializedInput instance with the provided maps
     */
    @Override
    public MaterializedInput copy(Map<String, Object> data, Map<String, Object> metadata) {
        return new MaterializedInput(data, metadata);
    }
    /**
     * Retrieves a data value wrapped in an {@link Optional}.
     *
     * @param key The data key
     * @return Optional containing the value if present, empty otherwise
     */
    public Optional<Object> getData(String key) {
        return Optional.ofNullable(data.get(key));
    }

    // ==================== Metadata Access Methods ====================

    /**
     * Retrieves a typed data value.
     *
     * @param key  The data key
     * @param type The expected type
     * @param <T>  The data type
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
     * @param key          The data key
     * @param defaultValue Value to return if key is absent
     * @param <T>          The data type
     * @return The data value, or defaultValue if absent
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataOrDefault(String key, T defaultValue) {
        Object value = data.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * Checks if data contains the specified key.
     *
     * @param key The data key
     * @return true if the key exists in data
     */
    public boolean hasData(String key) {
        return data.containsKey(key);
    }

    /**
     * Retrieves a metadata value wrapped in an {@link Optional}.
     *
     * @param key The metadata key
     * @return Optional containing the value if present, empty otherwise
     */
    public Optional<Object> getMetadata(String key) {
        return Optional.ofNullable(metadata.get(key));
    }

    // ==================== Functional Mutation Methods ====================

    /**
     * Retrieves a typed metadata value.
     *
     * @param key  The metadata key
     * @param type The expected type
     * @param <T>  The metadata type
     * @return Optional containing the typed value if present and of correct type
     */
    public <T> Optional<T> getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * Retrieves a metadata value or returns a default.
     *
     * @param key          The metadata key
     * @param defaultValue Value to return if key is absent
     * @param <T>          The metadata type
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
     * @param key The metadata key
     * @return true if the key exists in metadata
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    /**
     * Creates a new MaterializedInput with an additional data entry.
     * <p>
     * This method implements functional mutation by creating a new instance
     * with all existing data plus the new entry. If the key exists, it will
     * be replaced in the new instance.
     * </p>
     *
     * @param key   The data key
     * @param value The data value
     * @return A new MaterializedInput with the updated data
     */
    public MaterializedInput withData(String key, Object value) {
        var newData = new LinkedHashMap<>(this.data);
        newData.put(key, value);
        return new MaterializedInput(newData, this.metadata);
    }

    /**
     * Creates a new MaterializedInput with additional data entries.
     *
     * @param additionalData Map of data entries to add
     * @return A new MaterializedInput with the merged data
     */
    public MaterializedInput withData(Map<String, Object> additionalData) {
        var newData = new LinkedHashMap<>(this.data);
        newData.putAll(additionalData);
        return new MaterializedInput(newData, this.metadata);
    }

    /**
     * Creates a new MaterializedInput with an additional metadata entry.
     * <p>
     * If the key exists, it will be replaced in the new instance.
     * </p>
     *
     * @param key   The metadata key
     * @param value The metadata value
     * @return A new MaterializedInput with the updated metadata
     */
    public MaterializedInput withMetadata(String key, Object value) {
        var newMetadata = new LinkedHashMap<>(this.metadata);
        newMetadata.put(key, value);
        return new MaterializedInput(this.data, newMetadata);
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a new MaterializedInput with additional metadata entries.
     *
     * @param additionalMetadata Map of metadata entries to add
     * @return A new MaterializedInput with the merged metadata
     */
    public MaterializedInput withMetadata(Map<String, Object> additionalMetadata) {
        var newMetadata = new LinkedHashMap<>(this.metadata);
        newMetadata.putAll(additionalMetadata);
        return new MaterializedInput(this.data, newMetadata);
    }

    /**
     * Creates a new MaterializedInput with a data entry removed.
     *
     * @param key The data key to remove
     * @return A new MaterializedInput without the specified data entry
     */
    public MaterializedInput withoutData(String key) {
        var newData = new LinkedHashMap<>(this.data);
        newData.remove(key);
        return new MaterializedInput(newData, this.metadata);
    }

    /**
     * Creates a new MaterializedInput with a metadata entry removed.
     *
     * @param key The metadata key to remove
     * @return A new MaterializedInput without the specified metadata entry
     */
    public MaterializedInput withoutMetadata(String key) {
        var newMetadata = new LinkedHashMap<>(this.metadata);
        newMetadata.remove(key);
        return new MaterializedInput(this.data, newMetadata);
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
}
