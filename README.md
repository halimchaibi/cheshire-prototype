<div align="center">
<pre>
    /\_/\  
   ( o.o ) 
    > ^ <
</pre>
</div>

<h1 align="center">Cheshire Framework</h1>

<p align="center">
  <strong>A modular Java 21+ framework for exposing resources as capabilities through multiple protocols</strong>
</p>

<p align="center">
⚠️ <strong>Prototype / Experimental Project</strong> ⚠️  
</p>

This framework is currently in **prototype stage**. While it compiles and demonstrates core ideas, **features may be incomplete or non-functional**. Use for experimentation, research, and development purposes only.


Cheshire is a framework that enables developers to expose various resources—databases, APIs, and services—as unified **capabilities** accessible through multiple protocols including REST, MCP (Model Context Protocol), and more.

The framework features a powerful **three-stage pipeline architecture**, **DSL-based query templates**, and **federated query processing** capabilities, making it ideal for building modern data-driven applications and LLM-powered agents.

> *"We're all mad here."* — The Cheshire Cat

Like its namesake, Cheshire appears wherever you need it, providing a consistent interface to your resources regardless of how they're accessed.

## ✨ Features

- 🔌 **Multi-Protocol Support** - REST API, MCP stdio, MCP streamable HTTP
- 🎯 **Capability-Driven** - Resources exposed as business-aligned capabilities
- 🔄 **Three-Stage Pipelines** - PreProcessor → Executor → PostProcessor
- 🗄️ **DSL Query Templates** - JSON-based SQL generation with parameter binding
- 🔧 **Modular Architecture** - SPI-based extensibility for custom implementations
- 🛡️ **Type Safe** - Leverages Java 21's modern features (records, sealed interfaces, pattern matching)
- ⚡ **High Performance** - Virtual Thread support, lock-free metrics, structured concurrency
- 🧩 **Extensible** - Plugin architecture via ServiceLoader for query engines and source providers
- 🧪 **Well Tested** - Comprehensive test coverage with reference implementations
- 📚 **Comprehensively Documented** - 80+ classes with production-ready Javadoc

## 🚀 Quick Start

### Prerequisites

- **Java 21+** (with preview features enabled)
- **Maven 3.8+**

### Installation

Add the Cheshire BOM to your `pom.xml`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.cheshire</groupId>
            <artifactId>cheshire-bom</artifactId>
            <version>1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Then add the modules you need:

```xml
<dependencies>
    <!-- Core framework -->
    <dependency>
        <groupId>io.cheshire</groupId>
        <artifactId>cheshire-core</artifactId>
    </dependency>
    
    <!-- Runtime container -->
    <dependency>
        <groupId>io.cheshire</groupId>
        <artifactId>cheshire-runtime</artifactId>
    </dependency>
    
    <!-- JDBC Query Engine -->
    <dependency>
        <groupId>io.cheshire</groupId>
        <artifactId>cheshire-query-engine-jdbc</artifactId>
    </dependency>
    
    <!-- JDBC Source Provider -->
    <dependency>
        <groupId>io.cheshire</groupId>
        <artifactId>cheshire-source-provider-jdbc</artifactId>
    </dependency>
    
    <!-- Server implementations (Jetty + Stdio) -->
    <dependency>
        <groupId>io.cheshire</groupId>
        <artifactId>cheshire-server</artifactId>
    </dependency>
</dependencies>
```

### Your First Application

Create a simple blog application with multi-protocol support:

```java
package io.blog;

import io.cheshire.core.CheshireBootstrap;
import io.cheshire.core.CheshireSession;
import io.cheshire.runtime.CheshireRuntime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BlogApp {
    public static void main(String[] args) {
        log.info("Starting Blog Application...");
        
        // Select configuration based on command-line argument
        String configFileName = selectConfigFromArgs(args);
        System.setProperty("cheshire.config", configFileName);
        
        try {
            // 1. Bootstrap the framework
            CheshireSession session = CheshireBootstrap
                    .fromClasspath("config")
                    .build();
            
            // 2. Create and start the runtime
            CheshireRuntime runtime = CheshireRuntime
                    .expose(session)
                    .start();
            
            // 3. Await termination
            runtime.awaitTermination();
            
        } catch (Exception e) {
            log.error("Fatal startup error", e);
            System.exit(1);
        }
    }
    
    private static String selectConfigFromArgs(String[] args) {
        // Configuration is now specified via --config flag
        // Handled by PicoCLI command-line parser
        // Default: blog-rest.yaml
        
        // For advanced usage, use PicoCLI @CommandLine.Option annotations:
        // @Option(names = {"-c", "--config"}, defaultValue = "blog-rest.yaml")
        // private String configFile;
        
        return "blog-rest.yaml"; // Default if no --config provided
    }
}
```

