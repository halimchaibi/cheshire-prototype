# Jetty Multi-Protocol Server Configuration

## Overview

This document describes the configuration for a single Jetty server instance that exposes multiple protocols (MCP, REST,
GraphQL, WebSocket) on a single port with different security and execution models per endpoint.

## Architecture Pattern

**Single Port, Multiple Protocols**

```
Port 9000 (HTTPS)
├── /mcp/v1      → MCP JSON-RPC (Async, API Key Auth)
├── /api/v1      → REST API (Sync, OAuth2)
├── /graphql     → GraphQL (Async, JWT)
└── /ws/v1       → WebSocket (Async, Session Auth)
```

### Key Concepts

- **Server**: Single Jetty instance listening on one port
- **Handler**: Routes requests to different contexts based on path
- **ServletContextHandler**: Provides servlet container environment for specific path prefix
- **ServletHolder**: Manages servlet lifecycle and configuration
- **Servlet**: Contains your business logic

### Hierarchy

```
Server (Port 9000)
│
└── HandlerList (Router)
    ├── ServletContextHandler (/mcp/v1)
    │   └── ServletHolder → McpServlet (business logic)
    ├── ServletContextHandler (/api/v1)
    │   └── ServletHolder → RestServlet (business logic)
    ├── ServletContextHandler (/graphql)
    │   └── ServletHolder → GraphQLServlet (business logic)
    └── ServletContextHandler (/ws/v1)
        └── WebSocketHandler (business logic)
```

## Configuration Structure

### Shared Configuration (Server-Level)

These settings apply to **all** handlers and protocols:

| Setting      | Scope  | Description                                           |
|--------------|--------|-------------------------------------------------------|
| `port`       | Server | Single port for all protocols                         |
| `host`       | Server | Bind address (0.0.0.0 = all interfaces)               |
| `threadPool` | Server | Shared thread pool for all contexts                   |
| `ssl`        | Server | SSL/TLS configuration (all or nothing)                |
| `http`       | Server | HTTP connector settings                               |
| `security`   | Server | Network-level security (IP filtering, DOS protection) |

### Handler-Specific Configuration

These settings are **unique per endpoint**:

| Setting                   | Scope   | Description                                           |
|---------------------------|---------|-------------------------------------------------------|
| `path`                    | Handler | Unique URL path                                       |
| `execution`               | Handler | Sync vs Async execution mode                          |
| `asyncTimeout`            | Handler | Timeout for async operations                          |
| `security.authentication` | Handler | Authentication method (API Key, OAuth2, JWT, Session) |
| `security.authorization`  | Handler | Role-based access control                             |
| `security.cors`           | Handler | CORS policy                                           |
| `rateLimit`               | Handler | Request rate limiting                                 |

## Complete Configuration Example

