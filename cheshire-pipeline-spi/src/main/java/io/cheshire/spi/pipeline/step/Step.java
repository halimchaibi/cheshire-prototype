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

import io.cheshire.spi.pipeline.Context;
import io.cheshire.spi.pipeline.exception.PipelineException;

public sealed interface Step<I, O> permits PostProcessor, PreProcessor, Executor {

    O apply(I input, Context ctx) throws PipelineException;
}
