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

import java.util.HashMap;
import java.util.Map;

public class MapUtils {

    /**
     * Safely puts a key-value pair into a nested Map within a parent Map. * @param parent The outer map (e.g., your
     * metadata map)
     *
     * @param category
     *            The key for the nested map (e.g., "DEBUG")
     * @param key
     *            The key to insert inside the nested map
     * @param value
     *            The value to insert
     */
    @SuppressWarnings("unchecked")
    public static void putNested(Map<String, Object> parent, String category, String key, Object value) {
        ((Map<String, Object>) parent.computeIfAbsent(category, k -> new HashMap<String, Object>())).put(key, value);
    }
}
