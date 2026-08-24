/*-
 * #%L
 * Cheshire :: Pipeline :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;

class ContextIsolationTest {

  @Test
  void defaultAttributeMutationUsesTheImplementingContextNotGlobalInterfaceState() {
    final var first = new LocalContext();
    final var second = new LocalContext();

    assertThat(first.putIfAbsent("trace-id", "trace-a")).isNull();

    assertThat(second.putIfAbsent("trace-id", "trace-b")).isNull();
    assertThat(first.attributes().get("trace-id")).isEqualTo("trace-a");
    assertThat(second.attributes().get("trace-id")).isEqualTo("trace-b");
  }

  private static final class LocalContext implements Context {
    private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();

    public ConcurrentMap<String, Object> attributes() {
      return attributes;
    }
  }
}
