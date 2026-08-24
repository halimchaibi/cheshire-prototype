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

import java.util.*;
import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.schema.SchemaPlus;

/** Calcite adapter over QueryRuntimeContext. This is the ONLY class that implements DataContext. */
public final class CalciteDataContext implements DataContext {

  private final QueryRuntimeContext runtimeContext;
  private final SchemaPlus rootSchema;
  private final JavaTypeFactory typeFactory;

  public CalciteDataContext(QueryRuntimeContext runtimeContext, SchemaPlus rootSchema) {
    this.runtimeContext = runtimeContext;
    this.rootSchema = rootSchema;
    this.typeFactory = new JavaTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
  }

  @Override
  public SchemaPlus getRootSchema() {
    return rootSchema;
  }

  @Override
  public JavaTypeFactory getTypeFactory() {
    return typeFactory;
  }

  @Override
  public QueryProvider getQueryProvider() {
    return null; // not using linq4j
  }

  @Override
  public Object get(String name) {
    if (Variable.TIME_ZONE.camelName.equals(name)) {
      return runtimeContext.timeZone();
    }
    if (Variable.LOCALE.camelName.equals(name)) {
      return runtimeContext.locale();
    }
    if (Variable.CANCEL_FLAG.camelName.equals(name)) {
      return runtimeContext.cancelFlag();
    }

    if (runtimeContext.hasParameter(name)) {
      return runtimeContext.parameter(name);
    }

    return null;
  }
}
