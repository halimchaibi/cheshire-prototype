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

import io.cheshire.query.engine.calcite.schema.SchemaManager;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.schema.SchemaPlus;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Production-grade DataContext implementation for a federated Calcite engine. Holds: - Named
 * sources (JDBC, Spark, Elasticsearch, etc.) - Query parameters - Execution metadata (timezone,
 * locale)
 */
public class CalciteDataContext implements DataContext {
  private final Map<String, Object> variables;
  private Map<String, Object> values;
  private SchemaManager schemaManager;

  public CalciteDataContext(SchemaManager schemaManager) {

    long now = System.currentTimeMillis();

    variables = Map.of();

    values =
        Map.of(
            Variable.CURRENT_TIMESTAMP.camelName,
            now,
            Variable.LOCAL_TIMESTAMP.camelName,
            now,
            Variable.UTC_TIMESTAMP.camelName,
            now,
            Variable.TIME_ZONE.camelName,
            TimeZone.getDefault(),
            Variable.LOCALE.camelName,
            Locale.ROOT,
            Variable.CANCEL_FLAG.camelName,
            new AtomicBoolean(false));
  }

  /**
   * Main constructor
   *
   * @param sources Named runtime objects (DataSource, Spark, etc.)
   * @param parameters Query parameters
   * @param timeZone Execution timezone
   * @param locale Execution locale
   */
  public CalciteDataContext(
      Map<String, Object> sources,
      Map<String, Object> parameters,
      TimeZone timeZone,
      Locale locale) {
    Map<String, Object> vars = new HashMap<>();

    if (sources != null) vars.putAll(sources);
    if (parameters != null) vars.putAll(parameters);

    // Standard Calcite variables
    vars.putIfAbsent(
        DataContext.Variable.TIME_ZONE.camelName,
        timeZone != null ? timeZone : TimeZone.getDefault());
    vars.putIfAbsent(
        DataContext.Variable.LOCALE.camelName, locale != null ? locale : Locale.getDefault());
    vars.putIfAbsent(DataContext.Variable.CURRENT_TIMESTAMP.camelName, System.currentTimeMillis());
    vars.putIfAbsent(DataContext.Variable.UTC_TIMESTAMP.camelName, System.currentTimeMillis());
    vars.putIfAbsent(DataContext.Variable.LOCAL_TIMESTAMP.camelName, System.currentTimeMillis());
    vars.putIfAbsent(DataContext.Variable.CANCEL_FLAG.camelName, new AtomicBoolean(false));

    this.variables = Collections.unmodifiableMap(vars);
  }

  @Override
  public SchemaPlus getRootSchema() {
    return schemaManager.rootSchema();
  }

  @Override
  public JavaTypeFactory getTypeFactory() {
    return new JavaTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
  }

  @Override
  public QueryProvider getQueryProvider() {
    return null; // TODO:
  }

  /**
   * Returns a context variable.
   *
   * <p>Supported variables include: "sparkContext", "currentTimestamp", "localTimestamp".
   *
   * @param name Name of variable
   */
  @Override
  public @Nullable Object get(String name) {
    return variables.get(name);
  }

  // Utility methods
  public boolean hasSource(String name) {
    return variables.containsKey(name);
  }

  @SuppressWarnings("unchecked")
  public <T> T getSource(String name, Class<T> clazz) {
    Object obj = variables.get(name);
    if (obj == null) {
      throw new IllegalArgumentException("No source found for name: " + name);
    }
    return (T) clazz.cast(obj);
  }

  public Map<String, Object> allSources() {
    return Collections.unmodifiableMap(variables);
  }
}
