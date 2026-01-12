/**
 * Common utility classes for various framework operations.
 * <p>
 * <strong>Package Overview:</strong>
 * <p>
 * This package contains utility classes that provide reusable functionality:
 * <ul>
 *   <li>Map manipulation and transformation utilities</li>
 *   <li>Object inspection and introspection helpers</li>
 *   <li>String processing utilities</li>
 *   <li>Collection utilities</li>
 * </ul>
 * <p>
 * <strong>Utility Class Pattern:</strong>
 * <p>
 * All utility classes follow these conventions:
 * <ul>
 *   <li>Final class (cannot be extended)</li>
 *   <li>Private constructor (cannot be instantiated)</li>
 *   <li>Static methods only</li>
 *   <li>Pure functions (no side effects)</li>
 * </ul>
 * <p>
 * <strong>Example Utility:</strong>
 * <pre>{@code
 * public final class MapUtils {
 *     private MapUtils() {
 *         throw new UnsupportedOperationException("Utility class");
 *     }
 *
 *     public static <K, V> Map<K, V> copyOf(Map<K, V> map) {
 *         return map != null ? Map.copyOf(map) : Map.of();
 *     }
 * }
 * }</pre>
 * <p>
 * <strong>Immutability Focus:</strong>
 * <p>
 * Utilities prefer immutable operations:
 * <pre>{@code
 * // Returns new map, doesn't mutate input
 * Map<String, Object> filtered = MapUtils.filter(input, predicate);
 *
 * // Returns immutable copy
 * List<String> copy = CollectionUtils.immutableCopy(list);
 * }</pre>
 *
 * @see io.cheshire.common.config
 * @see io.cheshire.common.exception
 * @since 1.0.0
 */
package io.cheshire.common.utils;
