/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.adapter;

import io.cheshire.common.utils.MapUtils;
import io.cheshire.spi.query.exception.QueryEngineConfigurationException;
import io.cheshire.spi.query.exception.QueryEngineInitializationException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.schema.TableFactory;

public final class TableFactorySourceAdapter implements SourceAdapter {

  private final String supportedType;
  private final String displayName;
  private final String tableFactoryClassName;

  public TableFactorySourceAdapter(
      final String supportedType, final String displayName, final String tableFactoryClassName) {
    this.supportedType = supportedType;
    this.displayName = displayName;
    this.tableFactoryClassName = tableFactoryClassName;
  }

  @Override
  public Schema createSchema(
      final String name, final Map<String, Object> config, final SchemaPlus schema)
      throws QueryEngineInitializationException {
    try {
      @SuppressWarnings("unchecked")
      final Map<String, Object> sourceConfig =
          MapUtils.someValueFromMapAs(config, "config", Map.class)
              .orElseThrow(
                  () ->
                      new QueryEngineConfigurationException(
                          "Source configuration missing, required 'config' field"));

      validate(sourceConfig);
      final List<Map<String, Object>> tableConfigs = tableConfigs(sourceConfig);
      final TableFactory<?> tableFactory = tableFactory();
      final Map<String, Table> tables =
          tableConfigs.stream()
              .collect(
                  Collectors.toUnmodifiableMap(
                      tableConfig -> tableName(tableConfig),
                      tableConfig ->
                          tableFactory.create(
                              schema, tableName(tableConfig), tableOperand(tableConfig), null)));

      return new StaticTableSchema(tables);
    } catch (Exception e) {
      throw new QueryEngineInitializationException(
          "Failed to create " + displayName + " schema for source: " + name, e);
    }
  }

  @Override
  public String supportedType() {
    return supportedType;
  }

  private List<Map<String, Object>> tableConfigs(final Map<String, Object> sourceConfig) {
    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> tables =
        MapUtils.someValueFromMapAs(sourceConfig, "tables", List.class)
            .filter(values -> !values.isEmpty())
            .orElseThrow(
                () ->
                    new IllegalArgumentException("Source config missing required 'tables' field"));
    return List.copyOf(tables);
  }

  private String tableName(final Map<String, Object> tableConfig) {
    return MapUtils.someValueFromMapAs(tableConfig, "name", String.class)
        .filter(name -> !name.isBlank())
        .orElseThrow(
            () -> new IllegalArgumentException("Table config missing required 'name' field"));
  }

  private Map<String, Object> tableOperand(final Map<String, Object> tableConfig) {
    @SuppressWarnings("unchecked")
    final Map<String, Object> nestedOperand =
        MapUtils.someValueFromMapAs(tableConfig, "operand", Map.class).orElse(Map.of());
    final Map<String, Object> topLevelOperand =
        tableConfig.entrySet().stream()
            .filter(entry -> !Set.of("name", "operand").contains(entry.getKey()))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    return java.util.stream.Stream.concat(
            nestedOperand.entrySet().stream(), topLevelOperand.entrySet().stream())
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private TableFactory<?> tableFactory() throws ReflectiveOperationException {
    final Class<?> factoryClass = Class.forName(tableFactoryClassName);
    final Optional<TableFactory<?>> singleton = singletonFactory(factoryClass);
    if (singleton.isPresent()) {
      return singleton.orElseThrow();
    }

    final Object instance = factoryClass.getDeclaredConstructor().newInstance();
    if (!TableFactory.class.isInstance(instance)) {
      throw new IllegalArgumentException(
          "Factory class " + factoryClass.getName() + " does not implement TableFactory");
    }
    return (TableFactory<?>) instance;
  }

  private Optional<TableFactory<?>> singletonFactory(final Class<?> factoryClass)
      throws IllegalAccessException {
    try {
      final Field instanceField = factoryClass.getField("INSTANCE");
      final Object instance = instanceField.get(null);
      return TableFactory.class.isInstance(instance)
          ? Optional.of((TableFactory<?>) instance)
          : Optional.empty();
    } catch (NoSuchFieldException e) {
      return Optional.empty();
    }
  }
}
