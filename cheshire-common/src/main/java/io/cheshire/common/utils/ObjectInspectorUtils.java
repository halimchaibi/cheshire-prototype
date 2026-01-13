/*-
 * #%L
 * Cheshire :: Common Utils
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

@Slf4j
public final class ObjectInspectorUtils {

    private ObjectInspectorUtils() {
    }

    public static void inspect(Object obj) {
        inspect(obj, 0);
    }

    private static void inspect(Object obj, int indent) {
        if (obj == null) {
            logIndented("null", indent);
            return;
        }

        Class<?> clazz = obj.getClass();

        // Simple / scalar values
        if (isSimple(clazz)) {
            logIndented(obj.toString(), indent);
            return;
        }

        // Array
        if (clazz.isArray()) {
            int length = Array.getLength(obj);
            logIndented(clazz.getComponentType().getSimpleName() + "[" + length + "] {", indent);
            for (int i = 0; i < length; i++) {
                inspect(Array.get(obj, i), indent + 2);
            }
            logIndented("}", indent);
            return;
        }

        // Collection
        if (obj instanceof Collection<?> collection) {
            logIndented(clazz.getSimpleName() + " (" + collection.size() + ") {", indent);
            for (Object element : collection) {
                inspect(element, indent + 2);
            }
            logIndented("}", indent);
            return;
        }

        // Map
        if (obj instanceof Map<?, ?> map) {
            logIndented(clazz.getSimpleName() + " (" + map.size() + ") {", indent);
            for (var entry : map.entrySet()) {
                logIndented("Key:", indent + 2);
                inspect(entry.getKey(), indent + 4);
                logIndented("Value:", indent + 2);
                inspect(entry.getValue(), indent + 4);
            }
            logIndented("}", indent);
            return;
        }

        // Object
        logIndented(clazz.getSimpleName() + " {", indent);

        for (Field field : clazz.getDeclaredFields()) {
            if (shouldSkip(field)) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(obj);

                logIndented(field.getName() + ":", indent + 2);

                if (value == null) {
                    logIndented("null", indent + 4);
                    continue;
                }

                Class<?> valueClass = value.getClass();

                // Containers are always expanded
                if (valueClass.isArray() || value instanceof Collection || value instanceof Map) {
                    inspect(value, indent + 4);
                    continue;
                }

                // Foreign objects are summarized
                if (isForeign(valueClass)) {
                    logIndented(summarize(value), indent + 4);
                    continue;
                }

                // Domain object → recurse
                inspect(value, indent + 4);

            } catch (Exception e) {
                logIndented(field.getName() + ": <unavailable>", indent + 2);
            }
        }

        logIndented("}", indent);
    }

    /* ================= helpers ================= */

    private static boolean isSimple(Class<?> clazz) {
        return clazz.isPrimitive() || clazz == String.class || Number.class.isAssignableFrom(clazz)
                || clazz == Boolean.class || clazz == Character.class;
    }

    /**
     * Foreign = not part of your domain modules
     */
    private static boolean isForeign(Class<?> clazz) {
        Module m = clazz.getModule();
        return m.isNamed() && !clazz.getName().startsWith("io.cheshire");
    }

    private static boolean shouldSkip(Field field) {
        int mods = field.getModifiers();
        return Modifier.isStatic(mods) || Modifier.isTransient(mods) || field.isSynthetic();
    }

    private static String summarize(Object obj) {
        return obj.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(obj));
    }

    private static void logIndented(String message, int indent) {
        // Prefix the message with spaces for indentation
        String indented = " ".repeat(indent) + message;
        log.info(indented);
    }
}
