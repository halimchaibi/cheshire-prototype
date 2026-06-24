/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.optimizer;

import io.cheshire.query.engine.calcite.query.QueryCharacteristics;
import io.cheshire.query.engine.calcite.query.QueryType;
import java.util.*;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.schema.Schema;

/**
 * Context information used to select appropriate optimization rules based on query characteristics
 * and schema metadata.
 */
public class OptimizationContext {

  private final QueryType queryType;
  private final Set<String> involvedSchemas;
  private final Map<String, Schema> schemaMap;
  private final QueryCharacteristics characteristics;
  private final Map<String, Object> hints;

  private OptimizationContext(Builder builder) {
    this.queryType = builder.queryType != null ? builder.queryType : QueryType.UNKNOWN;
    this.involvedSchemas = new HashSet<>(builder.involvedSchemas);
    this.schemaMap = new HashMap<>(builder.schemaMap);
    this.characteristics =
        builder.characteristics != null ? builder.characteristics : new QueryCharacteristics();
    this.hints = new HashMap<>(builder.hints);
  }

  public QueryType getQueryType() {
    return queryType;
  }

  public Set<String> getInvolvedSchemas() {
    return Collections.unmodifiableSet(involvedSchemas);
  }

  public Map<String, Schema> getSchemaMap() {
    return Collections.unmodifiableMap(schemaMap);
  }

  public QueryCharacteristics getCharacteristics() {
    return characteristics;
  }

  public boolean hasHint(String key) {
    return hints.containsKey(key);
  }

  public Object getHint(String key) {
    return hints.get(key);
  }

  public boolean isFederatedQuery() {
    return involvedSchemas.size() > 1;
  }

  public boolean isRemoteSchemaInvolved() {
    return schemaMap.values().stream().anyMatch(schema -> isRemoteSchema(schema));
  }

  private boolean isRemoteSchema(Schema schema) {
    // Built-in JDBC schemas
    if (schema instanceof JdbcSchema) {
      return true;
    }

    // Add support for  custom remote/federated schemas
    //    if (schema instanceof RestSchema) {
    //      return true;
    //    }
    //    if (schema instanceof SparkSchema) {
    //      return true;
    //    }

    // Default: local / in-memory schema
    return false;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private QueryType queryType;
    private Set<String> involvedSchemas = new HashSet<>();
    private Map<String, Schema> schemaMap = new HashMap<>();
    private QueryCharacteristics characteristics;
    private Map<String, Object> hints = new HashMap<>();

    public Builder withQueryType(QueryType type) {
      this.queryType = type;
      return this;
    }

    public Builder addSchema(String name, Schema schema) {
      this.involvedSchemas.add(name);
      this.schemaMap.put(name, schema);
      return this;
    }

    public Builder withSchemas(Map<String, Schema> schemas) {
      this.schemaMap.putAll(schemas);
      this.involvedSchemas.addAll(schemas.keySet());
      return this;
    }

    public Builder withCharacteristics(QueryCharacteristics characteristics) {
      this.characteristics = characteristics;
      return this;
    }

    public Builder addHint(String key, Object value) {
      this.hints.put(key, value);
      return this;
    }

    public OptimizationContext build() {
      return new OptimizationContext(this);
    }
  }
}
