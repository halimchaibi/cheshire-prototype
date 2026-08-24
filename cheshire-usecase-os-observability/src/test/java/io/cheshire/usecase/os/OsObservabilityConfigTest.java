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

import static org.assertj.core.api.Assertions.assertThat;

import io.cheshire.common.config.ConfigSource;
import io.cheshire.core.CheshireBootstrap;
import io.cheshire.core.CheshireSession;
import io.cheshire.core.SessionContext;
import io.cheshire.core.SessionTask;
import io.cheshire.core.TaskResult;
import io.cheshire.core.config.CheshireConfig;
import io.cheshire.core.constant.Key;
import io.cheshire.core.manager.ConfigurationManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OsObservabilityConfigTest {

  @AfterEach
  void clearConfigProperty() {
    System.clearProperty("cheshire.config");
  }

  @Test
  void loadsRestUseCaseConfiguration() {
    final CheshireConfig config = load("os-rest.yaml");

    assertThat(config.getCapabilities()).containsKey("os");
    assertThat(config.getCapabilities().get("os").getExposure()).isEqualTo("rest-api");
    assertThat(config.getSources().get("os").getConfig()).containsEntry("type", "os");
    assertThat(config.getQueryEngines().get("calcite").getFactory())
        .isEqualTo("io.cheshire.query.engine.calcite.CalciteQueryEngineFactory");
    assertThat(config.getCapabilities().get("os").getPipelines())
        .containsKeys("list_system_info", "list_java_info", "list_processes");
  }

  @Test
  void loadsMcpStdioUseCaseConfiguration() {
    final CheshireConfig config = load("os-mcp-stdio.yaml");

    assertThat(config.getCapabilities()).containsKey("os");
    assertThat(config.getCapabilities().get("os").getExposure()).isEqualTo("mcp-stdio");
    assertThat(config.getExposures().get("mcp-stdio").getBinding()).isEqualTo("mcp-stdio");
    assertThat(config.getCapabilities().get("os").getActions().tools)
        .extracting(tool -> tool.name)
        .contains("list_system_info", "list_java_info", "list_processes");
  }

  @Test
  void executesOsActionsThroughConfiguredSession() throws Exception {
    System.setProperty("cheshire.config", "os-rest.yaml");
    final CheshireSession session =
        CheshireBootstrap.fromClasspath("config").skipSessionAutoStart().build();

    try {
      session.start();

      final TaskResult systemInfo = execute(session, "list_system_info");
      final TaskResult processes = execute(session, "list_processes");

      assertSuccessfulRows(systemInfo);
      assertSuccessfulRows(processes);
    } finally {
      session.stop();
    }
  }

  private TaskResult execute(final CheshireSession session, final String action) throws Exception {
    return session.execute(
        new SessionTask(
            Map.of(Key.PAYLOAD_DATA.key(), Map.of()),
            Map.of(Key.CAPABILITY.key(), "os", Key.ACTION.key(), action)),
        SessionContext.empty());
  }

  private void assertSuccessfulRows(final TaskResult result) {
    assertThat(result)
        .isInstanceOfSatisfying(
            TaskResult.Success.class,
            success -> {
              assertThat(success.output()).isInstanceOf(Map.class);
              @SuppressWarnings("unchecked")
              final Map<String, Object> output = (Map<String, Object>) success.output();
              assertThat(output).containsKeys("columns", "rows");
              assertThat(output.get("rows")).isInstanceOf(List.class);
              assertThat((List<?>) output.get("rows")).isNotEmpty();
            });
  }

  private CheshireConfig load(final String configFile) {
    System.setProperty("cheshire.config", configFile);
    return new ConfigurationManager(ConfigSource.classpath("config")).getCheshireConfig();
  }
}
