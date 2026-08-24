/*-
 * #%L
 * Cheshire :: Core
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.core.manager;

import static org.assertj.core.api.Assertions.assertThat;

import io.cheshire.core.config.CheshireConfig;
import java.lang.reflect.Constructor;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SourceProviderManagerTest {

  @Test
  void skipsSourceDefinitionsWithoutProviderFactory() throws Exception {
    final CheshireConfig.Source osSource = new CheshireConfig.Source();
    osSource.setName("os");
    osSource.setType("os");
    osSource.setConfig(Map.of("type", "os"));

    final CheshireConfig config = new CheshireConfig();
    config.setSources(Map.of("os", osSource));

    final SourceProviderManager manager = newManager(config);

    manager.initialize();

    assertThat(manager.size()).isZero();
  }

  private static SourceProviderManager newManager(final CheshireConfig config) throws Exception {
    final Constructor<SourceProviderManager> constructor =
        SourceProviderManager.class.getDeclaredConstructor(CheshireConfig.class);
    constructor.setAccessible(true);
    return constructor.newInstance(config);
  }
}
