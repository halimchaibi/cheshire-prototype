# Architectural Review - Production MVP Hardening

Date: 2026-06-30

## 1. Multi-Module Hierarchy & Dependency Architecture

### Compile-scope logging bindings

Locate: `cheshire-server/pom.xml`, `cheshire-query-engine-calcite/pom.xml`

Diagnose: Library modules exposed `slf4j-simple` on the compile classpath. That forces a logging
binding onto downstream applications and can break container logging, MCP stdio, and application
server deployments.

Refactor: Scope `slf4j-simple` to tests only. Runtime applications should choose their own SLF4J
provider.

Test: Full Maven reactor tests validate that modules still compile and execute with test-only
bindings.

### Core classpath pollution

Locate: `cheshire-core/pom.xml`, `cheshire-common/pom.xml`

Diagnose: Core and common modules carried unused Jetty, MCP, and Swagger dependencies. This
expanded the framework baseline classpath and blurred module ownership.

Refactor: Remove unused transport and OpenAPI dependencies from core/common. Keep transport-specific
dependencies in `cheshire-server`.

Test: `mvn -q -Ptest test`

### Elasticsearch source module

Locate: `cheshire-source-provider-elasticsearch`

Diagnose: Search-backed workloads needed a source-provider implementation without imposing the
official Elasticsearch Java client dependency graph on the base framework.

Refactor: Add a dedicated SPI module using JDK `HttpClient`, Jackson, immutable records, API-key or
basic auth, request/connect timeouts, retries with jitter for transient HTTP statuses, and
`maxResults` capping.

Test: `ElasticsearchSourceProviderTest` runs against an in-process HTTP server and validates request
shape plus row materialization.

## 2. State Management, Concurrency & Threading

### Shared context state

Locate: `cheshire-pipeline-spi/src/main/java/io/cheshire/spi/pipeline/Context.java`

Diagnose: The interface owned a static `ConcurrentMap`, so attributes leaked across unrelated
contexts. Under concurrent request traffic this creates cross-request contamination.

Refactor: Require each context implementation to provide its own `attributes()` map and null-check
keys/values at the boundary.

Test: `ContextIsolationTest`

### Blocking async pipeline execution

Locate: `cheshire-pipeline-spi/src/main/java/io/cheshire/spi/pipeline/PipelineProcessor.java`

Diagnose: `executeAsync` created a virtual-thread executor in try-with-resources and closed it
before returning. The returned `CompletableFuture` behaved synchronously for slow pipelines.

Refactor: Use a reusable virtual-thread `ThreadFactory` backed executor that starts one virtual
thread per async pipeline task.

Test: `PipelineProcessorAsyncTest`

### Lifecycle startup and restart safety

Locate: `cheshire-core/src/main/java/io/cheshire/core/manager/LifecycleManager.java`

Diagnose: Startup registered components on every initialize call, initialized all phases together
despite documented phase ordering, used a cached platform-thread pool, and shut that pool down
during shutdown, preventing restart.

Refactor: Build an immutable phase graph once, initialize phases sequentially while allowing
parallelism inside a phase on virtual threads, and clean up already-started components on failure.

Test: `LifecycleManagerTest`

## 3. API Contracts & Serialization

### Servlet error JSON escaping

Locate: `cheshire-server/src/main/java/io/cheshire/jetty/http/RestAdapterServlet.java`

Diagnose: Error responses were hand-formatted JSON strings. Quotes and newlines in messages could
emit invalid JSON and break clients.

Refactor: Serialize an immutable `ErrorResponse` record through the module ObjectMapper.

Test: `RestAdapterServletTest`

### JDBC query SPI recursion

Locate: `cheshire-source-provider-jdbc/src/main/java/io/cheshire/source/jdbc/SqlSourceProviderQuery.java`

Diagnose: The SPI method `parameters()` recursively called itself. Any query-engine path using the
interface method would stack overflow.

Refactor: Return the immutable `params` record component and defensively copy constructor maps.

Test: `SqlSourceProviderQueryTest`

## 4. Enterprise Resilience

### Jetty live-state and executor lifecycle

Locate: `cheshire-server/src/main/java/io/cheshire/jetty/JettyServerContainer.java`

Diagnose: `isRunning()` read a never-updated boolean, and the virtual-thread executor handed to
Jetty was not owned or shut down.

Refactor: Track lifecycle through `AtomicBoolean` plus `server.isRunning()` and shut down the owned
virtual-thread executor on stop.

Test: `ServerConfigTest`

### Test log volume

Locate: `cheshire-query-engine-calcite/src/test/resources/simplelogger.properties`

Diagnose: Calcite tests defaulted to DEBUG on stdout, generating extremely large test logs and
masking failures.

Refactor: Default tests to WARN, keep Cheshire at INFO, and emit logs on stderr.

Test: Full test run remains green with readable output.

## 5. Extension Blueprint

The next cohesive extension should be a query-context enrichment pipeline:

1. Add a `cheshire-context-spi` module with immutable `ContextDocument`, `ContextRetriever`, and
   `ContextRankingPolicy` records/interfaces.
2. Use the Elasticsearch source provider as the first retrieval backend for lexical search.
3. Add a vector provider later behind the same SPI for hybrid retrieval.
4. Expose retrieval as a pipeline preprocessor so REST and MCP capabilities can opt in without
   changing protocol code.
5. Add OpenTelemetry span attributes at the query-engine and source-provider boundaries, carrying
   request id, capability, action, source name, retry count, and timeout status.

This keeps search and context enrichment outside core orchestration while preserving the existing
capability, pipeline, and source-provider boundaries.
