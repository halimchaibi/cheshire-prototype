/*-
 * #%L
 * Cheshire :: Query Engine :: JDBC
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.query.engine.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.cheshire.spi.query.exception.QueryEngineException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SqlTemplateQueryBuilder")
final class SqlTemplateQueryBuilderTest {

  @Nested
  @DisplayName("SELECT Query Building")
  final class SelectQueryTests {

    @Test
    @DisplayName("should build simple SELECT query")
    void shouldBuildSimpleSelectQuery() throws QueryEngineException {
      final var template =
          """
                    {
                        "operation": "SELECT",
                        "source": { "table": "users", "alias": "u" },
                        "projection": [
                            { "field": "u.id", "alias": "id" },
                            { "field": "u.name", "alias": "name" }
                        ]
                    }
                    """;

      final var request = SqlTemplateQueryBuilder.buildQuery(template, Map.of());

      assertThat(request).isNotNull();
      assertThat(request.sqlQuery())
          .containsIgnoringCase("SELECT")
          .containsIgnoringCase("FROM users");
    }

    @Test
    @DisplayName("should build SELECT with WHERE clause")
    void shouldBuildSelectWithWhereClause() throws QueryEngineException {
      final var template =
          """
                    {
                        "operation": "SELECT",
                        "source": { "table": "users" },
                        "projection": [{ "field": "id" }],
                        "filters": {
                            "op": "AND",
                            "conditions": [
                                { "field": "id", "op": "=", "param": "userId" }
                            ]
                        }
                    }
                    """;

      final var params = Map.<String, Object>of("userId", 123L);
      final var request = SqlTemplateQueryBuilder.buildQuery(template, params);

      assertThat(request.sqlQuery()).containsIgnoringCase("WHERE").containsIgnoringCase("id");
      assertThat(request.parameters()).containsEntry("userId", 123L);
    }
  }

  @Nested
  @DisplayName("INSERT Query Building")
  final class InsertQueryTests {

    @Test
    @DisplayName("should build simple INSERT query")
    void shouldBuildSimpleInsertQuery() throws QueryEngineException {
      final var template =
          """
                    {
                        "operation": "INSERT",
                        "source": { "table": "users" },
                        "columns": [
                            { "field": "name", "param": "name" },
                            { "field": "email", "param": "email" }
                        ],
                        "returning": ["id"]
                    }
                    """;

      final var params = Map.<String, Object>of("name", "John Doe", "email", "john@example.com");

      final var request = SqlTemplateQueryBuilder.buildQuery(template, params);

      assertThat(request.sqlQuery())
          .containsIgnoringCase("INSERT INTO users")
          .containsIgnoringCase("name")
          .containsIgnoringCase("email");
      assertThat(request.parameters())
          .containsEntry("name", "John Doe")
          .containsEntry("email", "john@example.com");
    }
  }

  @Nested
  @DisplayName("UPDATE Query Building")
  final class UpdateQueryTests {

    @Test
    @DisplayName("should build UPDATE query with SET and WHERE")
    void shouldBuildUpdateQuery() throws QueryEngineException {
      final var template =
          """
                    {
                        "operation": "UPDATE",
                        "source": { "table": "users" },
                        "set": [
                            { "field": "name", "param": "name" }
                        ],
                        "filters": {
                            "op": "AND",
                            "conditions": [
                                { "field": "id", "op": "=", "param": "id" }
                            ]
                        }
                    }
                    """;

      final var params = Map.<String, Object>of("id", 123L, "name", "Jane Doe");

      final var request = SqlTemplateQueryBuilder.buildQuery(template, params);

      assertThat(request.sqlQuery())
          .containsIgnoringCase("UPDATE users")
          .containsIgnoringCase("SET")
          .containsIgnoringCase("WHERE");
      assertThat(request.parameters()).containsEntry("id", 123L);
    }
  }

  @Nested
  @DisplayName("DELETE Query Building")
  final class DeleteQueryTests {

    @Test
    @DisplayName("should build DELETE query with WHERE clause")
    void shouldBuildDeleteQuery() throws QueryEngineException {
      final var template =
          """
                    {
                        "operation": "DELETE",
                        "source": { "table": "users" },
                        "filters": {
                            "op": "AND",
                            "conditions": [
                                { "field": "id", "op": "=", "param": "id" }
                            ]
                        },
                        "allowDeleteAll": false
                    }
                    """;

      final var params = Map.<String, Object>of("id", 123L);
      final var request = SqlTemplateQueryBuilder.buildQuery(template, params);

      assertThat(request.sqlQuery())
          .containsIgnoringCase("DELETE FROM users")
          .containsIgnoringCase("WHERE");
    }
  }

  @Nested
  @DisplayName("Error Handling")
  final class ErrorHandlingTests {

    @Test
    @DisplayName("should throw exception for invalid JSON template")
    void shouldThrowExceptionForInvalidJson() {
      final var invalidTemplate = "{ invalid json }";

      assertThatThrownBy(() -> SqlTemplateQueryBuilder.buildQuery(invalidTemplate, Map.of()))
          .isInstanceOf(QueryEngineException.class);
    }

    // TODO: handle missing parameter and raise a meaningfull ecep^tion
    //    @Test
    //    @DisplayName("should throw exception for missing required parameter")
    //    void shouldThrowExceptionForMissingParameter() {
    //      final var template =
    //          """
    //                    {
    //                        "operation": "SELECT",
    //                        "source": { "table": "users" },
    //                        "projection": [{ "field": "id" }],
    //                        "filters": {
    //                            "conditions": [
    //                                { "field": "id", "op": "=", "param": "userId" }
    //                            ]
    //                        }
    //                    }
    //                    """;
    //
    //      // Missing required userId parameter
    //      assertThatThrownBy(() -> SqlTemplateQueryBuilder.buildQuery(template, Map.of()))
    //          .isInstanceOf(QueryExecutionException.class);
    //    }
  }
}
