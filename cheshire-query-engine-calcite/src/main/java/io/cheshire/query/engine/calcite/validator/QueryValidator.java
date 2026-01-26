/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.validator;

import io.cheshire.spi.query.exception.QueryEngineException;
import io.cheshire.spi.query.exception.QueryValidationException;
import io.cheshire.spi.query.exception.ValidationError;
import java.util.List;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlValidator;

public class QueryValidator {

  // TODO: This requires to extend sqlValidatorn rather then wraping it
  private SqlValidator sqlValidator;
  private SchemaPlus schema;
  private RelDataTypeFactory typeFactory;

  private QueryValidator() {
    // TODO: make it public if a default validator is needed
  }

  public QueryValidator(SchemaPlus defaultSchema, RelDataTypeFactory typeFactory) {
    this.schema = defaultSchema;
    this.typeFactory = typeFactory;
  }

  public void initialize(SqlValidator validator) {
    // TODO: Never called, sqlValidator is an interface, only to by pass compilation error
    this.sqlValidator = validator;
  }

  public SqlNode validate(SqlNode sqlNode) throws QueryEngineException {
    try {
      // TODO: Implemement lean validation
      return sqlValidator.validate(sqlNode);
    } catch (Exception e) {
      throw new QueryValidationException(
          "Validation error: "
              + List.of(new ValidationError("fields", e.getMessage(), "VALIDATION_ERROR")));
    }
  }
}
