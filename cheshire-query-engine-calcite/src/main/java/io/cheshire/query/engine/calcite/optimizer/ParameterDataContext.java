/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.optimizer;

import java.util.Map;
import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;

/** DataContext implementation that provides parameter values */
class ParameterDataContext implements DataContext {
  private final SchemaPlus rootSchema;
  private final Map<String, Object> parameters;

  public ParameterDataContext(SchemaPlus rootSchema, Map<String, Object> parameters) {
    this.rootSchema = rootSchema;
    this.parameters = parameters;
  }

  @Override
  public SchemaPlus getRootSchema() {
    return rootSchema;
  }

  @Override
  public Object get(String name) {
    // Handle dynamic parameters
    if (name.startsWith("?")) {
      return parameters.get(name.substring(1));
    }
    return parameters.get(name);
  }

  @Override
  public QueryProvider getQueryProvider() {
    return null;
  }

  @Override
  public JavaTypeFactory getTypeFactory() {
    return (JavaTypeFactory) rootSchema.getSubSchema("").unwrap(RelDataTypeFactory.class);
  }
}
