/*-
 * #%L
 * Cheshire :: Pipeline :: SPI
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.spi.pipeline.step;

import java.util.Map;

public record PipelineStep<I, O>(StepType type, String name, Step<I, O> step, Map<String, Object> config) {
}
