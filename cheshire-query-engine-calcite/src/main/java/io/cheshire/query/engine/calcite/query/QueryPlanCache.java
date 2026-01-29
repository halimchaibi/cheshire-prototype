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
import org.apache.calcite.rel.RelNode;

public class QueryPlanCache {

  private final CacheConfig config;
  private final Map<String, CachedPlan> cache;

  public QueryPlanCache(CacheConfig config) {
    this.config = config;
    // LinkedHashMap with access-order for LRU behavior
    this.cache =
        new LinkedHashMap<>(config.maxCacheSize(), 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, CachedPlan> eldest) {
            return size() > config.maxCacheSize();
          }
        };
  }

  public synchronized RelNode get(String queryKey) {
    CachedPlan cached = cache.get(queryKey);
    if (cached != null && !cached.isExpired(config.cacheTtlSeconds())) {
      return cached.plan();
    }
    if (cached != null && cached.isExpired(config.cacheTtlSeconds())) {
      cache.remove(queryKey);
    }
    return null;
  }

  public synchronized void put(String queryKey, RelNode plan) {
    if (config.enableQueryPlanCache()) {
      cache.put(queryKey, new CachedPlan(plan, System.currentTimeMillis()));
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

  private record CachedPlan(RelNode plan, long timestamp) {
    boolean isExpired(long ttlSeconds) {
      return System.currentTimeMillis() - timestamp > ttlSeconds * 1000;
    }
  }
}
