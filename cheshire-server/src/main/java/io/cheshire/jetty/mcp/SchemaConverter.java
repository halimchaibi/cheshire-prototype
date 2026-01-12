package io.cheshire.jetty.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cheshire.core.config.ActionsConfig;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class SchemaConverter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert ActionsConfig.Tool input schema to MCP JsonSchema
     */
    public static McpSchema.JsonSchema convertToJsonSchema(Map<String, Object> inputSchema) {
        if (inputSchema == null || inputSchema.isEmpty()) {
            return createDefaultJsonSchema();
        }

        try {
            Map<String, Object> normalizedSchema = normalizeSchema(inputSchema);
            return new McpSchema.JsonSchema(
                    getStringValue(normalizedSchema, "type", "object"),
                    getMapValue(normalizedSchema, "properties", Map.of()),
                    getListValue(normalizedSchema, "required", List.of()),
                    getBooleanValue(normalizedSchema, "additionalProperties"),
                    getMapValue(normalizedSchema, "$defs", Map.of()),
                    getMapValue(normalizedSchema, "definitions", Map.of())
            );
        } catch (Exception e) {
            log.warn("Failed to convert input schema, using default: {}", e.getMessage());
            return createDefaultJsonSchema();
        }
    }

    private static Map<String, Object> normalizeSchema(Map<String, Object> schema) {
        Map<String, Object> normalized = new LinkedHashMap<>(schema);
        if (!schema.containsKey("type") && schema.containsKey("properties")) {
            normalized.putIfAbsent("type", "object");
        }
        if (schema.containsKey("properties") && !(schema.get("properties") instanceof Map)) {
            try {
                Object properties = schema.get("properties");
                if (properties instanceof String) {
                    // Try to parse JSON string
                    Map<String, Object> parsed = objectMapper.readValue(
                            (String) properties,
                            new TypeReference<Map<String, Object>>() {
                            }
                    );
                    normalized.put("properties", parsed);
                } else {
                    normalized.put("properties", Map.of());
                }
            } catch (Exception e) {
                normalized.put("properties", Map.of());
            }
        }
        if (schema.containsKey("required") && !(schema.get("required") instanceof List)) {
            try {
                Object required = schema.get("required");
                if (required instanceof String) {
                    // Parse comma-separated or JSON array string
                    String str = (String) required;
                    if (str.startsWith("[") && str.endsWith("]")) {
                        List<String> parsed = objectMapper.readValue(str, List.class);
                        normalized.put("required", parsed);
                    } else {
                        List<String> list = Arrays.stream(str.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList();
                        normalized.put("required", list);
                    }
                } else if (required instanceof Object[]) {
                    normalized.put("required", Arrays.asList((Object[]) required));
                } else {
                    normalized.put("required", List.of());
                }
            } catch (Exception e) {
                normalized.put("required", List.of());
            }
        }
        return normalized;
    }

    private static String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof String str) return str;
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMapValue(Map<String, Object> map, String key,
                                                   Map<String, Object> defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Map) return (Map<String, Object>) value;

        try {
            if (value instanceof String str) {
                return objectMapper.readValue(str, new TypeReference<Map<String, Object>>() {
                });
            }
        } catch (Exception e) {
            log.debug("Failed to parse map for key {}: {}", key, e.getMessage());
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getListValue(Map<String, Object> map, String key,
                                             List<String> defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                    .map(Object::toString)
                    .toList();
        }

        try {
            if (value instanceof String str) {
                List<?> parsed = objectMapper.readValue(str, List.class);
                return parsed.stream()
                        .map(Object::toString)
                        .toList();
            }
        } catch (Exception e) {
            log.debug("Failed to parse list for key {}: {}", key, e.getMessage());
        }
        return defaultValue;
    }

    private static Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Boolean bool) return bool;
        if (value instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return null;
    }

    private static McpSchema.JsonSchema createDefaultJsonSchema() {
        return new McpSchema.JsonSchema(
                "object",
                Map.of(),
                List.of(),
                null,
                Map.of(),
                Map.of()
        );
    }

    /**
     * Helper method to create JsonSchema from ActionsConfig.Tool
     */
    public static McpSchema.JsonSchema fromTool(ActionsConfig.Tool tool) {
        return convertToJsonSchema(tool.inputSchema);
    }

    /**
     * Create a JSON schema string representation
     */
    public static String toJsonString(McpSchema.JsonSchema schema) {
        try {
            Map<String, Object> jsonMap = new LinkedHashMap<>();

            if (schema.type() != null) {
                jsonMap.put("type", schema.type());
            }

            if (schema.properties() != null && !schema.properties().isEmpty()) {
                jsonMap.put("properties", schema.properties());
            }

            if (schema.required() != null && !schema.required().isEmpty()) {
                jsonMap.put("required", schema.required());
            }

            if (schema.additionalProperties() != null) {
                jsonMap.put("additionalProperties", schema.additionalProperties());
            }

            if (schema.defs() != null && !schema.defs().isEmpty()) {
                jsonMap.put("$defs", schema.defs());
            } else if (schema.definitions() != null && !schema.definitions().isEmpty()) {
                jsonMap.put("definitions", schema.definitions());
            }

            return objectMapper.writeValueAsString(jsonMap);
        } catch (Exception e) {
            log.error("Failed to serialize JsonSchema to string", e);
            return "{\"type\":\"object\",\"properties\":{},\"required\":[]}";
        }
    }
}
