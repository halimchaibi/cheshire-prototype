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

import java.util.List;

/** Declarative catalog of built-in Calcite source adapters shipped with the query engine. */
public final class BuiltInSourceAdapters {

  private BuiltInSourceAdapters() {}

  public static List<SourceAdapterProvider> providers() {
    return List.of(
        typed("jdbc", new JdbcAdapter()),
        typed(
            "arrow",
            SchemaFactorySourceAdapter.of(
                "ARROW",
                "Arrow",
                "org.apache.calcite.adapter.arrow.ArrowSchemaFactory",
                List.of("directory"),
                List.of(),
                List.of("directory"),
                List.of())),
        typed(
            "csv",
            SchemaFactorySourceAdapter.of(
                "CSV",
                "CSV",
                "org.apache.calcite.adapter.file.FileSchemaFactory",
                List.of("directory"),
                List.of(),
                List.of("directory"),
                List.of())),
        typed(
            "cassandra",
            SchemaFactorySourceAdapter.of(
                "CASSANDRA",
                "Cassandra",
                "org.apache.calcite.adapter.cassandra.CassandraSchemaFactory",
                List.of("host", "keyspace"),
                List.of(),
                List.of(),
                List.of())),
        typed(
            "file",
            SchemaFactorySourceAdapter.of(
                "FILE",
                "File",
                "org.apache.calcite.adapter.file.FileSchemaFactory",
                List.of(),
                List.of("directory", "tables"),
                List.of("directory"),
                List.of())),
        typed(
            "innodb",
            SchemaFactorySourceAdapter.of(
                "INNODB",
                "InnoDB",
                "org.apache.calcite.adapter.innodb.InnodbSchemaFactory",
                List.of("sqlFilePath", "ibdDataFileBasePath"),
                List.of(),
                List.of("ibdDataFileBasePath"),
                List.of("sqlFilePath"))),
        typed("os", new OsAdapter()),
        typed(
            "redis",
            SchemaFactorySourceAdapter.withJsonCustomTables(
                "REDIS",
                "Redis",
                "org.apache.calcite.adapter.redis.RedisSchemaFactory",
                List.of("host", "port", "database", "tables"),
                List.of(),
                List.of(),
                List.of())),
        typed(
            "kafka",
            new TableFactorySourceAdapter(
                "KAFKA", "Kafka", "org.apache.calcite.adapter.kafka.KafkaTableFactory")),
        typed("mat", SchemaFactorySourceAdapter.plugin("MAT", "MAT")));
  }

  private static SourceAdapterProvider typed(final String type, final SourceAdapter adapter) {
    return new TypedSourceAdapterProvider(type, adapter);
  }
}
