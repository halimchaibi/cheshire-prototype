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
import io.cheshire.spi.pipeline.step.PreProcessor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class IdentityInputProcessor implements PreProcessor<MaterializedInput> {

    private final String template;
    private final String name;

    public IdentityInputProcessor(Map<String, Object> config) {
        this.template = (String) config.get("template");
        this.name = (String) config.get("name");
    }

    @Override
    public MaterializedInput apply(MaterializedInput preInput, Context ctx) {

        log.debug("Identity preprocessing input");

        // ---- metadata: copy + enrich
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(preInput.metadata());

        metadata.put("pre-processor-executed-at", Instant.now().toString());
        metadata.put("pre-processor-name", name);
        metadata.put("pre-processor-template", template);

        // optional: context trace (side-channel, not payload)
        ctx.putIfAbsent("pre-processor-at", Instant.now().toString());

        var data = new LinkedHashMap<>(preInput.data());
        // ---- data: untouched
        return MaterializedInput.of(data, metadata);
    }
}