```yaml
# --------------------------------------------------------------------------
# Protocol / Transport Configuration
# Defines enabled transports and their parameters.
# --------------------------------------------------------------------------
exposures:
  # === MCP HANDLER - STREAMING JSON-RPC ===
  mcp-streaming:
    enabled: true
    binding: "mcp-jsonrpc"
    execution: "async"              # Async for streaming responses
    config:
      path: "/mcp/v1"               # Unique path
      contextPath: "/mcp/v1"
      displayName: "MCP Service"

      # Servlet-specific
      asyncTimeout: 300000          # 5 min for long-running streams
      multipartConfig:
        enabled: false

      # Business logic config
      maxRequestSize: 10485760      # 10MB
      allowedOrigins: ["*"]
      rateLimit:
        requestsPerMinute: 100

      # Security - API Key Authentication
      security:
        authentication:
          type: "api-key"
          headerName: "X-API-Key"
          required: true

        authorization:
          enabled: true
          roles: ["admin", "service"]

        cors:
          enabled: false            # Internal API, no CORS

        csrf:
          enabled: true

  # === REST API HANDLER - STANDARD HTTP ===
  rest-api:
    enabled: true
    binding: "http-json"
    execution: "sync"               # Synchronous request/response
    config:
      path: "/api/v1"

      # REST-specific features
      asyncTimeout: 30000           # 30s timeout
      enableSwagger: true           # OpenAPI documentation
      corsEnabled: true

      # Business rules
      maxRequestSize: 5242880       # 5MB
      rateLimit:
        requestsPerMinute: 1000     # Higher limit for public API

      # Security - OAuth2 Authentication
      security:
        authentication:
          type: "oauth2"
          tokenEndpoint: "https://auth.example.com"
          required: true

        authorization:
          enabled: true
          roles: ["user", "premium"]

        cors:
          enabled: true             # Public API needs CORS
          allowedOrigins: ["https://app.example.com"]
          allowedMethods: ["GET", "POST"]
          allowCredentials: true

  # === GRAPHQL HANDLER - QUERY AGGREGATION ===
  graphql-api:
    enabled: true
    binding: "http-graphql"
    execution: "async"              # Async for subscriptions
    config:
      path: "/graphql"

      # Security - JWT Authentication
      security:
        authentication:
          type: "jwt"
          secret: ${JWT_SECRET}
          issuer: "example.com"

        authorization:
          enabled: true
          fieldLevelSecurity: true  # GraphQL field-level auth

  # === WEBSOCKET HANDLER - BIDIRECTIONAL ===
  websocket-events:
    enabled: true
    binding: "websocket-json"
    execution: "async"
    config:
      path: "/ws/v1"

      # WebSocket-specific
      maxIdleTime: 300000           # 5 min idle timeout
      maxTextMessageSize: 65536     # 64KB
      maxBinaryMessageSize: 65536
      subProtocols: ["chat-v1"]

      # Connection management
      maxConnections: 10000
      pingInterval: 30000           # 30s keepalive

      # Security - Session-based Authentication
      security:
        authentication:
          type: "session"
          sessionTimeout: 3600      # 1 hour

        originCheck:
          enabled: true
          allowedOrigins: ["https://app.example.com"]

# --------------------------------------------------------------------------
# Transport Configuration (Shared Settings)
# --------------------------------------------------------------------------
transports:
  jetty:
    # === NETWORK CONFIGURATION (SHARED) ===
    port: 9000                      # Single port for all protocols
    host: 0.0.0.0                   # Bind to all interfaces

    # === THREAD POOL (SHARED) ===
    threadPool:
      minThreads: 8                 # Minimum available threads
      maxThreads: 200               # Maximum concurrent requests
      idleTimeout: 60000            # Thread idle timeout (ms)

    # === HTTP CONFIGURATION (SHARED) ===
    http:
      requestHeaderSize: 8192       # 8KB max header size
      responseHeaderSize: 8192
      outputBufferSize: 32768       # 32KB buffer
      requestTimeout: 30000         # 30s global timeout

    # === SSL/TLS CONFIGURATION (SHARED) ===
    ssl:
      enabled: true                 # Applies to ALL handlers
      protocol: TLSv1.3
      keystorePath: /path/to/keystore.jks
      keystorePassword: ${KEYSTORE_PASS}
      truststorePath: /path/to/truststore.jks

      # Cipher suites (applies to all)
      includeCipherSuites:
        - TLS_AES_256_GCM_SHA384
        - TLS_AES_128_GCM_SHA256
      excludeCipherSuites:
        - TLS_RSA_.*

      # Mutual TLS (if enabled, affects all handlers)
      needClientAuth: false
      wantClientAuth: false

    # === NETWORK SECURITY (SHARED) ===
    security:
      # IP filtering (applies to all handlers)
      allowedHosts:
        - 10.0.0.0/8                # Private networks
        - 192.168.0.0/16
      blockedHosts:
        - 192.168.1.100             # Blocked IPs

      # DOS protection (shared)
      maxConnections: 10000         # Total connections
      maxConnectionsPerIP: 100      # Per-IP limit
```

