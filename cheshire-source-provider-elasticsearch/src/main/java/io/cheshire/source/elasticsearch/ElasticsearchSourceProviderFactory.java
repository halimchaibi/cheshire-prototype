/*-
 * #%L
 * Cheshire :: Source Provider :: Elasticsearch
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.source.elasticsearch;

import io.cheshire.spi.source.SourceProvider;
import io.cheshire.spi.source.SourceProviderConfigAdapter;
import io.cheshire.spi.source.SourceProviderFactory;
import io.cheshire.spi.source.exception.SourceProviderConfigException;
import io.cheshire.spi.source.exception.SourceProviderException;
import java.util.Objects;

public final class ElasticsearchSourceProviderFactory
    implements SourceProviderFactory<ElasticsearchSourceProviderConfig> {

  @Override
  public Class<ElasticsearchSourceProviderConfig> configType() {
    return ElasticsearchSourceProviderConfig.class;
  }

  @Override
  public void validate(ElasticsearchSourceProviderConfig config) throws SourceProviderException {
    final var sourceConfig = Objects.requireNonNull(config, "config must not be null");
    if (sourceConfig.apiKey().isPresent() && sourceConfig.basicAuth().isPresent()) {
      throw new SourceProviderConfigException("Use either apiKey or basic auth, not both", config);
    }
  }

  @Override
  public SourceProviderConfigAdapter<ElasticsearchSourceProviderConfig> adapter() {
    return new ElasticsearchSourceProviderConfigAdapter();
  }

  @Override
  public SourceProvider<?> create(ElasticsearchSourceProviderConfig config)
      throws SourceProviderException {
    validate(config);
    return new ElasticsearchSourceProvider(config);
  }
}
