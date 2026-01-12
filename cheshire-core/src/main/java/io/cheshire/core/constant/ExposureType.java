package io.cheshire.core.constant;

public enum ExposureType {

    REST_HTTP("rest-http"),
    MCP_STREAMABLE_HTTP("mcp-streamable-http");

    public final String type;

    ExposureType(String key) {
        this.type = key;
    }

    public static ExposureType from(String raw) {
        String normalized = raw.trim();
        for (ExposureType t : values()) {
            if (t.type.equalsIgnoreCase(normalized)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown matadata Key: " + raw);
    }

    public String type() {
        return type;
    }
}
