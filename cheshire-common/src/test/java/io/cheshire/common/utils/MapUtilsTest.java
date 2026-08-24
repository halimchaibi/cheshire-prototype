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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MapUtilsTest {

  @Test
  void unwrapConvertsNumericStringToInteger() {
    assertEquals(9000, MapUtils.wrap("9000").unwrap(Integer.class));
  }

  @Test
  void unwrapReturnsNullForInvalidIntegerString() {
    assertNull(MapUtils.wrap("not-a-number").unwrap(Integer.class));
  }
}
