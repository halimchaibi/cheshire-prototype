/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.transformer;

import io.cheshire.spi.query.result.MapQueryResult;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;

import java.util.*;

/**
 * Transforms Calcite query execution results into MapQueryResult.
 * <p>
 * Converts Enumerator<Object[]> results from Calcite execution into the MapQueryResult format expected by the query
 * engine SPI.
 */
public class ResultTransformer {

    /**
     * Transforms a Calcite RelNode plan and enumerator to a MapQueryResult.
     *
     * @param plan
     *            the RelNode plan (for column metadata)
     * @param enumerator
     *            the enumerator containing row data
     * @return the MapQueryResult
     */
    public MapQueryResult transform(RelNode plan, Enumerator<Object[]> enumerator) {
        if (enumerator == null) {
            return new MapQueryResult(Collections.emptyList(), Collections.emptyList());
        }

        // Extract column metadata from the plan
        RelDataType rowType = plan.getRowType();
        List<MapQueryResult.Column> columns = new ArrayList<>();
        List<String> columnNames = new ArrayList<>();

        for (RelDataTypeField field : rowType.getFieldList()) {
            String columnName = field.getName();
            String typeName = field.getType().getSqlTypeName().toString();
            boolean nullable = field.getType().isNullable();

            columnNames.add(columnName);
            columns.add(new MapQueryResult.Column(columnName, typeName, nullable));
        }

        // Convert rows from Object[] to Map<String, Object>
        List<Map<String, Object>> rows = new ArrayList<>();

        try {
            while (enumerator.moveNext()) {
                Object[] current = enumerator.current();
                if (current == null) {
                    continue;
                }

                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 0; i < Math.min(columnNames.size(), current.length); i++) {
                    row.put(columnNames.get(i), current[i]);
                }
                rows.add(row);
            }
        } finally {
            enumerator.close();
        }

        return new MapQueryResult(columns, rows);
    }
}
