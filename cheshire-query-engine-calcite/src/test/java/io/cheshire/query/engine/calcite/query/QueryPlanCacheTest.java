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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cheshire.query.engine.calcite.config.CacheConfig;
import java.lang.reflect.Proxy;
import org.apache.calcite.rel.RelNode;
import org.junit.jupiter.api.Test;

class QueryPlanCacheTest {

  @Test
  void putDoesNotStorePlansWhenCacheIsDisabled() {
    final QueryPlanCache cache = new QueryPlanCache(new CacheConfig(false, true, 10, 3600));
    final RelNode plan = plan();

    cache.put("select-1", plan);

    assertTrue(cache.get("select-1").isEmpty());
    assertEquals(0, cache.size());
  }

  @Test
  void putEvictsLeastRecentlyUsedPlanWhenCacheExceedsMaxSize() {
    final QueryPlanCache cache = new QueryPlanCache(new CacheConfig(true, true, 2, 3600));
    final RelNode first = plan();
    final RelNode second = plan();
    final RelNode third = plan();

    cache.put("first", first);
    cache.put("second", second);
    assertSame(first, cache.get("first").orElseThrow());
    cache.put("third", third);

    assertSame(first, cache.get("first").orElseThrow());
    assertTrue(cache.get("second").isEmpty());
    assertSame(third, cache.get("third").orElseThrow());
  }

  @Test
  void getRemovesExpiredPlans() {
    final QueryPlanCache cache = new QueryPlanCache(new CacheConfig(true, true, 10, -1));

    cache.put("expired", plan());

    assertTrue(cache.get("expired").isEmpty());
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
