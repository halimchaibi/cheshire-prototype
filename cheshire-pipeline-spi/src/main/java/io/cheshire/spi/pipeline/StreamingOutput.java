package io.cheshire.spi.pipeline;

import java.util.Map;

public record StreamingOutput(
        Map<String, Object> data,
        Map<String, Object> metadata) {
}
