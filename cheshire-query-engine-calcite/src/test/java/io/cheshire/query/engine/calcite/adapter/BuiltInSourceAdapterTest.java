/*-
 * #%L
 * Cheshire :: Query Engine :: Calcite
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.calcite.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.cheshire.query.engine.calcite.schema.CalciteSchemaAdapter;
import io.cheshire.spi.query.exception.QueryEngineInitializationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.tools.Frameworks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuiltInSourceAdapterTest {

  @TempDir private Path workspace;

  @Test
  void createsCsvSchemaFromDirectoryBackedSourceConfig()
      throws IOException, QueryEngineInitializationException {
    final Path csvDirectory = Files.createDirectory(workspace.resolve("csv"));
    Files.writeString(csvDirectory.resolve("people.csv"), "id:int,name:string\n1,Ada\n");

    final Schema schema = createSchema("csv", Map.of("directory", csvDirectory.toString()));

    assertThat(schema.getTableNames()).contains("people");
  }

  @Test
  void createsFileSchemaFromTableBackedSourceConfig()
      throws IOException, QueryEngineInitializationException {
    final Path htmlFile = workspace.resolve("departments.html");
    Files.writeString(
        htmlFile,
        """
        <html><body><table>
        <thead><tr><th>ID</th><th>NAME</th></tr></thead>
        <tbody><tr><td>10</td><td>Engineering</td></tr></tbody>
        </table></body></html>
        """);

    final Schema schema =
        createSchema(
            "file",
            Map.of(
                "tables",
                List.of(Map.of("name", "departments", "url", htmlFile.toUri().toString()))));

    assertThat(schema.getTableNames()).contains("departments");
  }

  @Test
  void createsRedisSchemaFromTableBackedSourceConfig() throws QueryEngineInitializationException {
    final Schema schema =
        createSchema(
            "redis",
            Map.of(
                "host",
                "localhost",
                "port",
                6379,
                "database",
                0,
                "password",
                "",
                "tables",
                List.of(
                    Map.of(
                        "name",
                        "people",
                        "operand",
                        Map.of(
                            "dataFormat",
                            "raw",
                            "fields",
                            List.of(Map.of("name", "id", "type", "varchar", "mapping", "id")))))));

    assertThat(schema.getTableNames()).contains("people");
  }

  @Test
  void createsInnodbSchemaFromMetadataPaths()
      throws IOException, QueryEngineInitializationException {
    final Path dataDirectory = Files.createDirectory(workspace.resolve("innodb"));
    final Path ddlFile = workspace.resolve("schema.sql");
    Files.writeString(
        ddlFile,
        """
        CREATE TABLE `EMP` (
          `EMPNO` int NOT NULL,
          PRIMARY KEY (`EMPNO`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """);

    final Schema schema =
        createSchema(
            "innodb",
            Map.of(
                "sqlFilePath",
                List.of(ddlFile.toString()),
                "ibdDataFileBasePath",
                dataDirectory.toString()));

    assertThat(schema).isNotNull();
  }

  @Test
  void createsKafkaSchemaFromConfiguredTables() throws QueryEngineInitializationException {
    final Schema schema =
        createSchema(
            "kafka",
            Map.of(
                "tables",
                List.of(
                    Map.of(
                        "name",
                        "events",
                        "operand",
                        Map.of(
                            "bootstrap.servers",
                            "localhost:9092",
                            "topic.name",
                            "events",
                            "consumer.params",
                            Map.of(
                                "key.deserializer",
                                "org.apache.kafka.common.serialization.ByteArrayDeserializer",
                                "value.deserializer",
                                "org.apache.kafka.common.serialization.ByteArrayDeserializer"))))));

    assertThat(schema.getTableNames()).contains("events");
  }

  @Test
  void createsOsSchemaWithSafeMetadataTables() throws QueryEngineInitializationException {
    final Schema schema = createSchema("os", Map.of());

    assertThat(schema.getTableNames()).contains("system_info", "java_info", "os_version");
  }

  @Test
  void createsMatSchemaThroughClasspathFactoryHook()
      throws IOException, QueryEngineInitializationException {
    final Path csvDirectory = Files.createDirectory(workspace.resolve("mat-plugin"));
    Files.writeString(
        csvDirectory.resolve("heap_objects.csv"), "id:int,class_name:string\n1,java.lang.String\n");

    final Schema schema =
        createSchema(
            "mat",
            Map.of(
                "factoryClass",
                "org.apache.calcite.adapter.file.FileSchemaFactory",
                "operand",
                Map.of("directory", csvDirectory.toString())));

    assertThat(schema.getTableNames()).contains("heap_objects");
  }

  @Test
  void rejectsMissingCassandraKeyspaceBeforeOpeningExternalConnection() {
    assertThatThrownBy(() -> createSchema("cassandra", Map.of("host", "localhost")))
        .isInstanceOf(QueryEngineInitializationException.class)
        .hasMessageContaining("Failed to create Cassandra schema")
        .hasRootCauseMessage("Source config missing required 'keyspace' field");
  }

  @Test
  void rejectsMissingFileDirectoryOrTables() {
    assertThatThrownBy(() -> createSchema("file", Map.of()))
        .isInstanceOf(QueryEngineInitializationException.class)
        .hasMessageContaining("Failed to create File schema")
        .hasRootCauseMessage("Source config requires one of these fields: [directory, tables]");
  }

  @Test
  void rejectsMissingMatFactoryClass() {
    assertThatThrownBy(() -> createSchema("mat", Map.of("operand", Map.of())))
        .isInstanceOf(QueryEngineInitializationException.class)
        .hasMessageContaining("Failed to create MAT schema")
        .hasRootCauseMessage("Source config missing required 'factoryClass' field");
  }

  private Schema createSchema(final String type, final Map<String, Object> config)
      throws QueryEngineInitializationException {
    final SchemaPlus rootSchema = Frameworks.createRootSchema(true);
    final Map<String, Object> sourceConfig =
        Map.of("name", type, "type", type, "config", withType(type, config));

    return new CalciteSchemaAdapter().createSchema(type, sourceConfig, rootSchema);
  }

  private Map<String, Object> withType(final String type, final Map<String, Object> config) {
    final java.util.HashMap<String, Object> copy = new java.util.HashMap<>(config);
    copy.put("type", type);
    return Map.copyOf(copy);
  }
}
