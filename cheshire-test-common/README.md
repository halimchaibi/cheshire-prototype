# Cheshire Test Common

Common testing utilities and architecture tests for the Cheshire Framework.

## Overview

This module provides:

- **ArchUnit Tests** - Architecture validation using ArchUnit
- **Test Utilities** - Common test fixtures and helpers
- **Test Base Classes** - Base classes for integration tests

## Architecture Tests

### CheshireArchitectureTest

Validates core architectural constraints:

- ✅ **Module Layering** - Ensures proper dependency hierarchy
    - SPI modules are independent (base layer)
    - Core depends only on SPI
    - Server depends on Core and SPI
    - Runtime is the top layer

- ✅ **No Circular Dependencies** - Prevents cyclic module dependencies
- ✅ **SPI Compliance** - Query engines and source providers implement correct SPIs
- ✅ **Factory Pattern** - Factories implement appropriate SPI factory interfaces
- ✅ **Exception Hierarchy** - All exceptions extend Exception
- ✅ **Naming Conventions** - Consistent class naming across modules
- ✅ **Immutability** - Config/request/response classes are records

### PipelineArchitectureTest

Validates the three-stage pipeline pattern:

- ✅ **PreProcessor** - Input validation and transformation
- ✅ **Executor** - Business logic execution
- ✅ **PostProcessor** - Output transformation and enrichment
- ✅ **MaterializedInput/Output** - Immutable data carriers
- ✅ **Method Signatures** - Correct parameter and return types
- ✅ **Package Organization** - Pipeline implementations properly organized

### SecurityArchitectureTest

Enforces security best practices:

- ✅ **SQL Injection Prevention** - No string concatenation for SQL
- ✅ **No Hardcoded Credentials** - Passwords/keys in config, not code
- ✅ **Immutable Security Configs** - Security configs are records
- ✅ **Final Auth Classes** - Authentication classes are final
- ✅ **Immutable Collections** - Sensitive data uses immutable collections

### CodingStandardsTest

Validates Scala-influenced functional Java style:

- ✅ **Immutability** - Fields are final (or atomic for state)
- ✅ **Records for DTOs** - Value objects use Java 21 records
- ✅ **Optional Instead of Null** - Finder methods return Optional
- ✅ **SLF4J Logging** - Consistent logging via SLF4J
- ✅ **No System.out/err** - Use logging instead
- ✅ **Utility Classes** - Final with private constructors
- ✅ **Sealed Interfaces** - ADTs use sealed interfaces
- ✅ **Constructor Injection** - Dependencies via constructor

## Running Tests

### Run All Architecture Tests

```bash
# From cheshire root
mvn test -pl cheshire-test-common

# With coverage
mvn test -pl cheshire-test-common jacoco:report
```

### Run Specific Test Class

```bash
mvn test -Dtest=CheshireArchitectureTest
mvn test -Dtest=PipelineArchitectureTest
mvn test -Dtest=SecurityArchitectureTest
mvn test -Dtest=CodingStandardsTest
```

### Run from IDE

All tests can be run directly from your IDE using JUnit 5 runners. Simply right-click on the test class and select "Run".

## Architecture Rules Summary

### Module Dependencies (Enforced)

```
┌─────────────────────────────────────────┐
│           cheshire-runtime              │ (Top layer)
└─────────────────────────────────────────┘
                  ↓ depends on
┌─────────────────────────────────────────┐
│           cheshire-server               │
└─────────────────────────────────────────┘
                  ↓ depends on
┌─────────────────────────────────────────┐
│           cheshire-core                 │
└─────────────────────────────────────────┘
                  ↓ depends on
┌─────────────────────────────────────────┐
│        SPI Modules (base layer)         │
│  • cheshire-pipeline-spi                │
│  • cheshire-query-engine-spi            │
│  • cheshire-source-provider-spi         │
└─────────────────────────────────────────┘
```

### Naming Conventions (Enforced)

