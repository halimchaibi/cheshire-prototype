/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.cheshire.common.exception.CheshireException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LifecycleManagerTest {

  @Test
  void initializesComponentsInPhaseOrder() {
    final var events = new CopyOnWriteArrayList<String>();
    final var source = new RecordingComponent("source", events, 125);
    final var query = new RecordingComponent("query", events, 0);
    final var capability = new RecordingComponent("capability", events, 0);

    final var manager =
        new LifecycleManager(
            List.of(
                new LifecycleManager.ComponentEntry(capability, InitializationPhase.CAPABILITIES),
                new LifecycleManager.ComponentEntry(source, InitializationPhase.SOURCE_PROVIDERS),
                new LifecycleManager.ComponentEntry(query, InitializationPhase.QUERY_ENGINES)));

    manager.initialize();

    assertThat(events)
        .containsSubsequence(
            "source:init:start",
            "source:init:end",
            "query:init:start",
            "query:init:end",
            "capability:init:start",
            "capability:init:end");
  }

  @Test
  void canRestartAfterShutdown() {
    final var component = new RecordingComponent("component", new CopyOnWriteArrayList<>(), 0);
    final var manager =
        new LifecycleManager(
            List.of(
                new LifecycleManager.ComponentEntry(
                    component, InitializationPhase.SOURCE_PROVIDERS)));

    manager.initialize();
    manager.shutdown();
    manager.initialize();

    assertThat(manager.isRunning()).isTrue();
    assertThat(component.initializations()).isEqualTo(2);
    assertThat(component.shutdowns()).isEqualTo(1);
  }

  @Test
  void shutsDownStartedComponentsWhenInitializationFails() {
    final var events = new CopyOnWriteArrayList<String>();
    final var source = new RecordingComponent("source", events, 0);
    final var query = new FailingComponent("query", events);
    final var capability = new RecordingComponent("capability", events, 0);
    final var manager =
        new LifecycleManager(
            List.of(
                new LifecycleManager.ComponentEntry(source, InitializationPhase.SOURCE_PROVIDERS),
                new LifecycleManager.ComponentEntry(query, InitializationPhase.QUERY_ENGINES),
                new LifecycleManager.ComponentEntry(capability, InitializationPhase.CAPABILITIES)));

    assertThatThrownBy(manager::initialize).isInstanceOf(CheshireException.class);

    assertThat(manager.isRunning()).isFalse();
    assertThat(source.shutdowns()).isEqualTo(1);
    assertThat(capability.initializations()).isZero();
    assertThat(events)
        .containsSubsequence("source:init:end", "query:init:start", "source:shutdown");
  }

  private static final class RecordingComponent implements Initializable {
    private final String name;
    private final List<String> events;
    private final long initDelayMs;
    private final AtomicInteger initializations = new AtomicInteger();
    private final AtomicInteger shutdowns = new AtomicInteger();

    private RecordingComponent(String name, List<String> events, long initDelayMs) {
      this.name = name;
      this.events = events;
      this.initDelayMs = initDelayMs;
    }

    @Override
    public void initialize() {
      events.add(name + ":init:start");
      initializations.incrementAndGet();
      sleep(initDelayMs);
      events.add(name + ":init:end");
    }

    @Override
    public void shutdown() {
      events.add(name + ":shutdown");
      shutdowns.incrementAndGet();
    }

    private int initializations() {
      return initializations.get();
    }

    private int shutdowns() {
      return shutdowns.get();
    }
  }

  private static final class FailingComponent implements Initializable {
    private final String name;
    private final List<String> events;

    private FailingComponent(String name, List<String> events) {
      this.name = name;
      this.events = events;
    }

    @Override
    public void initialize() {
      events.add(name + ":init:start");
      throw new IllegalStateException("boom");
    }

    @Override
    public void shutdown() {
      events.add(name + ":shutdown");
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted", e);
    }
  }
}