Run with different protocols:

```bash
# REST API on http://localhost:9000/api/v1/blog
java -jar blog-app.jar --config blog-rest.yaml

# MCP via stdio (for LLM agents and Claude Desktop)
java -jar blog-app.jar --config blog-mcp-stdio.yaml \
  --log-file /tmp/blog-mcp-stdio.log \
  --redirect-stderr

# MCP via HTTP on http://localhost:9000/mcp/v1
java -jar blog-app.jar --config blog-mcp-streamable-http.yaml
```

See the complete [blog-app example](https://github.com/halimchaibi/cheshire-blog-app/blob/main/README.md) for full source code, comprehensive documentation, and testing guides.

## 📚 Documentation

### Guides

- **[User Guide](docs/guides/user/)** - Getting started and common use cases
- **[Pipeline Configuration Guide](docs/guides/user/PIPELINE_CONFIGURATION_GUIDE.md)** - Complete guide for configuring pipelines with DSL
- **[Developer Guide](docs/guides/devloper/)** - Extending the framework
- **[SQL Template DSL Reference](docs/guides/user/SQL_TEMPLATE_DSL_REFERENCE.md)** - Complete DSL_QUERY specification

### Reference Documentation

- **[DSL-QUERY](docs/reference/DSL-QUERY.MD)** - Domain-neutral query template specification
- **[DSL Resolution](docs/reference/DSL_RESOLUTION.MD)** - How DSL templates are resolved
- **[Jetty Configuration](docs/reference/JETTY-CONFIGURATION.MD)** - HTTP server configuration
- **[MCP Integration](docs/reference/MCP0.17.0-JETTY.MD)** - Model Context Protocol setup
- **[Security](docs/reference/SECURITY.MD)** - Authentication and authorization

### Exploration Topics

- **[TODOs](docs/TODOs/)** - Research topics and architectural patterns under investigation
  - Performance optimizations (Bloom filters, off-heap streaming, probabilistic data structures)
  - Security patterns (RBAC/ABAC with Cedar Policy)
  - Query federation (Apache Calcite integration)
  - Reactive patterns (Project Reactor, context propagation)
  - Extensibility (SPI extensions, runtime compilation)

### Architecture

- **[Architecture Overview](docs/architecture/architecture.svg)** - Visual architecture diagram

Cheshire follows a modular, layered architecture with clear separation of concerns:

<div align="center">
  <img src="docs/architecture/architecture.svg" alt="Cheshire Framework Architecture" width="100%"/>
</div>

### Architecture Layers

The framework consists of seven distinct layers, each with well-defined responsibilities:

1. **Transport Layer** - Network I/O (Jetty HTTP/WebSocket, stdio, TCP)
2. **Protocol Layer** - Multi-protocol support (REST, MCP, WebSocket, GraphQL)
3. **Server Infrastructure** - Protocol adapters, dispatchers, server handles (Virtual Threads)
4. **Cheshire Core** - Session management, capability registry, orchestration
5. **Three-Stage Pipeline** - PreProcessor → Executor → PostProcessor
6. **Query Engines & Source Providers** - JDBC engine, Calcite integration, data source abstraction
7. **External Resources** - Databases, APIs, vector stores, data lakes

### Data Flow

**Request Flow (Top → Bottom, Red):**
- External Request → RequestEnvelope → SessionTask → MaterializedInput → SqlQuery → SQL/API Calls

**Response Flow (Bottom → Top, Green):**
- Data Rows → MapQueryResult → MaterializedOutput → TaskResult → ResponseEntity → Responses

### Key Concepts

#### 🎯 Capabilities

Business-aligned, self-contained domains that federate data sources and expose operations. Each capability defines:

- **Actions** - Invocable operations (exposed as MCP tools or REST endpoints)
- **Pipelines** - Three-stage processing flow for each action
- **Exposure** - Protocol configuration (REST, MCP stdio, MCP HTTP)
- **Transport** - Network configuration (port, host, threading)

#### 🔄 Three-Stage Pipelines

Every action is processed through three stages:

1. **PreProcessor** - Input validation and transformation
2. **Executor** - Core business logic (query execution, API calls)
3. **PostProcessor** - Output transformation and enrichment

#### 📝 DSL_QUERY

JSON-based query templates that generate SQL dynamically:

```json
{
  "operation": "SELECT",
  "source": {"table": "articles", "alias": "a"},
  "projection": [
    {"field": "a.id"},
    {"field": "a.title"},
    {"field": "a.content"}
  ],
  "filters": {
    "conditions": [
      {"field": "a.id", "op": "=", "param": "articleId"}
    ]
  }
}
```

See [SQL_TEMPLATE_DSL_REFERENCE.md](docs/guides/user/SQL_TEMPLATE_DSL_REFERENCE.md) for complete documentation.

## 📦 Modules

### Core Modules

| Module               | Description                                                 |
|----------------------|-------------------------------------------------------------|
| **cheshire-bom**     | Bill of Materials for dependency management                 |
| **cheshire-core**    | Core framework (Session, Managers, Capabilities, Pipelines) |
| **cheshire-runtime** | Operational container with structured concurrency           |
| **cheshire-common**  | Shared utilities, exceptions, and configuration             |

### SPI Modules

| Module                           | Description                                                           |
|----------------------------------|-----------------------------------------------------------------------|
| **cheshire-pipeline-spi**        | Pipeline processor interfaces (PreProcessor, Executor, PostProcessor) |
| **cheshire-query-engine-spi**    | Query engine abstraction and factory                                  |
| **cheshire-source-provider-spi** | Source provider abstraction and factory                               |

### Implementation Modules

| Module                            | Description                                          |
|-----------------------------------|------------------------------------------------------|
| **cheshire-query-engine-jdbc**    | Direct JDBC query execution with DSL_QUERY support   |
| **cheshire-query-engine-calcite** | Apache Calcite integration for federated queries     |
| **cheshire-source-provider-jdbc** | JDBC data source provider with connection management |
| **cheshire-server**               | Server implementations (Jetty HTTP, stdio)           |

### Security & Testing

| Module                   | Description                                           |
|--------------------------|-------------------------------------------------------|
| **cheshire-security**    | Authentication, authorization, and security utilities |
| **cheshire-test-common** | Common testing utilities and fixtures                 |

## 🎯 Use Cases

### 1. LLM Agent Data Access

Expose databases to LLM agents via MCP protocol:

```yaml
# config/blog-mcp-stdio.yaml
capabilities:
  blogmcpstdio:
    name: blog-mcp-stdio
    description: "Blog database via MCP stdio"
    
    exposure:
      type: MCP_STDIO
      binding: MCP_STDIO
      
    actions-specification-file: blog-actions.yaml
    pipelines-definition-file: blog-pipelines.yaml
```

**Exposed Actions** (automatically discovered by LLM agents):

- `create_author`, `update_author`, `delete_author`, `get_author`, `list_authors`
- `create_article`, `update_article`, `delete_article`, `get_article`, `list_articles`
- `search_articles`, `list_published_articles`
- `create_comment`, `delete_comment`, `list_comments_by_article`

### 2. Multi-Protocol API

Expose the same capability through multiple protocols:

```java
// Same actions accessible via:
// 1. REST API: http://localhost:9000/api/v1/blog
// 2. MCP HTTP: http://localhost:9000/mcp/v1
// 3. MCP stdio: java -jar app.jar --config blog-mcp-stdio.yaml
```

### 3. Dynamic Query Generation

Use DSL templates for type-safe, injectable SQL queries:

```json
{
  "operation": "SELECT",
  "source": {"table": "articles", "alias": "a"},
  "joins": [
    {
      "type": "LEFT",
      "table": "authors",
      "alias": "au",
      "on": [{"left": "a.author_id", "op": "=", "right": "au.id"}]
    }
  ],
  "filters": {
    "op": "AND",
    "conditions": [
      {"field": "a.status", "op": "=", "param": "status"},
      {"field": "a.published_at", "op": ">=", "param": "fromDate"}
    ]
  }
}
```

Parameters are safely bound at runtime:

```java
Map<String, Object> params = Map.of(
    "status", "published",
    "fromDate", LocalDateTime.now().minusDays(7)
);
```

### 4. Custom Pipeline Processors

Implement custom processing logic:

```java
@Slf4j
public class CustomInputProcessor implements PreProcessor {
    @Override
    public MaterializedInput process(MaterializedInput input) {
        // Validate, transform, enrich input
        var transformed = validateAndTransform(input);
        log.debug("Input processed: {}", transformed);
        return transformed;
    }
}

@Slf4j
public class CustomExecutor implements Executor {
    @Override
    public MaterializedOutput execute(MaterializedInput input, SessionTask task) {
        // Execute business logic
        var result = executeBusinessLogic(input, task);
        return MaterializedOutput.of(result);
    }
}
```

## 🛠️ Building from Source

### Prerequisites

- **Java 21+** (with preview features enabled)
- **No Maven installation required!** - Project includes Maven wrapper

### Clone and Build

```bash
# Clone the repository
git clone https://github.com/cheshire-framework/cheshire-framework.git
cd cheshire-framework/cheshire

# Build using Maven wrapper (no Maven installation needed!)
./mvnw clean install

# Or on Windows
mvnw.cmd clean install
```

### Build Options

```bash
# Full build with tests
./mvnw clean install

# Build without tests (faster)
./mvnw clean install -DskipTests

# Build with test coverage
./mvnw clean install jacoco:report

# Build individual module
cd cheshire-core
../mvnw clean install

# Clean all build artifacts
./mvnw clean

# Run tests only
./mvnw test

# Run integration tests
./mvnw verify
```

### Using System Maven (Optional)

If you have Maven 3.8+ installed, you can use it directly:

```bash
mvn clean install
```

### Maven Wrapper Details

The project includes Maven Wrapper (mvnw) which:
- ✅ Downloads correct Maven version (3.9.6) automatically
- ✅ Ensures consistent builds across environments
- ✅ No need to install Maven manually
- ✅ Works on Linux, macOS, and Windows

## 🧪 Running the Blog Application Example

### Blog Application - Reference Implementation

The blog application is a complete reference implementation demonstrating all Cheshire features:

**Features:**
- ✅ Full CRUD operations (Authors, Articles, Comments)
- ✅ Three protocol exposures (REST, MCP stdio, MCP HTTP)
- ✅ 15+ operations with comprehensive validation
- ✅ DSL-based query templates
- ✅ PostgreSQL with Docker or H2 in-memory database
- ✅ Complete testing guide with curl examples
- ✅ Claude Desktop integration for AI agents
- ✅ OpenAPI and Postman collections

### Quick Start

```bash
# Navigate to blog-app
cd ../cheshire-blog-app

# Build the application
mvn clean package

# Run with REST API (default)
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml
# Access at: http://localhost:9000/api/v1/blog

# Run with MCP stdio (for Claude Desktop)
java -jar target/blog-app-1.0-SNAPSHOT.jar \
  --config blog-mcp-stdio.yaml \
  --log-file /tmp/blog-mcp-stdio.log \
  --redirect-stderr

# Run with MCP HTTP (with streaming)
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-mcp-streamable-http.yaml
# Access at: http://localhost:9000/mcp/v1
```

### Using Maven Exec

```bash
# REST API
mvn exec:java -Dexec.mainClass="io.blog.BlogApp" \
  -Dexec.args="--config blog-rest.yaml"

# MCP stdio
mvn exec:java -Dexec.mainClass="io.blog.BlogApp" \
  -Dexec.args="--config blog-mcp-stdio.yaml --log-file /tmp/blog-mcp-stdio.log --redirect-stderr"

# MCP HTTP
mvn exec:java -Dexec.mainClass="io.blog.BlogApp" \
  -Dexec.args="--config blog-mcp-streamable-http.yaml"
```

### Database Setup

```bash
# Option 1: Use H2 in-memory (default - no setup needed)
# Pre-populated with test data (8 authors, 9 articles, 10 comments)

# Option 2: Use PostgreSQL with Docker
cd infra
docker-compose up -d
cd ..
java -jar target/blog-app-1.0-SNAPSHOT.jar --config blog-rest.yaml

# Option 3: Generate custom test data
cd infra/postgres
source venv/bin/activate
python populate_db.py --generate --authors 100 --articles 500 --comments 2000
cd ../..
```

### Testing

```bash
# Test REST API
curl "http://localhost:9000/api/v1/blog/list_authors?page=1&limit=10"

# Create author
curl "http://localhost:9000/api/v1/blog/create_author?username=demo_user&email=demo@example.com"

# Get statistics
curl "http://localhost:9000/api/v1/blog/stats_overview"
```

See [cheshire-blog-app/TESTING.md](https://github.com/halimchaibi/cheshire-blog-app/blob/main/TESTING.md) for comprehensive testing guide.

## 🧑‍💻 Development

### Project Structure

```
├── cheshire-prototype/              # Core framework
│   ├── cheshire-bom/                # Dependency management
│   ├── cheshire-core/               # Core framework (Session, Managers, Capabilities)
│   ├── cheshire-runtime/            # Runtime container with structured concurrency
│   ├── cheshire-pipeline-spi/       # Pipeline processor interfaces
│   ├── cheshire-query-engine-spi/   # Query engine interfaces
│   ├── cheshire-query-engine-jdbc/  # JDBC query implementation
│   ├── cheshire-query-engine-calcite/ # Calcite integration (federated queries)
│   ├── cheshire-source-provider-spi/ # Source provider interfaces
│   ├── cheshire-source-provider-jdbc/ # JDBC source implementation
│   ├── cheshire-server/             # Server implementations (Jetty, stdio)
│   ├── cheshire-common/             # Shared utilities and configuration
│   ├── cheshire-security/           # Security features
│   └── docs/                        # Framework documentation
│       ├── guides/user/             # User guides and tutorials
│       ├── guides/developer/        # Developer guides
│       ├── reference/               # Reference documentation
│       └── TODOs/            # Exploration topics and research
```

### Code Style

Cheshire follows **Scala-influenced functional Java** style:

- **Immutability by default** - All fields `final`, use records
- **No nulls** - Use `Optional<T>` everywhere
- **Declarative** - Prefer Streams API over imperative loops
- **Expressions over statements** - Use switch expressions, ternary operators
- **Pure functions** - Push side effects to boundaries

See [.cursorrules](.cursorrules) for complete style guide.

### Testing

```bash
# Run all tests
mvn test

# Run specific module tests
cd cheshire-core
mvn test

# Run integration tests
mvn verify

# Run with coverage
mvn clean test jacoco:report
```

## 📋 Roadmap
Current progress, upcoming features, and bug fixes: [GitHub Project Board](https://github.com/users/halimchaibi/projects/2).

### ✅ Completed (v1.0)

- TBF

### 🚧 In Progress

- TBF

### 📋 Planned

- TBF

---

## 🌟 Reference Application

The **[Blog Application](https://github.com/halimchaibi/cheshire-blog-app/blob/main/README.md)** serves as the primary reference implementation, featuring:

- Complete CRUD operations for Authors, Articles, and Comments
- Multi-protocol support (REST, MCP stdio, MCP HTTP)
- Comprehensive testing guide with 1,800+ lines of documentation
- Database setup with PostgreSQL Docker and H2 in-memory options
- Test data generation scripts for performance testing
- Claude Desktop integration examples
- OpenAPI 3.0 and Postman collections
- Production-ready pipeline configurations

See [cheshire-blog-app/README.md](https://github.com/halimchaibi/cheshire-blog-app/blob/main/README.md) for complete documentation.

## 📞 Support

- **Documentation**: [docs/](docs/)

---
## 🛠 Development Methodology
**This project was built using a Human-in-the-Loop AI workflow, leveraging LLMs as a force-multiplier for development velocity**

---

## License

This project is licensed under the [PolyForm Noncommercial License 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0/).

See the [LICENSE](LICENSE) file for the full license text.

---

<div align="center">

**Created by [Halim Chaibi](https://github.com/halimchaibi)**

</div>

