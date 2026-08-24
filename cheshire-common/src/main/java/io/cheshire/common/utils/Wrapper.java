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

import java.util.NoSuchElementException;
import java.util.Optional;

public interface Wrapper {

  <C> C unwrap(Class<C> clazz);

  default <C> C unwrapOrThrow(Class<C> clazz) {
    C result = unwrap(clazz);

    if (result == null) {
      throw new NoSuchElementException("Could not unwrap to " + clazz.getName());
    }
    return result;
  }

  default <C> Optional<C> someUnwrap(Class<C> clazz) {
    return Optional.ofNullable(unwrap(clazz));
  }
}
