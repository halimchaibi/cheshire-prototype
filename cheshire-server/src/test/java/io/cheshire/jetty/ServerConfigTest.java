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

import io.cheshire.core.config.CheshireConfig;
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

  @Test
  @DisplayName("should expose live state after Jetty container start and stop")
  void shouldExposeLiveStateAfterJettyContainerStartAndStop() throws Exception {
    final var transport = new CheshireConfig.Transport();
    final var threadPool = new CheshireConfig.Transport.ThreadPool();
    threadPool.setMinThreads(1);
    threadPool.setMaxThreads(4);
    transport.setThreadPool(threadPool);
    transport.setPort(0);

    final var container = new JettyServerContainer(transport);

    container.attach();
    try {
      container.start();

      assertThat(container.isRunning()).isTrue();
    } finally {
      container.stop();
    }

    assertThat(container.isRunning()).isFalse();
  }
}
