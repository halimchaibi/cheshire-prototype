# Cheshire Framework - Quick Start Guide

Get up and running with Cheshire in 5 minutes!

## 🚀 Installation

### 1. Prerequisites

```bash
# Check Java version (must be 21+)
java --version
```

**Required:** Java 21 or higher  
**Not Required:** Maven (project includes wrapper!)

### 2. Clone Repository

```bash
git clone https://github.com/halimchaibi/cheshire-prototype.git
cd cheshire-prototype
```

### 3. Build Project

**Linux/macOS:**
```bash
./mvnw clean install
```

**Windows:**
```cmd
mvnw.cmd clean install
```

That's it! The Maven wrapper will download Maven automatically if needed.

## 📝 Create Your First Application

### 1. Add Dependencies

```xml
<dependencies>
    <!-- Core Framework -->
    <dependency>
        <groupId>io.cheshire</groupId>
        <artifactId>cheshire-core</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- Runtime -->
    <dependency>
        <groupId>io.cheshire</groupId>
        <artifactId>cheshire-runtime</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- JDBC Support -->
    <dependency>
        <groupId>io.cheshire</groupId>
        <artifactId>cheshire-query-engine-jdbc</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    
    <dependency>
        <groupId>io.cheshire</groupId>
        <artifactId>cheshire-source-provider-jdbc</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    
    <!-- Server -->
    <dependency>
        <groupId>io.cheshire</groupId>
        <artifactId>cheshire-server</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### 2. Create Main Application

```java
package com.example.myapp;

import io.cheshire.core.CheshireBootstrap;
import io.cheshire.core.CheshireSession;
import io.cheshire.runtime.CheshireRuntime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyApp {
    public static void main(String[] args) {
        log.info("Starting application...");
        
        // 1. Bootstrap framework
        CheshireSession session = CheshireBootstrap
                .fromClasspath("config")
                .build();
        
        // 2. Start runtime
        CheshireRuntime runtime = CheshireRuntime
                .expose(session)
                .start();
        
        // 3. Await termination
        runtime.awaitTermination();
    }
}
```

### 3. Create Configuration

**src/main/resources/config/application.yaml:**

```yaml
application:
  name: my-app
  version: 1.0.0
  description: My Cheshire Application

sources:
  my-db:
    factory: io.cheshire.source.jdbc.JdbcDataSourceProviderFactory
    type: jdbc
    description: My Database
    config:
      connection:
        driver: org.h2.Driver
        url: jdbc:h2:mem:mydb
        username: sa
        password: ""

query-engines:
  jdbc-engine:
    engine: io.cheshire.query.engine.jdbc.JdbcQueryEngineFactory
    sources: [my-db]

capabilities:
  my-capability:
    name: my-capability
    description: My first capability
    
    exposure:
      type: REST_HTTP
      binding: HTTP_JSON
      config:
        base-path: /api/v1
        async-timeout: 30000
    
    transport:
      port: 8080
      host: 0.0.0.0
    
    actions-specification-file: actions.yaml
    pipelines-definition-file: pipelines.yaml
```

### 4. Run Application

```bash
# Build
./mvnw clean package

# Run
java -jar target/my-app-1.0-SNAPSHOT.jar
```

## 🎯 Common Use Cases

### REST API

```bash
# Start with REST API
java -jar my-app.jar --rest

# Access API
curl http://localhost:8080/api/v1/users
```

### MCP for LLM Agents

```bash
# Start with MCP stdio
java -jar my-app.jar --mcp-stdio

# LLM agents can now interact via stdin/stdout
```

### Multi-Protocol

Same application, multiple protocols:
- REST API: `http://localhost:8080/api/v1`
- MCP HTTP: `http://localhost:8080/mcp/v1`
- MCP stdio: via stdin/stdout

## 📚 Next Steps

### Learn More

- [Full Documentation](../../../README.md)
- [Architecture Guide](docs/architecture/)
- [DSL Query Reference](docs/guides/SQL_TEMPLATE_DSL_REFERENCE.md)
- [Building Guide](../devloper/BUILDING.md)

### Examples

- [Blog Application](../blog-app/) - Complete reference implementation
- [Chinook Example](../cheshire-reference-impl/) - Music store database

### Extend the Framework

- [Custom Query Engines](docs/guides/devloper/)
- [Custom Source Providers](docs/guides/devloper/)
- [Custom Pipeline Processors](docs/guides/devloper/)

## 🆘 Troubleshooting

### Build Issues

```bash
# Clean build
./mvnw clean install -U

# Skip tests
./mvnw clean install -DskipTests
```

### Java Version

```bash
# Check version
java --version

# Must be Java 21+
```

### Maven Wrapper

```bash
# Make executable (Linux/macOS)
chmod +x mvnw

# Test wrapper
./mvnw --version
```

## 💡 Tips

1. **Use Maven Wrapper** - No need to install Maven
2. **Start with Examples** - blog-app is fully functional
3. **Read DSL Guide** - Understand query templates
4. **Check Architecture Tests** - Learn framework patterns

## 🤝 Getting Help

- **Documentation**: [docs/](docs/)

---

**Ready to build?** Run `./mvnw clean install` and you're good to go! 🚀