## Configuration Reference

### Shared Settings (Transport Level)

#### Network

| Parameter | Type    | Default | Description                   |
|-----------|---------|---------|-------------------------------|
| `port`    | integer | 9000    | Port number for all protocols |
| `host`    | string  | 0.0.0.0 | Bind address                  |

#### Thread Pool

| Parameter     | Type    | Default | Description                |
|---------------|---------|---------|----------------------------|
| `minThreads`  | integer | 8       | Minimum threads in pool    |
| `maxThreads`  | integer | 200     | Maximum concurrent threads |
| `idleTimeout` | integer | 60000   | Thread idle timeout (ms)   |

#### HTTP

| Parameter            | Type    | Default | Description                      |
|----------------------|---------|---------|----------------------------------|
| `requestHeaderSize`  | integer | 8192    | Max request header size (bytes)  |
| `responseHeaderSize` | integer | 8192    | Max response header size (bytes) |
| `outputBufferSize`   | integer | 32768   | Output buffer size (bytes)       |
| `requestTimeout`     | integer | 30000   | Global request timeout (ms)      |

#### SSL/TLS

| Parameter             | Type    | Required | Description                     |
|-----------------------|---------|----------|---------------------------------|
| `enabled`             | boolean | Yes      | Enable SSL/TLS (applies to all) |
| `protocol`            | string  | No       | TLS protocol version            |
| `keystorePath`        | string  | Yes      | Path to keystore file           |
| `keystorePassword`    | string  | Yes      | Keystore password               |
| `truststorePath`      | string  | No       | Path to truststore              |
| `includeCipherSuites` | array   | No       | Allowed cipher suites           |
| `excludeCipherSuites` | array   | No       | Blocked cipher suites           |
| `needClientAuth`      | boolean | No       | Require client certificates     |
| `wantClientAuth`      | boolean | No       | Request client certificates     |

#### Security

| Parameter             | Type    | Description                  |
|-----------------------|---------|------------------------------|
| `allowedHosts`        | array   | IP ranges allowed to connect |
| `blockedHosts`        | array   | IP addresses blocked         |
| `maxConnections`      | integer | Total connection limit       |
| `maxConnectionsPerIP` | integer | Per-IP connection limit      |

### Handler-Specific Settings (Exposure Level)

#### Basic Configuration

| Parameter     | Type    | Description               |
|---------------|---------|---------------------------|
| `enabled`     | boolean | Enable this handler       |
| `binding`     | string  | Protocol binding type     |
| `execution`   | string  | `sync` or `async`         |
| `path`        | string  | URL path for this handler |
| `displayName` | string  | Human-readable name       |

#### Execution

| Parameter        | Type    | Description                       |
|------------------|---------|-----------------------------------|
| `asyncTimeout`   | integer | Timeout for async operations (ms) |
| `maxRequestSize` | integer | Maximum request body size (bytes) |

#### Rate Limiting

| Parameter           | Type    | Description                 |
|---------------------|---------|-----------------------------|
| `requestsPerMinute` | integer | Requests allowed per minute |

#### Security - Authentication

| Parameter        | Type    | Values                                | Description                           |
|------------------|---------|---------------------------------------|---------------------------------------|
| `type`           | string  | `api-key`, `oauth2`, `jwt`, `session` | Authentication method                 |
| `headerName`     | string  | -                                     | Header name for credentials (api-key) |
| `tokenEndpoint`  | string  | -                                     | OAuth2 token endpoint                 |
| `secret`         | string  | -                                     | JWT secret key                        |
| `issuer`         | string  | -                                     | JWT issuer                            |
| `sessionTimeout` | integer | -                                     | Session timeout (seconds)             |
| `required`       | boolean | -                                     | Require authentication                |

#### Security - Authorization

