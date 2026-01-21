/*-
 * #%L
 * Cheshire :: Source Provider :: JDBC
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.source.jdbc;

import io.cheshire.spi.source.SourceProviderFactory;
import io.cheshire.spi.source.exception.SourceProviderConfigException;

public class JdbcSourceProviderFactory implements SourceProviderFactory<JdbcSourceProviderConfig> {

  public JdbcSourceProviderFactory() {
    //
  }

  @Override
  public Class<JdbcSourceProviderConfig> configType() {
    return JdbcSourceProviderConfig.class;
  }

  @Override
  public JdbcSourceProvider create(JdbcSourceProviderConfig config)
      throws SourceProviderConfigException {
    validate(config);
    return new JdbcSourceProvider(config);
  }

  @Override
  public JdbcSourceProviderConfigAdapter adapter() {
    return new JdbcSourceProviderConfigAdapter();
  }

  @Override
  public void validate(JdbcSourceProviderConfig config) throws SourceProviderConfigException {
    // TODO: validate config;
  }
}
