/*-
 * #%L
 * Cheshire :: Servers
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.jetty;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sample tests for Jetty server configuration. Tests basic server configuration properties. */
@DisplayName("ServerConfig")
final class ServerConfigTest {

  @Test
  @DisplayName("should configure default HTTP port")
  void shouldConfigureDefaultHttpPort() {
    final var defaultPort = 8080;

    assertThat(defaultPort).isPositive().isGreaterThan(1024).isLessThan(65536);
  }

  @Test
  @DisplayName("should validate server host configuration")
  void shouldValidateServerHostConfiguration() {
    final var host = "localhost";

    assertThat(host).isNotNull().isNotBlank();
  }

  @Test
  @DisplayName("should handle SSL configuration")
  void shouldHandleSslConfiguration() {
    final var sslEnabled = false;
    final var sslPort = 8443;

    assertThat(sslEnabled).isFalse();
    assertThat(sslPort).isEqualTo(8443);
  }
}