| Parameter            | Type    | Description              |
|----------------------|---------|--------------------------|
| `enabled`            | boolean | Enable authorization     |
| `roles`              | array   | Allowed roles            |
| `fieldLevelSecurity` | boolean | GraphQL field-level auth |

#### Security - CORS

| Parameter          | Type    | Description          |
|--------------------|---------|----------------------|
| `enabled`          | boolean | Enable CORS          |
| `allowedOrigins`   | array   | Allowed origins      |
| `allowedMethods`   | array   | Allowed HTTP methods |
| `allowCredentials` | boolean | Allow credentials    |

#### WebSocket-Specific

| Parameter              | Type    | Description                     |
|------------------------|---------|---------------------------------|
| `maxIdleTime`          | integer | Connection idle timeout (ms)    |
| `maxTextMessageSize`   | integer | Max text message size (bytes)   |
| `maxBinaryMessageSize` | integer | Max binary message size (bytes) |
| `subProtocols`         | array   | Supported sub-protocols         |
| `maxConnections`       | integer | Max concurrent connections      |
| `pingInterval`         | integer | Keepalive ping interval (ms)    |

## Security Model

### Layered Security Architecture

```
┌─────────────────────────────────────────────────┐
│         Network Layer (Shared)                   │
│  • IP Filtering                                  │
│  • DOS Protection                                │
│  • Connection Limits                             │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│         Transport Layer (Shared)                 │
│  • SSL/TLS                                       │
│  • Certificate Validation                        │
│  • Cipher Suites                                 │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│         Handler Layer (Specific)                 │
│  • Authentication (API Key/OAuth/JWT/Session)    │
│  • Authorization (Roles)                         │
│  • CORS Policy                                   │
│  • Rate Limiting                                 │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│         Application Layer (Specific)             │
│  • Input Validation                              │
│  • Business Logic                                │
│  • Data Access Control                           │
└─────────────────────────────────────────────────┘
```

### Authentication Methods by Handler

| Handler   | Path       | Auth Type | Use Case            |
|-----------|------------|-----------|---------------------|
| MCP       | `/mcp/v1`  | API Key   | Internal services   |
| REST      | `/api/v1`  | OAuth2    | Public API          |
| GraphQL   | `/graphql` | JWT       | Partner integration |
| WebSocket | `/ws/v1`   | Session   | Web app clients     |

### Security Decision Matrix

| Question                     | Single Port          | Multiple Ports |
|------------------------------|----------------------|----------------|
| Different SSL certificates?  | ❌ Not supported      | ✅ Supported    |
| Different TLS versions?      | ❌ Not supported      | ✅ Supported    |
| mTLS for some, not others?   | ❌ Not supported      | ✅ Supported    |
| Different auth per endpoint? | ✅ Supported          | ✅ Supported    |
| Different CORS per endpoint? | ✅ Supported          | ✅ Supported    |
| Different rate limits?       | ✅ Supported          | ✅ Supported    |
| Network isolation?           | ⚠️ Use reverse proxy | ✅ Supported    |

## Best Practices

### 1. Execution Mode Selection

- **Sync (`execution: sync`)**: Traditional request/response, short operations
    - REST CRUD operations
    - Simple queries
    - Fast computations

- **Async (`execution: async`)**: Long-running, streaming, real-time
    - MCP streaming responses
    - GraphQL subscriptions
    - WebSocket connections
    - SSE (Server-Sent Events)

### 2. Timeout Configuration

```yaml
# Short timeout for quick operations
rest-api:
  config:
    asyncTimeout: 30000  # 30s

# Long timeout for streaming
mcp-streaming:
  config:
    asyncTimeout: 300000  # 5min
```

### 3. Rate Limiting Strategy

```yaml
# Conservative for internal APIs
mcp-streaming:
  config:
    rateLimit:
      requestsPerMinute: 100

# Higher for public APIs
rest-api:
  config:
    rateLimit:
      requestsPerMinute: 1000
```

