# Cheshire Framework - Architecture Documentation

## Architecture Diagram

The main architecture diagram (`architecture.svg`) provides a comprehensive visual representation of the Cheshire Framework's layered architecture.

### Viewing the Diagram

The diagram is embedded in the main [README.md](../../README.md) and can be viewed:

- **In GitHub**: Displays inline automatically
- **Locally**: Open `architecture.svg` in any modern web browser
- **In IDE**: Most IDEs (VS Code, IntelliJ IDEA) can preview SVG files

### Diagram Features

#### Bidirectional Data Flow

The diagram shows **bidirectional arrows** between layers, illustrating:

**Request Flow (Red ↓)** - Top to Bottom:

1. **Requests** - Client requests → Protocol Layer
2. **RequestEnvelope** - Server → Core
3. **SessionTask** - Core → Pipeline
4. **MaterializedInput** - Pipeline → Query Engines
5. **SqlQuery/QueryRequest** - Engines → Source Providers
6. **SQL/API Calls** - Providers → External Resources

**Response Flow (Green ↑)** - Bottom to Top:

1. **Data Rows** - External Resources → Providers
2. **MapQueryResult** - Providers → Engines
3. **MaterializedOutput** - Engines → Pipeline
4. **TaskResult** - Pipeline → Core
5. **ResponseEntity** - Core → Server
6. **Responses** - Server → Client

### Seven Architecture Layers

#### 1. Transport Layer

**Purpose**: Network I/O  
**Components**: Jetty (HTTP/WebSocket), Stdio/Pipes, Remote ROP (TCP)  
**Responsibility**: Handle low-level network communication

#### 2. Protocol Layer (Exposures)

**Purpose**: Multi-protocol support  
**Components**:

- **realtime-ws** - WebSocket JSON binding (Async, Stateful)
- **MCP Protocol** - MCP-RPC binding (Async)
- **REST API** - HTTP-JSON binding (Sync)
- **GraphQL** - GraphQL binding (Async)

**Responsibility**: Protocol-specific request/response handling

#### 3. Server Infrastructure

**Purpose**: Request translation and routing  
**Components**:

- Protocol Adapters (REST, MCP)
- Dispatchers (Sealed interfaces)
- Server Handles (Jetty/Stdio containers)

**Technology**: Virtual Thread Pools for massive concurrency  
**Responsibility**: Envelope wrapping, protocol translation

#### 4. Cheshire Core

**Purpose**: Session management and orchestration  
**Components**:

- CheshireSession (Runtime hub)
- Capability Registry
- Managers: Config, Lifecycle, Capability, SourceProvider, QueryEngine

**Pattern**: Singleton, long-lived  
**Responsibility**: Central orchestration and registry management

#### 5. Three-Stage Pipeline

**Purpose**: Request processing workflow  
**Stages**:

1. **PreProcessor** - Validate and transform input
2. **Executor** - Execute business logic
3. **PostProcessor** - Format and enrich output

**Pattern**: Stream-based reduction  
**Extensibility**: SPI-based custom implementations

#### 6. Query Engines (SPI)

**Purpose**: Query execution and optimization  
**Implementations**:

- **JDBC Query Engine** - Direct SQL execution, DSL_QUERY templates
- **Calcite Engine** - Federated queries, query optimization

**Extensibility**: Custom engines via ServiceLoader  
**Responsibility**: Parse, plan, optimize, execute queries

#### 7. Source Providers (SPI)

**Purpose**: Data source abstraction  
**Implementations**:

- **JDBC Provider** - SQL databases (PostgreSQL, MySQL, H2, etc.)
- **Vector Provider** - Vector stores (ChromaDB, Pinecone)
- **API Provider** - REST/GraphQL APIs
- **Apache Spark** - Big data clusters

**Extensibility**: Custom providers via ServiceLoader  
**Responsibility**: Connection management, query execution

### External Resources

**Examples**: PostgreSQL, MySQL, Snowflake, ChromaDB, Pinecone, ElasticSearch, REST APIs, Spark Clusters  
**Access**: Through Source Providers  
**Management**: Connection pooling, health checks, retry logic

## Key Design Patterns

### 1. Layered Architecture

Clear separation of concerns with unidirectional dependencies (top → bottom).

### 2. Service Provider Interface (SPI)

Plugin architecture for:

- Query Engines
- Source Providers
- Pipeline Processors

### 3. Three-Stage Pipeline

