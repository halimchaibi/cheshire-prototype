/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.config;

public record CacheConfig(
    boolean enableQueryPlanCache,
    boolean enableMetadataCache,
    int maxCacheSize,
    long cacheTtlSeconds) {
  public static CacheConfig defaults() {
    return new CacheConfig(true, true, 1000, 3600);
  }
}
