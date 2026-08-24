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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.calcite.model.JsonCustomTable;
import org.apache.calcite.model.JsonStream;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaFactory;
import org.apache.calcite.schema.SchemaPlus;

public final class SchemaFactorySourceAdapter implements SourceAdapter {

  private final AdapterSpec spec;

  private SchemaFactorySourceAdapter(final AdapterSpec spec) {
    this.spec = spec;
  }

  public static SchemaFactorySourceAdapter of(
      final String supportedType,
      final String displayName,
      final String factoryClassName,
      final List<String> requiredKeys,
      final List<String> requiredAnyKeys,
      final List<String> directoryKeys,
      final List<String> fileListKeys) {
    return new SchemaFactorySourceAdapter(
        new AdapterSpec(
            supportedType,
            displayName,
            Optional.of(factoryClassName),
            false,
            false,
            requiredKeys,
            requiredAnyKeys,
            directoryKeys,
            fileListKeys));
  }

  public static SchemaFactorySourceAdapter withJsonCustomTables(
      final String supportedType,
      final String displayName,
      final String factoryClassName,
      final List<String> requiredKeys,
      final List<String> requiredAnyKeys,
      final List<String> directoryKeys,
      final List<String> fileListKeys) {
    return new SchemaFactorySourceAdapter(
        new AdapterSpec(
            supportedType,
            displayName,
            Optional.of(factoryClassName),
            false,
            true,
            requiredKeys,
            requiredAnyKeys,
            directoryKeys,
            fileListKeys));
  }

