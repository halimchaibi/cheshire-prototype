/*-
 * #%L
 * Cheshire :: Use Case :: OS Observability
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.usecase.os;

import io.cheshire.core.CheshireBootstrap;
import io.cheshire.core.CheshireSession;
import io.cheshire.runtime.CheshireRuntime;
import java.util.Arrays;
import java.util.Optional;

public final class OsObservabilityApp {

  private static final String DEFAULT_CONFIG = "os-rest.yaml";

  private OsObservabilityApp() {}

  public static void main(final String[] args) {
    System.setProperty("cheshire.config", configName(args));

    final CheshireSession session = CheshireBootstrap.fromClasspath("config").build();
    final CheshireRuntime runtime = CheshireRuntime.expose(session).start();
    runtime.awaitTermination();
  }

  private static String configName(final String[] args) {
    return Arrays.stream(args)
        .filter(argument -> argument.startsWith("--config="))
        .map(argument -> argument.substring("--config=".length()))
        .filter(value -> !value.isBlank())
        .findFirst()
        .or(() -> configNameAfterFlag(args))
        .orElse(DEFAULT_CONFIG);
  }

  private static Optional<String> configNameAfterFlag(final String[] args) {
    return java.util.stream.IntStream.range(0, Math.max(args.length - 1, 0))
        .filter(index -> "--config".equals(args[index]))
        .mapToObj(index -> args[index + 1])
        .filter(value -> !value.isBlank())
        .findFirst();
  }
}
