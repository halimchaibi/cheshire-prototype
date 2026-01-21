/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.schema;

import io.cheshire.source.jdbc.JdbcSourceProviderConfig;
import io.cheshire.spi.source.SourceProvider;
import io.cheshire.spi.source.exception.SourceProviderException;
import org.apache.calcite.adapter.jdbc.JdbcSchema;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapts SourceProvider instances to Calcite Schema objects.
 * <p>
 * Currently supports JDBC source providers by creating Calcite JdbcSchema instances from the JDBC connection
 * configuration.
 */
public class CalciteSchemaAdapter {

    /**
     * Creates a Calcite Schema from a SourceProvider.
     *
     * @param schemaName
     *            the name for the schema in Calcite
     * @param sourceProvider
     *            the source provider to adapt
     * @param rootSchema
     *            the root schema to add the schema to
     * @return the Calcite Schema instance
     * @throws SourceProviderException
     *             if adaptation fails
     */
    public Schema createSchema(String schemaName, SourceProvider<?, ?> sourceProvider, SchemaPlus rootSchema)
            throws SourceProviderException {

        // Check if this is a JDBC source provider
        if (sourceProvider.config().type().equals("jdbc")) {
            return createJdbcSchema(schemaName, sourceProvider, rootSchema);
        }

        throw new SourceProviderException(
                "Unsupported source provider type for Calcite: " + sourceProvider.config().type());
    }

    /**
     * Creates a Calcite JdbcSchema from a JDBC source provider.
     */
    private Schema createJdbcSchema(String schemaName, SourceProvider<?, ?> sourceProvider, SchemaPlus rootSchema)
            throws SourceProviderException {

        try {
            // Check if schema already exists
            if (rootSchema.getSubSchema(schemaName) != null) {
                // Schema already registered, return it
                return rootSchema.getSubSchema(schemaName);
            }

            // Get the JDBC config
            if (!(sourceProvider.config() instanceof JdbcSourceProviderConfig jdbcConfig)) {
                throw new SourceProviderException(
                        "Source provider config is not a JdbcDataSourceConfig: " + sourceProvider.config().getClass());
            }

            // Build operand map for JdbcSchema.create()
            Map<String, Object> operand = new HashMap<>();
            operand.put("jdbcUrl", jdbcConfig.require("jdbcUrl"));
            operand.put("jdbcDriver", jdbcConfig.require("driverClassName"));

            String username = jdbcConfig.get("username");
            String password = jdbcConfig.get("password");

            if (username != null) {
                operand.put("jdbcUser", username);
            }
            if (password != null) {
                operand.put("jdbcPassword", password);
            }

            // Create the JDBC schema
            // JdbcSchema.create() signature: (SchemaPlus parentSchema, String name, Map<String, Object> operand)
            JdbcSchema jdbcSchema = JdbcSchema.create(rootSchema, schemaName, operand);

            // Verify schema was added to rootSchema
            // JdbcSchema.create() should add it automatically, but verify and add manually if needed
            if (rootSchema.getSubSchema(schemaName) == null) {
                // If not added automatically, add it manually
                rootSchema.add(schemaName, jdbcSchema);
            }

            return jdbcSchema;

        } catch (Exception e) {
            throw new SourceProviderException("Failed to create Calcite JDBC schema for source: " + schemaName, e);
        }
    }
}
