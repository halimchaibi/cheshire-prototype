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

import io.cheshire.core.CheshireBootstrap;
import io.cheshire.core.CheshireSession;
import io.cheshire.core.SessionContext;
import io.cheshire.core.SessionTask;
import io.cheshire.core.TaskResult;
import io.cheshire.core.constant.Key;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * End-to-end coverage: classpath config → session → CalciteSqlExecutor → OS adapter → rows.
 *
 * <p>Uses a single session for the class to avoid shutting down shared registries between tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OsObservabilityEndToEndTest {

  private CheshireSession session;

  @BeforeAll
  void startSession() {
    System.setProperty("cheshire.config", "os-rest.yaml");
    session = CheshireBootstrap.fromClasspath("config").skipSessionAutoStart().build();
    session.start();
  }

  @AfterAll
  void stopSession() {
    if (session != null) {
      session.stop();
    }
    System.clearProperty("cheshire.config");
  }

  @Test
  @Order(1)
  @DisplayName("list_system_info returns host metadata rows")
  void listSystemInfoReturnsHostMetadata() throws Exception {
    final Map<String, Object> output = successOutput(execute("list_system_info"));
    assertThat(rows(output)).isNotEmpty();
    assertThat(output).containsKey("columns");
  }

  @Test
  @Order(2)
  @DisplayName("list_java_info returns JVM metadata rows")
  void listJavaInfoReturnsJvmMetadata() throws Exception {
    final Map<String, Object> output = successOutput(execute("list_java_info"));
    assertThat(rows(output)).isNotEmpty();
    assertThat(output).containsKey("columns");
  }

  @Test
  @Order(3)
  @DisplayName("list_processes returns process rows")
  void listProcessesReturnsProcessRows() throws Exception {
    final Map<String, Object> output = successOutput(execute("list_processes"));
    assertThat(rows(output)).isNotEmpty();
    assertThat(output).containsKey("columns");
  }

  @Test
  @Order(4)
  @DisplayName("unknown action fails")
  void unknownActionFails() throws Exception {
    final TaskResult result = execute("does_not_exist");
    assertThat(result).isInstanceOf(TaskResult.Failure.class);
  }

  private TaskResult execute(final String action) throws Exception {
    return session.execute(
        new SessionTask(
            Map.of(Key.PAYLOAD_DATA.key(), Map.of()),
            Map.of(Key.CAPABILITY.key(), "os", Key.ACTION.key(), action)),
        SessionContext.empty());
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> successOutput(final TaskResult result) {
    assertThat(result).isInstanceOf(TaskResult.Success.class);
    final Object output = ((TaskResult.Success) result).output();
    assertThat(output).isInstanceOf(Map.class);
    return (Map<String, Object>) output;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> rows(final Map<String, Object> output) {
    assertThat(output.get("rows")).isInstanceOf(List.class);
    return (List<Map<String, Object>>) output.get("rows");
  }
}
