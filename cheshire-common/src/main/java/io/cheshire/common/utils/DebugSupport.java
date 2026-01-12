package io.cheshire.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class DebugSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private DebugSupport() {
    } // Prevent instantiation

    public static String stringify(Object obj) {
        if (obj == null) return "null";

        if (obj.getClass().isAnnotationPresent(Printable.class)) {
            try {
                return MAPPER.writeValueAsString(obj);
            } catch (Exception e) {
                return "[Serialization Error: " + e.getMessage() + "]";
            }
        }
        return String.valueOf(obj);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Printable {
    }
}
