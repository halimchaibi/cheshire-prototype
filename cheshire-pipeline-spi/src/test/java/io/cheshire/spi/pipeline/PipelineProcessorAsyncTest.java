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

import io.cheshire.spi.pipeline.exception.PipelineException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PipelineProcessorAsyncTest {

  @Test
  void executeAsyncReturnsBeforeThePipelineFinishes() throws Exception {
    final var started = new CountDownLatch(1);
    final var release = new CountDownLatch(1);
    final var processor =
        new PipelineProcessor<>(
            "slow-pipeline",
            TestInput.class,
            TestOutput.class,
            java.util.List.of(),
            (input, context) -> {
              started.countDown();
              await(release);
              return new TestOutput(Map.of("done", true), Map.of());
            },
            java.util.List.of());

    final var future =
        processor.executeAsync(new TestInput(Map.of(), Map.of()), ExecutionContext.empty());

    assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();
    assertThat(future).isNotDone();

    release.countDown();

    assertThat(future.get(1, TimeUnit.SECONDS).data()).containsEntry("done", true);
  }

  private static void await(CountDownLatch latch) throws PipelineException {
    try {
      latch.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new PipelineException("Interrupted while waiting for test latch", e);
    }
  }

  private record TestInput(Map<String, Object> data, Map<String, Object> metadata)
      implements CanonicalInput<TestInput> {
    @Override
    public TestInput copy(Map<String, Object> data, Map<String, Object> metadata) {
      return new TestInput(Map.copyOf(data), Map.copyOf(metadata));
    }
  }

  private record TestOutput(Map<String, Object> data, Map<String, Object> metadata)
      implements CanonicalOutput<TestOutput> {
    @Override
    public TestOutput copy(Map<String, Object> data, Map<String, Object> metadata) {
      return new TestOutput(Map.copyOf(data), Map.copyOf(metadata));
    }
  }
}
