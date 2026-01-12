package io.cheshire.core.pipeline;

import io.cheshire.spi.pipeline.Context;
import io.cheshire.spi.pipeline.exception.PipelineException;
import io.cheshire.spi.pipeline.step.Executor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class NoopExecutor implements Executor<MaterializedInput, MaterializedOutput> {

    private final String template;
    private final String name;

    public NoopExecutor(Map<String, Object> config) {
        this.template = (String) config.get("template");
        this.name = (String) config.get("name");
    }

    @Override
    public MaterializedOutput apply(MaterializedInput executorInput, Context ctx)
            throws PipelineException {

        log.debug("Executing NOOP executor");

        // ---- context trace
        ctx.putIfAbsent("executor-at", Instant.now().toString());

        // ---- hardcoded output data (template-driven)
        LinkedHashMap<String, Object> outputData = new LinkedHashMap<>();
        outputData.put("message", "{ }");
        outputData.put("template", template);

        // ---- metadata (execution info only)
        LinkedHashMap<String, Object> outputMetadata = new LinkedHashMap<>();
        outputMetadata.put("executor-executed-at", Instant.now().toString());
        outputMetadata.put("executor-name", name);
        outputMetadata.put("executor-template", template);

        return MaterializedOutput.of(outputData, outputMetadata);
    }
}
