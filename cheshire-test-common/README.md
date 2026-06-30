# Cheshire Test Common

Common testing utilities and architecture tests for the Cheshire Framework.

## Overview

This module provides:

- **ArchUnit Tests** - Architecture validation using ArchUnit.
- **Module Coverage Guard** - Verifies the architecture suite imports every governed module.
- **Test Utilities** - Common test fixtures and helpers.

## Architecture Tests

### ModuleCoverageTest

Validates that the ArchUnit classpath includes every governed module package:

- `io.cheshire.common`
- `io.cheshire.core`
- `io.cheshire.spi.pipeline`
- `io.cheshire.spi.query`
- `io.cheshire.spi.source`
- `io.cheshire.query.engine.jdbc`
- `io.cheshire.query.engine.calcite`
- `io.cheshire.source.jdbc`
- `io.cheshire.source.elasticsearch`
- `io.cheshire.jetty`
- `io.cheshire.stdio`
- `io.cheshire.runtime`

This test fails when a module is added to the reactor but not wired into the architecture-test
module dependencies.

### CheshireArchitectureTest

Validates core architectural constraints:

- **SPI isolation** - SPI packages must not depend on core, providers, transports, or runtime.
- **Core neutrality** - Core must not depend on concrete provider, query-engine, transport, or runtime packages.
- **Provider isolation** - Query engines and source providers must not depend on transports or runtime.
- **Transport isolation** - Jetty and stdio packages must not depend on runtime.
- **Runtime top layer** - Lower modules must not depend on `io.cheshire.runtime`.
- **SPI compliance** - Query engines, source providers, and their factories implement the correct SPIs.
- **Core package cycles** - Core package slices must remain acyclic.
- **Naming conventions** - Core manager classes belong under manager packages.

### PipelineArchitectureTest

Validates the three-stage pipeline pattern:

- **PreProcessor** - Input validation and transformation naming.
- **Executor** - Business logic execution naming.
- **PostProcessor** - Output transformation and enrichment naming.
- **MaterializedInput/Output** - Immutable records.
- **Method signatures** - Correct generic `Step.apply(value, Context)` contract.
- **Functional interfaces** - Pipeline stage interfaces stay lambda-friendly.
- **Package organization** - Framework pipeline implementations stay in pipeline packages.
- **PipelineProcessor** - Immutable orchestration record.

### SecurityArchitectureTest

Enforces security best practices:

- **No process stream writes** - Production code must not write to `System.out` or `System.err`.
- **Immutable source configs** - Source provider configs are records.
- **Immutable query configs** - Query engine configs are records.

### CodingStandardsTest

Validates focused functional-Java standards:

- **SPI immutability** - SPI fields are final.
- **Records for query/result values** - Implementation query and result data carriers are records.
- **Null-safety conventions** - Public APIs must not be annotated nullable.
- **Specific error contracts** - SPI public APIs must not declare raw `Throwable`.

## Running Tests

### Run All Architecture Tests

```bash
mvn -q -Ptest -pl cheshire-test-common -am test
```

### Run Full Reactor Verification

```bash
mvn -q -Ptest verify
```

### Run Specific Test Class

```bash
mvn -q -Ptest -pl cheshire-test-common -am -Dtest=ModuleCoverageTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -q -Ptest -pl cheshire-test-common -am -Dtest=CheshireArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -q -Ptest -pl cheshire-test-common -am -Dtest=PipelineArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -q -Ptest -pl cheshire-test-common -am -Dtest=SecurityArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -q -Ptest -pl cheshire-test-common -am -Dtest=CodingStandardsTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## Architecture Rules Summary

### Module Dependencies

`runtime -> transports -> core -> spi/common`

Concrete query engines and source providers hang off SPI contracts and must remain independent of
transport and runtime packages.

### Naming And Contract Requirements

| Pattern | Example | Requirement |
| --- | --- | --- |
| `*Manager` | `ConfigurationManager` | Resides in `..manager..` |
| `*QueryEngine` | `JdbcQueryEngine` | Implements `QueryEngine` |
| `*SourceProvider` | `JdbcSourceProvider` | Implements `SourceProvider` |
| `*Factory` | `JdbcQueryEngineFactory` | Implements the matching factory SPI |
| `*Config` in providers/engines | `JdbcQueryEngineConfig` | Record |
| `*Query` in providers/engines | `SqlQuery` | Record |
| `*Result` in providers/engines | `ElasticsearchQueryResult` | Record |

### SPI Implementation Requirements

**Query Engines:**

```java
public class JdbcQueryEngine implements QueryEngine<SqlQueryEngineRequest> {
  // Must implement QueryEngine SPI.
}

public class JdbcQueryEngineFactory implements QueryEngineFactory<JdbcQueryEngineConfig> {
  // Must implement QueryEngineFactory SPI.
}
```

**Source Providers:**

```java
public class JdbcSourceProvider implements SourceProvider<SqlSourceProviderQuery> {
  // Must implement SourceProvider SPI.
}

public class JdbcSourceProviderFactory implements SourceProviderFactory<JdbcSourceProviderConfig> {
  // Must implement SourceProviderFactory SPI.
}
```

**Pipeline Processors:**

```java
public class MyPreProcessor implements PreProcessor<MaterializedInput> {
  @Override
  public MaterializedInput apply(MaterializedInput input, Context ctx) {
    return input;
  }
}

public class MyExecutor implements Executor<MaterializedInput, MaterializedOutput> {
  @Override
  public MaterializedOutput apply(MaterializedInput input, Context ctx) {
    return MaterializedOutput.empty();
  }
}

public class MyPostProcessor implements PostProcessor<MaterializedOutput> {
  @Override
  public MaterializedOutput apply(MaterializedOutput output, Context ctx) {
    return output;
  }
}
```

## Adding New Architecture Tests

1. Choose the appropriate test class:
   - General layering -> `CheshireArchitectureTest`
   - Pipeline-specific -> `PipelineArchitectureTest`
   - Security-related -> `SecurityArchitectureTest`
   - Coding standards -> `CodingStandardsTest`
   - Module classpath coverage -> `ModuleCoverageTest`
2. Add a focused `@ArchTest` rule with a clear `.because(...)` reason.
3. Document the rule in this README.
4. Run the focused test class and then the full architecture suite.

## Dependencies

- **ArchUnit** - Architecture testing framework.
- **JUnit 5** - Test framework.
- **All governed Cheshire modules** - Imported through test-scope module dependencies.

## CI Integration

Architecture tests should be part of CI:

```yaml
- name: Run Architecture Tests
  run: mvn -q -Ptest -pl cheshire-test-common -am test
```

## Best Practices

1. Keep each rule focused on one architectural concern.
2. Prefer package-boundary and SPI-contract rules over implementation-style guesses.
3. Use `.because()` to explain why a violation matters.
4. Update `ModuleCoverageTest` and this README when adding governed modules.
5. Document intentional exceptions in the rule text instead of hiding them in broad exclusions.