Consistent processing flow: PreProcessor → Executor → PostProcessor

### 4. Sealed Interfaces

Java 21 sealed interfaces for:

- CheshireDispatcher (HTTP, MCP implementations)
- ResponseEntity (Success, Failure variants)
- TaskResult (Success, Failure variants)

### 5. Virtual Threads

Java 21 Virtual Threads for:

- Massive concurrency (10,000+ concurrent requests)
- Structured concurrency in CheshireRuntime
- Efficient thread pooling in Jetty

### 6. Immutability

Records for all data carriers:

- Config objects
- Request/Response objects
- Input/Output objects

## Data Flow Example

### Request Journey: REST API Call

```
1. Client: HTTP GET /api/v1/articles/123
   ↓
2. Jetty Server: Receives HTTP request
   ↓
3. RestProtocolAdapter: Converts to RequestEnvelope
   ↓
4. CheshireDispatcher: Routes to capability
   ↓
5. CheshireSession: Creates SessionTask
   ↓
6. Pipeline.PreProcessor: Validates input
   ↓
7. Pipeline.Executor: Builds SQL query from DSL template
   ↓
8. JdbcQueryEngine: Executes SQL query
   ↓
9. JdbcDataSourceProvider: Queries PostgreSQL
   ↓
10. PostgreSQL: Returns data rows
    ↑
11. Provider: Converts to MapQueryResult
    ↑
12. Executor: Wraps in MaterializedOutput
    ↑
13. PostProcessor: Formats response
    ↑
14. Session: Returns TaskResult
    ↑
15. Dispatcher: Creates ResponseEntity
    ↑
16. Adapter: Converts to HTTP response
    ↑
17. Jetty: Sends HTTP 200 OK with JSON
    ↑
18. Client: Receives response
```

## Architecture Principles

### 1. Dependency Inversion

High-level modules (Core, Runtime) depend on abstractions (SPIs), not implementations.

### 2. Open/Closed Principle

Framework is open for extension (via SPI) but closed for modification.

### 3. Single Responsibility

Each layer has one clear responsibility.

### 4. Interface Segregation

Small, focused SPI interfaces (QueryEngine, SourceProvider, PreProcessor, etc.).

### 5. Separation of Concerns

Protocol handling, business logic, and data access are cleanly separated.

## Technology Stack

### Core

- **Java 21** - Modern Java with preview features
- **Records** - Immutable data carriers
- **Sealed Interfaces** - Restricted hierarchies
- **Virtual Threads** - Massive concurrency
- **Pattern Matching** - Type-safe operations

### Server

- **Eclipse Jetty** - High-performance HTTP server
- **MCP Java SDK** - Model Context Protocol
- **Virtual Thread Pools** - Efficient concurrency

### Query Processing

- **Apache Calcite** - Federated query engine
- **JDBC** - Database connectivity
- **DSL_QUERY** - JSON-based query templates

### Utilities

- **Jackson** - JSON processing
- **SLF4J** - Logging facade
- **Lombok** - Boilerplate reduction

## Performance Characteristics

### Concurrency

- **Virtual Threads**: Handle 10,000+ concurrent requests
- **Lock-Free Metrics**: Zero-contention performance tracking
- **Structured Concurrency**: Automatic cleanup and cancellation

### Throughput

- **Async Pipelines**: Non-blocking execution
- **Connection Pooling**: Efficient database connections
- **Query Optimization**: Calcite-based query planning

### Latency

- **Direct SQL**: Minimal overhead for JDBC queries
- **Stream Processing**: Zero-copy transformations where possible
- **Lazy Initialization**: Components created on-demand

## Monitoring & Observability

### RuntimeHealth

- State machine tracking (NEW → STARTING → RUNNING → STOPPING → STOPPED)
- Component health checks
- Event history

### RuntimeMetrics

- Request counts and timing
- Error rates
- Memory usage
- Component-specific metrics

### Logging

- SLF4J throughout
- Structured logging support
- Debug/trace for development

## Future Enhancements

### Planned Features

- Additional protocol adapters (gRPC, AMQP)
- More query engines (MongoDB, Neo4j)
- Distributed tracing integration
- Advanced caching layer
- Admin UI for monitoring

### Under Consideration

- GraphQL federation
- Stream processing integration
- Cloud-native deployment templates
- Kubernetes operators

---

**Maintained By**: Cheshire Framework Team  
**Last Updated**: January 2026  
**Version**: 1.0.0

