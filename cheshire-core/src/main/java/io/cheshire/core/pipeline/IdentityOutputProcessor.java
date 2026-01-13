/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.pipeline;

import io.cheshire.spi.pipeline.Context;
import io.cheshire.spi.pipeline.step.PostProcessor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class IdentityOutputProcessor implements PostProcessor<MaterializedOutput> {

    private final String template;
    private final String name;

    public IdentityOutputProcessor(Map<String, Object> config) {
        this.template = (String) config.get("template");
        this.name = (String) config.get("name");
    }

    @Override
    public MaterializedOutput apply(MaterializedOutput postInput, Context ctx) {

        log.debug("Identity post-processing output");

        // ---- metadata: copy + enrich
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(postInput.metadata());
        metadata.put("post-processor-executed-at", Instant.now().toString());
        metadata.put("post-processor-name", name);
        metadata.put("post-processor-template", template);

        var data = new LinkedHashMap<>(postInput.data());
        // ---- context: side-channel trace
        ctx.putIfAbsent("post-processor-at", Instant.now().toString());

        // ---- data: untouched
        return MaterializedOutput.of(data, metadata);
    }
}
