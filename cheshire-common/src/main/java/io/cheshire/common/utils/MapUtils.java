/*-
 * #%L
 * Cheshire :: Common Utils
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.common.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class MapUtils {

  /**
   * Safely puts a key-value pair into a nested Map within a parent Map. * @param parent The outer
   * map (e.g., your metadata map)
   *
   * @param category The key for the nested map (e.g., "DEBUG")
   * @param key The key to insert inside the nested map
   * @param value The value to insert
   */
  @SuppressWarnings("unchecked")
  public static void putNested(
      Map<String, Object> parent, String category, String key, Object value) {
    ((Map<String, Object>) parent.computeIfAbsent(category, k -> new HashMap<String, Object>()))
        .put(key, value);
  }

  public static <T> T valueFromMapAs(
      Map<String, Object> config, String key, Class<T> clazz, T defaultValue) {

    Supplier<T> supplier = () -> defaultValue;
    return valueFromMapAs(config, key, clazz, supplier);
  }

  public static <T> T valueFromMapAs(
      Map<String, Object> config, String key, Class<T> clazz, Supplier<T> defaultValue) {

    Object value = config.get(key);

    if (value == null) {
      return defaultValue.get();
    }

    if (!clazz.isInstance(value)) {
      throw new IllegalArgumentException(
          "Config key '"
              + key
              + "' expected "
              + clazz.getName()
              + " but got "
              + value.getClass().getName());
    }

    return clazz.cast(value);
  }

  /** Convenience method: fetch from map and cast safely to class. */
  public static <T> Optional<T> someValueFromMapAs(
      Map<String, Object> map, String key, Class<T> clazz) {
    if (map == null || key == null) {
      return Optional.empty();
    }

    Object value = map.get(key);
    return ObjectUtils.someObjectAs(value, clazz);
  }

  /**
   * Wraps a map or a value into a Fluent Wrapper Usage: int port = MapUtils.wrap(source)
   * .unwrapOrThrow(Map.class) .get("config") .get("port") .unwrapOrThrow(Integer.class);
   */
  public static Wrapper wrap(Object value) {
    return new FluentMapWrapper(value);
  }

  private record FluentMapWrapper(Object content) implements Wrapper {

    @Override
    @SuppressWarnings("unchecked")
    public <C> C unwrap(Class<C> target) {
      return switch (content) {
        case null -> null;
        case Object o when target.isInstance(o) -> target.cast(o);
        case String s when target == Integer.class -> (C) Integer.valueOf(s);
        case String s when target == Boolean.class -> (C) Boolean.valueOf(s);
        case List<?> list when !target.isAssignableFrom(List.class) && !list.isEmpty() ->
            wrap(list.getFirst()).unwrap(target);
        default -> null;
      };
    }

    /** Deep navigation helper embedded in the wrapper */
    public Wrapper get(String key) {
      if (content instanceof Map<?, ?> map) {
        return wrap(map.get(key));
      }
      return wrap(null);
    }
  }
}