### 4. CORS Configuration

```yaml
# Disable for internal APIs
mcp-streaming:
  config:
    security:
      cors:
        enabled: false

# Enable only for public APIs
rest-api:
  config:
    security:
      cors:
        enabled: true
        allowedOrigins: ["https://app.example.com"]
        allowCredentials: true
```

### 5. Authentication Selection

| Use Case            | Recommended Auth | Reason              |
|---------------------|------------------|---------------------|
| Internal services   | API Key          | Simple, fast        |
| Public web API      | OAuth2           | Industry standard   |
| Partner integration | JWT              | Stateless, portable |
| Web app clients     | Session          | Browser-native      |

## Environment Variables

Use environment variable substitution for sensitive values:

```yaml
ssl:
  keystorePassword: ${KEYSTORE_PASS}

security:
  authentication:
    secret: ${JWT_SECRET}
```

Set in your environment:

```bash
export KEYSTORE_PASS="your-secure-password"
export JWT_SECRET="your-jwt-secret"
```

## Monitoring & Logging

### Recommended Metrics

**Server-Level (Shared)**:

- Active connections
- Thread pool utilization
- Request rate (total)
- Error rate (total)
- SSL handshake failures

**Handler-Level (Specific)**:

- Request rate per endpoint
- Response time per endpoint
- Error rate per endpoint
- Authentication failures
- Rate limit hits

### Logging Configuration

```yaml
transports:
  jetty:
    logging:
      level: INFO
      accessLog: logs/access.log
      errorLog: logs/error.log

exposures:
  mcp-streaming:
    config:
      logging:
        level: DEBUG  # Handler-specific log level
```

## Troubleshooting

### Common Issues

#### Issue: SSL handshake failures

**Cause**: TLS version or cipher suite mismatch
**Solution**: Check `includeCipherSuites` and `protocol` settings

#### Issue: WebSocket connection drops

**Cause**: `maxIdleTime` too short
**Solution**: Increase `maxIdleTime` and implement `pingInterval`

#### Issue: High thread pool exhaustion

**Cause**: Too many synchronous operations
**Solution**: Increase `maxThreads` or change to `async` execution

#### Issue: CORS errors on public API

**Cause**: Missing or incorrect CORS configuration
**Solution**: Enable CORS and set `allowedOrigins` correctly

#### Issue: Rate limit false positives

**Cause**: Shared IP addresses (NAT, proxy)
**Solution**: Implement token-based rate limiting instead of IP-based

## Migration Guide

### From Multiple Ports to Single Port

**Before** (Multiple Servers):

```yaml
mcp-server:
  port: 9000

rest-server:
  port: 9001

graphql-server:
  port: 9002
```

**After** (Single Server):

```yaml
transports:
  jetty:
    port: 9000  # Single port

exposures:
  mcp-streaming:
    config:
      path: "/mcp/v1"

  rest-api:
    config:
      path: "/api/v1"

  graphql-api:
    config:
      path: "/graphql"
```

### Client URL Updates

| Old URL                                | New URL                                |
|----------------------------------------|----------------------------------------|
| `https://api.example.com:9000/mcp`     | `https://api.example.com:9000/mcp/v1`  |
| `https://api.example.com:9001/api`     | `https://api.example.com:9000/api/v1`  |
| `https://api.example.com:9002/graphql` | `https://api.example.com:9000/graphql` |

## Further Reading

- [Jetty Documentation](https://www.eclipse.org/jetty/documentation/)
- [MCP Protocol Specification](https://modelcontextprotocol.io/)
- [OAuth 2.0 Best Practices](https://oauth.net/2/)
- [JWT.io](https://jwt.io/)
- [WebSocket Protocol RFC 6455](https://tools.ietf.org/html/rfc6455)

---

**Document Version**: 1.0
**Last Updated**: 2025-01-01
**Maintainer**: Platform Team
