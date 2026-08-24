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

import java.util.Optional;

public class ObjectUtils {

  public static <T> T requireObjectAs(Object o, Class<T> clazz) {
    if (o == null) {
      throw new IllegalArgumentException("Object is null");
    }
    if (!clazz.isInstance(o)) {
      throw new IllegalArgumentException(
          "Expected type: " + clazz.getName() + ", got: " + o.getClass().getName());
    }
    return clazz.cast(o);
  }

  public static <T> Optional<T> someObjectAs(Object obj, Class<T> clazz) {
    if (clazz == null) {
      throw new IllegalArgumentException("Class parameter cannot be null");
    }

    if (obj == null) {
      return Optional.empty();
    }

    if (!clazz.isInstance(obj)) {
      return Optional.empty();
    }

    return Optional.of(clazz.cast(obj));
  }
}
