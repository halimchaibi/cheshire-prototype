/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.schema;

import io.cheshire.common.utils.ObjectUtils;
import io.cheshire.spi.query.exception.QueryEngineInitializationException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.lookup.Lookup;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.util.NameMap;

public class SchemaManager {

  private final CalciteSchemaAdapter schemaAdapter;
  private SchemaPlus rootSchema;
  private final Map<String, Object> sources = new HashMap<>();

  public SchemaManager() {
    this.schemaAdapter = new CalciteSchemaAdapter();
  }

  /** Entry point for the Fluent API */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final SchemaManager instance = new SchemaManager();

    public Builder withRootSchema(SchemaPlus rootSchema) {
      instance.rootSchema = rootSchema;
      return this;
    }

    public Builder addSource(String key, Object config) {
      instance.sources.put(key, config);
      return this;
    }

    public Builder addSources(Map<String, Object> sources) {
      instance.sources.putAll(sources);
      return this;
    }

    /** The "Build" step: Validates state and initializes the schemas. */
    public SchemaManager build() throws QueryEngineInitializationException {
      if (instance.rootSchema == null) {
        throw new QueryEngineInitializationException("RootSchema must be set before building.");
      }

      if (!instance.sources.isEmpty()) {
        instance.registerSchemas(instance.sources);
      } else {
        throw new QueryEngineInitializationException("At least one source must be provided.");
      }
      return instance;
    }
  }

  private void registerSchemas(Map<String, Object> sources)
      throws QueryEngineInitializationException {

    for (Map.Entry<String, Object> entry : sources.entrySet()) {
      String name = entry.getKey();
      Object source = entry.getValue();

      @SuppressWarnings("unchecked")
      Map<String, Object> config =
          ObjectUtils.someObjectAs(source, Map.class)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Source config for '" + name + "' is missing required 'config' field"));

      Schema schema = schemaAdapter.createSchema(name, config, rootSchema);
      rootSchema.add(name, schema);
    }
  }

  public SchemaPlus rootSchema() {
    return rootSchema;
  }

  /**
   * Retrieves an unmodifiable view of all sub-schemas registered under the root schema. *
   *
   * <p>This method leverages the {@link Lookup} API to discover schema names and populates a {@link
   * NameMap} to ensure consistent name resolution. The resulting map is wrapped as unmodifiable to
   * prevent external structural modifications. * @return A non-null, immutable {@code Map<String,
   * Schema>} containing the mapping of sub-schema names to their respective {@link Schema}
   * instances.
   *
   * @throws RuntimeException if a sub-schema cannot be initialized during the lookup process.
   */
  public Map<String, Schema> schemas() {
    NameMap<Schema> nameMap = new NameMap<>();
    Lookup<? extends Schema> lookup = rootSchema.subSchemas();

    lookup.getNames(null).forEach(name -> nameMap.put(name, lookup.get(name)));

    return Collections.unmodifiableMap(nameMap.map());
  }

  /**
   * Gets the source configuration for a given source name.
   *
   * @param sourceName the name of the source
   * @return the source configuration map, or null if not found
   */
  public Map<String, Object> getSourceConfig(String sourceName) {
    Object sourceConfig = sources.get(sourceName);
    if (sourceConfig instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> config = (Map<String, Object>) sourceConfig;
      return config;
    }
    return null;
  }

  /**
   * Gets all registered source names.
   *
   * @return a set of source names
   */
  public java.util.Set<String> getSourceNames() {
    return java.util.Collections.unmodifiableSet(sources.keySet());
  }

  public void close() throws QueryEngineInitializationException {
    try {
      sources.clear();
      rootSchema = Frameworks.createRootSchema(true);
    } catch (Exception e) {
      throw new QueryEngineInitializationException("Failed to close source providers", e);
    }
  }
}
