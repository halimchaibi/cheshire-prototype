/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite;

enum ExecutionStage {
  EXTRACT_PARAMETERS,
  PARSE,
  VALIDATE,
  RESOLVE_PARAMETERS,
  CONVERT,
  BUILD_CONTEXT,
  SELECT_RULES,
  OPTIMIZE,
  EXECUTE,
  TRANSFORM
}
