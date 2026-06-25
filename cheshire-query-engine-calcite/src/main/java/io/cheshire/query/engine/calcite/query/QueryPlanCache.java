/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.query;

import io.cheshire.query.engine.calcite.config.CacheConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.calcite.rel.RelNode;

public class QueryPlanCache {

  private final CacheConfig config;
  private final Map<String, CachedPlan> cache;

  public QueryPlanCache(final CacheConfig config) {
    this.config = Objects.requireNonNull(config, "Cache config is required");
    this.cache =
        new LinkedHashMap<>(config.maxCacheSize(), 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(final Map.Entry<String, CachedPlan> eldest) {
            return super.size() > config.maxCacheSize();
          }
        };
  }

  public synchronized Optional<RelNode> get(final String queryKey) {
    return Optional.ofNullable(cache.get(Objects.requireNonNull(queryKey, "Query key is required")))
        .flatMap(cached -> validPlan(queryKey, cached));
  }

  public synchronized void put(final String queryKey, final RelNode plan) {
    if (config.enableQueryPlanCache()) {
      cache.put(
          Objects.requireNonNull(queryKey, "Query key is required"),
          new CachedPlan(
              Objects.requireNonNull(plan, "Query plan is required"), System.currentTimeMillis()));
    }
  }

  public synchronized void clear() {
    cache.clear();
  }

  public synchronized int size() {
    return cache.size();
  }

  public synchronized void evictExpired() {
    cache.entrySet().removeIf(entry -> entry.getValue().isExpired(config.cacheTtlSeconds()));
  }

  private Optional<RelNode> validPlan(final String queryKey, final CachedPlan cached) {
    if (cached.isExpired(config.cacheTtlSeconds())) {
      cache.remove(queryKey);
      return Optional.empty();
    }
    return Optional.of(cached.plan());
  }

  private record CachedPlan(RelNode plan, long timestamp) {
    boolean isExpired(final long ttlSeconds) {
      return System.currentTimeMillis() - timestamp > ttlSeconds * 1000;
    }
  }
}
