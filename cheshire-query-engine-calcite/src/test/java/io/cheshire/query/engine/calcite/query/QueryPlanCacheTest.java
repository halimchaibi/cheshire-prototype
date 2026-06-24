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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.cheshire.query.engine.calcite.config.CacheConfig;
import java.lang.reflect.Proxy;
import org.apache.calcite.rel.RelNode;
import org.junit.jupiter.api.Test;

class QueryPlanCacheTest {

  @Test
  void putDoesNotStorePlansWhenCacheIsDisabled() {
    QueryPlanCache cache = new QueryPlanCache(new CacheConfig(false, true, 10, 3600));
    RelNode plan = plan();

    cache.put("select-1", plan);

    assertNull(cache.get("select-1"));
    assertEquals(0, cache.size());
  }

  @Test
  void putEvictsLeastRecentlyUsedPlanWhenCacheExceedsMaxSize() {
    QueryPlanCache cache = new QueryPlanCache(new CacheConfig(true, true, 2, 3600));
    RelNode first = plan();
    RelNode second = plan();
    RelNode third = plan();

    cache.put("first", first);
    cache.put("second", second);
    assertSame(first, cache.get("first"));
    cache.put("third", third);

    assertSame(first, cache.get("first"));
    assertNull(cache.get("second"));
    assertSame(third, cache.get("third"));
  }

  @Test
  void getRemovesExpiredPlans() {
    QueryPlanCache cache = new QueryPlanCache(new CacheConfig(true, true, 10, -1));

    cache.put("expired", plan());

    assertNull(cache.get("expired"));
    assertEquals(0, cache.size());
  }

  private RelNode plan() {
    return (RelNode)
        Proxy.newProxyInstance(
            RelNode.class.getClassLoader(),
            new Class<?>[] {RelNode.class},
            (proxy, method, args) -> {
              if ("toString".equals(method.getName())) {
                return "test-plan";
              }
              throw new UnsupportedOperationException(method.getName());
            });
  }
}