  public static SchemaFactorySourceAdapter plugin(
      final String supportedType, final String displayName) {
    return new SchemaFactorySourceAdapter(
        new AdapterSpec(
            supportedType,
            displayName,
            Optional.empty(),
            true,
            false,
            List.of("factoryClass"),
            List.of(),
            List.of(),
            List.of()));
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
      validateRequiredFields(sourceConfig);
      validatePaths(sourceConfig);

      return schemaFactory(sourceConfig).create(schema, name, operandFrom(sourceConfig));
    } catch (Exception e) {
      throw new QueryEngineInitializationException(
          "Failed to create " + spec.displayName() + " schema for source: " + name, e);
    }
  }

  @Override
  public String supportedType() {
    return spec.supportedType();
  }

  private void validateRequiredFields(final Map<String, Object> config)
      throws QueryEngineConfigurationException {
    spec.requiredKeys().forEach(key -> requirePresent(config, key));

    if (!spec.requiredAnyKeys().isEmpty()
        && spec.requiredAnyKeys().stream().noneMatch(key -> isPresent(config.get(key)))) {
      throw new QueryEngineConfigurationException(
          "Source config requires one of these fields: " + spec.requiredAnyKeys());
    }
  }

  private void validatePaths(final Map<String, Object> config)
      throws QueryEngineConfigurationException {
    spec.directoryKeys().stream()
        .filter(config::containsKey)
        .map(key -> Map.entry(key, valueAsString(config, key)))
        .forEach(entry -> requireDirectory(entry.getKey(), entry.getValue()));

    spec.fileListKeys().stream()
        .filter(config::containsKey)
        .forEach(key -> requireExistingFiles(key, config.get(key)));
  }

  private void requirePresent(final Map<String, Object> config, final String key) {
    if (!isPresent(config.get(key))) {
      throw new IllegalArgumentException("Source config missing required '" + key + "' field");
    }
  }

  private boolean isPresent(final Object value) {
    return switch (value) {
      case null -> false;
      case String text -> !text.isBlank();
      case List<?> values -> !values.isEmpty();
      case Map<?, ?> values -> !values.isEmpty();
      default -> true;
    };
  }

  private String valueAsString(final Map<String, Object> config, final String key) {
    return MapUtils.someValueFromMapAs(config, key, String.class)
        .filter(value -> !value.isBlank())
        .orElseThrow(
            () ->
                new IllegalArgumentException("Source config missing required '" + key + "' field"));
  }

  private void requireDirectory(final String key, final String value) {
    final Path path = Path.of(value).toAbsolutePath().normalize();
    if (!Files.isDirectory(path)) {
      throw new IllegalArgumentException(
          "Source config field '" + key + "' is not an existing directory: " + path);
    }
  }

  private void requireExistingFiles(final String key, final Object value) {
    if (!(value instanceof List<?> paths)) {
      throw new IllegalArgumentException("Source config field '" + key + "' must be a list");
    }

    paths.stream()
        .map(String.class::cast)
        .map(Path::of)
        .map(Path::toAbsolutePath)
        .map(Path::normalize)
        .filter(path -> !Files.isRegularFile(path))
        .findFirst()
        .ifPresent(
            path -> {
              throw new IllegalArgumentException(
                  "Source config field '" + key + "' contains a non-existing file: " + path);
            });
  }

  private SchemaFactory schemaFactory(final Map<String, Object> config)
      throws ReflectiveOperationException {
    final String factoryClassName =
        spec.factoryClassName().orElseGet(() -> valueAsString(config, "factoryClass"));
    final Class<?> factoryClass = Class.forName(factoryClassName);

    return factoryInstance(factoryClass, SchemaFactory.class);
  }

  private <T> T factoryInstance(final Class<?> factoryClass, final Class<T> targetType)
      throws ReflectiveOperationException {
    final Optional<T> singleton = singletonFactory(factoryClass, targetType);
    if (singleton.isPresent()) {
      return singleton.orElseThrow();
    }

    final Object instance = factoryClass.getDeclaredConstructor().newInstance();
    if (!targetType.isInstance(instance)) {
      throw new IllegalArgumentException(
          "Factory class "
              + factoryClass.getName()
              + " does not implement "
              + targetType.getName());
    }
    return targetType.cast(instance);
  }

  private <T> Optional<T> singletonFactory(final Class<?> factoryClass, final Class<T> targetType)
      throws IllegalAccessException {
    try {
      final Field instanceField = factoryClass.getField("INSTANCE");
      final Object instance = instanceField.get(null);
      return targetType.isInstance(instance)
          ? Optional.of(targetType.cast(instance))
          : Optional.empty();
    } catch (NoSuchFieldException e) {
      return Optional.empty();
    }
  }

  private Map<String, Object> operandFrom(final Map<String, Object> config) {
    if (spec.usesNestedOperand()) {
      @SuppressWarnings("unchecked")
      final Map<String, Object> operand =
          MapUtils.someValueFromMapAs(config, "operand", Map.class).orElse(Map.of());
      return Map.copyOf(operand);
    }

    return config.entrySet().stream()
        .filter(entry -> !Set.of("type", "factoryClass").contains(entry.getKey()))
        .map(entry -> Map.entry(entry.getKey(), operandValue(entry)))
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Object operandValue(final Map.Entry<String, Object> entry) {
    if (spec.usesJsonCustomTables() && "tables".equals(entry.getKey())) {
      return jsonCustomTables(entry.getValue());
    }

    return entry.getValue();
  }

  private List<JsonCustomTable> jsonCustomTables(final Object value) {
    if (!(value instanceof List<?> tables)) {
      throw new IllegalArgumentException("Source config field 'tables' must be a list");
    }

    return tables.stream()
        .map(Map.class::cast)
        .map(this::jsonCustomTable)
        .collect(Collectors.toUnmodifiableList());
  }

  private JsonCustomTable jsonCustomTable(final Map<?, ?> tableConfig) {
    final String tableName =
        Optional.ofNullable(tableConfig.get("name"))
            .map(Object::toString)
            .filter(value -> !value.isBlank())
            .orElseThrow(
                () -> new IllegalArgumentException("Table config missing required 'name' field"));

    return new JsonCustomTable(
        tableName,
        new JsonStream(false, false),
        "org.apache.calcite.adapter.redis.RedisTableFactory",
        jsonCustomTableOperand(tableConfig));
  }

  private Map<String, Object> jsonCustomTableOperand(final Map<?, ?> tableConfig) {
    @SuppressWarnings("unchecked")
    final Map<String, Object> nestedOperand =
        Optional.ofNullable(tableConfig.get("operand"))
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .orElse(Map.of());
    final Map<String, Object> topLevelOperand =
        tableConfig.entrySet().stream()
            .filter(entry -> !Set.of("name", "operand").contains(entry.getKey().toString()))
            .collect(
                Collectors.toUnmodifiableMap(
                    entry -> entry.getKey().toString(), Map.Entry::getValue));

    return Stream.concat(nestedOperand.entrySet().stream(), topLevelOperand.entrySet().stream())
        .map(entry -> Map.entry(entry.getKey(), redisFieldValue(entry)))
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Object redisFieldValue(final Map.Entry<String, Object> entry) {
    if ("fields".equals(entry.getKey()) && entry.getValue() instanceof List<?> fields) {
      return fields.stream()
          .map(Map.class::cast)
          .map(this::linkedField)
          .collect(Collectors.toUnmodifiableList());
    }

    return entry.getValue();
  }

  private LinkedHashMap<String, Object> linkedField(final Map<?, ?> fieldConfig) {
    return fieldConfig.entrySet().stream()
        .collect(
            Collectors.toMap(
                entry -> entry.getKey().toString(),
                Map.Entry::getValue,
                (left, right) -> right,
                LinkedHashMap::new));
  }

  private record AdapterSpec(
      String supportedType,
      String displayName,
      Optional<String> factoryClassName,
      boolean usesNestedOperand,
      boolean usesJsonCustomTables,
      List<String> requiredKeys,
      List<String> requiredAnyKeys,
      List<String> directoryKeys,
      List<String> fileListKeys) {

    AdapterSpec {
      requiredKeys = List.copyOf(requiredKeys);
      requiredAnyKeys = List.copyOf(requiredAnyKeys);
      directoryKeys = List.copyOf(directoryKeys);
      fileListKeys = List.copyOf(fileListKeys);
    }
  }
}
