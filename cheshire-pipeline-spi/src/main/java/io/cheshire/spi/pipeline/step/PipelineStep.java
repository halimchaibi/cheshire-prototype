package io.cheshire.spi.pipeline.step;

import java.util.Map;

public record PipelineStep<I, O>(
        StepType type,
        String name,
        Step<I, O> step,
        Map<String, Object> config
) {
}
