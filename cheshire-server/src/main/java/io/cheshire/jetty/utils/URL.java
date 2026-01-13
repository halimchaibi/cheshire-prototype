/*-
 * #%L
 * Cheshire :: Servers
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.jetty.utils;

import jakarta.servlet.http.HttpServletRequest;

public class URL {
    public static String getSubpathFromPath(HttpServletRequest request, int index) {
        return switch (request.getPathInfo()) {
        case null -> throw new IllegalArgumentException("Path info is null");
        case String path when path.isBlank() -> throw new IllegalArgumentException("Path is blank");
        case String path -> {
            String[] segments = path.split("/");
            // Use index validation within the arrow rule
            if (index < 0 || index >= segments.length) {
                throw new IllegalArgumentException("No path segment at index " + index);
            }
            yield segments[index];
        }
        };
    }
}