| Pattern        | Example                  | Location                     |
|----------------|--------------------------|------------------------------|
| `*Manager`     | `ConfigurationManager`   | `..manager..`                |
| `*QueryEngine` | `JdbcQueryEngine`        | `..query.engine..`           |
| `*Provider`    | `JdbcDataSourceProvider` | `..source..`                 |
| `*Factory`     | `JdbcQueryEngineFactory` | SPI implementation           |
| `*Config`      | `JdbcQueryEngineConfig`  | Any package (must be record) |
| `*Request`     | `SqlQueryRequest`        | Any package (must be record) |
| `*Response`    | `ResponseEntity`         | Any package (must be sealed) |
| `*Processor`   | `BlogInputProcessor`     | `..pipeline..`               |
| `*Executor`    | `BlogExecutor`           | `..pipeline..`               |

### SPI Implementation Requirements

**Query Engines:**

```java
public class JdbcQueryEngine implements QueryEngine<SqlQueryRequest, JdbcDataSourceProvider> {
    // Must implement QueryEngine SPI
}

public class JdbcQueryEngineFactory implements QueryEngineFactory<...> {
    // Must implement QueryEngineFactory SPI
    // Must be registered in META-INF/services
}
```

**Source Providers:**

```java
public class JdbcDataSourceProvider implements SourceProvider<SqlQuery, SqlQueryResult> {
    // Must implement SourceProvider SPI
}

public class JdbcDataSourceProviderFactory implements SourceProviderFactory<...> {
    // Must implement SourceProviderFactory SPI
    // Must be registered in META-INF/services
}
```

**Pipeline Processors:**

```java
public class MyPreProcessor implements PreProcessor {
    @Override
    public MaterializedInput process(MaterializedInput input) { ... }
}

public class MyExecutor implements Executor {
    @Override
    public MaterializedOutput execute(MaterializedInput input, SessionTask task) { ... }
}

public class MyPostProcessor implements PostProcessor {
    @Override
    public MaterializedOutput process(MaterializedOutput output) { ... }
}
```

## Adding New Architecture Tests

To add new architecture rules:

1. **Choose the appropriate test class:**
    - General layering → `CheshireArchitectureTest`
    - Pipeline-specific → `PipelineArchitectureTest`
    - Security-related → `SecurityArchitectureTest`
    - Coding standards → `CodingStandardsTest`

2. **Add the rule:**

```java
@ArchTest
static final ArchRule my_New_Rule =
        classes().that()...
                .should()...
                .because("Clear explanation of why this rule exists");
```

3. **Document the rule:**
    - Add comprehensive Javadoc explaining the rule
    - Include examples of violations and correct patterns
    - Reference relevant framework documentation

4. **Run and verify:**

```bash
mvn test -Dtest=YourTestClass
```

## Dependencies

- **ArchUnit** `1.2.1` - Architecture testing framework
- **JUnit 5** `5.10.1` - Test framework
- **All Cheshire modules** - For comprehensive testing

## Integration with CI/CD

Architecture tests should be part of your CI/CD pipeline:

```yaml
# Example GitHub Actions
- name: Run Architecture Tests
  run: mvn test -pl cheshire-test-common
  
- name: Fail on Architecture Violations
  run: mvn verify -pl cheshire-test-common
```

## Best Practices

1. **Keep rules focused** - Each rule should test one architectural concern
2. **Provide clear messages** - Use `.because()` to explain why the rule exists
3. **Use examples** - Show both violations and correct patterns in Javadoc
4. **Test incrementally** - Add rules as architectural patterns emerge
5. **Document exceptions** - If a rule has legitimate exceptions, document them

## References

- [ArchUnit Documentation](https://www.archunit.org/)
- [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html)
- [Cheshire Architecture](../docs/architecture/)
- [Cheshire Coding Standards](../.cursorrules)

---

**Module**: `cheshire-test-common`  
**Purpose**: Testing utilities and architecture validation  
**Maintainers**: Cheshire Framework Team

