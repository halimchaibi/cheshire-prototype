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

import org.apache.calcite.DataContext;
import org.apache.calcite.plan.Context;
import org.jspecify.annotations.Nullable;

public final class PlannerContext implements Context {

  private final DataContext dataContext;

  public PlannerContext(DataContext dataContext) {
    this.dataContext = dataContext;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <C> @Nullable C unwrap(Class<C> clazz) {

    if (clazz.isInstance(dataContext)) {
      return (C) dataContext;
    }
    return null;
  }
}
