# Cheshire Security Documentation

## Overview

The Cheshire framework implements comprehensive security using **Role-Based Access Control (RBAC)** and *
*Attribute-Based Access Control (ABAC)** through integration with [Cedar Policy](https://www.cedarpolicy.com/en). This
provides fine-grained authorization for **Capabilities** and **Resources**.

Security is **protocol-agnostic**: the same authentication and authorization modules work seamlessly across MCP, HTTP,
GraphQL, and streaming adapters. All modules are designed to be pluggable via **Java Service Provider Interface (SPI)**,
enabling dynamic discovery and replacement at runtime.

---

## Features

* **Pluggable Authentication** – Support for token-based authentication (extensible to OAuth, Basic Auth, custom
  providers)
* **RBAC/ABAC Authorization** – Fine-grained access control using Cedar Policy
* **Cedar Policy Integration** – External policy engine for flexible policy management
* **Thread-Local Security Context** – Secure principal propagation through synchronous and asynchronous request
  pipelines
* **Protocol-Agnostic** – Security works across MCP, HTTP, GraphQL, SSE, and other protocol adapters
* **SPI-Based Extensibility** – Authenticators, Authorizers, and Interceptors are pluggable modules loaded using
  `ServiceLoader`

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Protocol Adapters                        │
│              (MCP, HTTP, GraphQL, Streaming)               │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Authentication Interceptor                      │
│         (Extract credentials, validate tokens)              │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Authorization Interceptor                       │
│    (Build Cedar request, check permissions)                 │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                  RequestEngine                               │
│            (Execute Capability if authorized)               │
└─────────────────────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Cedar Policy Service                            │
│         (HTTP/REST - Policy evaluation)                    │
└─────────────────────────────────────────────────────────────┘
```

---

## Configuration

### Enabling Security

Security is disabled by default. To enable it, configure security in your `CheshireConfig`:

```java
import io.cheshire.core.config.CheshireConfig;
import io.cheshire.security.config.SecurityConfig;

CheshireConfig config = CheshireConfig.builder()
    .security(SecurityConfig.enabled()
        .cedar(SecurityConfig.CedarConfig.builder()
            .serviceUrl("http://localhost:8080")
            .connectTimeoutMs(5000)
            .readTimeoutMs(10000)
            .build())
        .authentication(SecurityConfig.AuthenticationConfig.builder()
            .type("token")
            .tokenHeader("Authorization")
            .tokenPrefix("Bearer ")
            .requireAuth(true)
            .build())
        .authorization(SecurityConfig.AuthorizationConfig.builder()
            .type(AuthorizerType.SIMPLE)  // Default: SIMPLE (no external service required)
            .defaultAction("execute")
            .allowOnServiceUnavailable(false)
            .build())
        .build())
    .build();
```

### YAML Configuration Example

```yaml
security:
  enabled: true
  cedar:
    serviceUrl: "http://localhost:8080"
    connectTimeoutMs: 5000
    readTimeoutMs: 10000
  authentication:
    type: "token"
    tokenHeader: "Authorization"
    tokenPrefix: "Bearer "
    requireAuth: true
  authorization:
    type: "SIMPLE"  # Options: CEDAR, SIMPLE, NONE
    defaultAction: "execute"
    allowOnServiceUnavailable: false
    simple:
      defaultBehavior: "ROLE_BASED"  # Options: ALLOW_ALL, DENY_ALL, ROLE_BASED
      adminRole: "admin"
      privilegedRoles: ["admin"]
      capabilityRoles:
        query_users: ["admin", "manager"]
        read_data: ["user", "admin"]
```

---

## Authentication

### Token-Based Authentication

The framework supports token-based authentication out of the box. Tokens can be provided via:

* **HTTP Headers**: `Authorization: Bearer <token>`
* **Environment Variables**: `CHESHIRE_MCP_AUTH_TOKEN`
* **Configuration**: MCP adapter `authToken` setting

### Custom Authenticators (SPI Modules)

All authenticators are **SPI modules** and loaded at runtime using Java `ServiceLoader`. You can implement custom
authentication by implementing the `Authenticator` interface:

```java
import io.cheshire.security.auth.Authenticator;
import io.cheshire.security.auth.AuthenticationContext;
import io.cheshire.security.Principal;
import java.util.concurrent.CompletableFuture;

public class CustomAuthenticator implements Authenticator {
    @Override
    public CompletableFuture<Principal> authenticate(AuthenticationContext context) {
        String token = context.getCredential("Authorization").orElse(null);
        Principal principal = Principal.builder()
            .id("user123")
            .type("User")
            .roles(Set.of("admin", "developer"))
            .attributes(Map.of("department", "engineering"))
            .build();
        return CompletableFuture.completedFuture(principal);
    }

    @Override
    public boolean canAuthenticate(AuthenticationContext context) {
        return context.hasCredential("Authorization");
    }
}
```

#### SPI Loading Example

```java
ServiceLoader<Authenticator> authenticators = ServiceLoader.load(Authenticator.class);
for (Authenticator auth : authenticators) {
    auth.authenticate(context);
}
```

### Registering Tokens

```java
import io.cheshire.security.auth.TokenAuthenticator;
import io.cheshire.security.auth.TokenAuthenticator.PrincipalInfo;

TokenAuthenticator authenticator = TokenAuthenticator.builder()
    .tokenHeader("Authorization")
    .tokenPrefix("Bearer ")
    .requireAuth(true)
    .build();

authenticator.registerToken("my-secret-token-123",
    PrincipalInfo.builder()
        .id("alice")
        .type("User")
        .roles(Set.of("admin"))
        .attributes(Map.of("department", "engineering"))
        .build());
```

---

## Authorization with Cedar Policy

### Cedar Policy Service

Cheshire integrates with Cedar Policy via HTTP service. The service evaluates requests and returns allow/deny decisions.

#### Endpoints

* `POST /authorize` - Evaluate authorization requests
* `GET /health` - Health check

#### Authorization Request Format

```json
{
  "principal": "User::alice",
  "action": "execute",
  "resource": "Capability::query_users",
  "context": {
    "ip": "192.168.1.1",
    "timestamp": "2024-01-01T00:00:00Z"
  }
}
```

#### Authorization Response Format

```json
{
  "decision": "Allow",
  "reason": "Policy permits access",
  "diagnostics": {}
}
```

### Entity Model

* **Principal** – Authenticated users or service accounts
* **Role** – Principal roles (e.g., `Role::admin`)
* **Capability** – Cheshire Capabilities (e.g., `Capability::query_users`)
* **Resource** – Cheshire resources (e.g., `Resource::database1`)

### Actions

* `execute` – Execute a Capability
* `read` – Read from a Resource
* `write` – Write to a Resource
* `query` – Query a Resource

---

## Policy Examples

### RBAC Policy Example

```cedar
permit(
    principal in Role::"admin",
    action == Action::"execute",
    resource in Capability
);
```

### ABAC Policy Example

```cedar
permit(
    principal,
    action == Action::"execute",
    resource in Capability
) when {
    principal.permissions.contains(action)
};
```

### Resource-Level Policy Example

```cedar
permit(
    principal,
    action,
    resource in Resource
) when {
    resource.owner == principal.id
};
```

### Capability-Specific Policy

```cedar
permit(
    principal == User::"alice",
    action == Action::"execute",
    resource == Capability::"query_users"
);
```

---

## Usage Examples

### Basic Setup with Security

```java
import io.cheshire.core.Cheshire;
import io.cheshire.core.config.CheshireConfig;
import io.cheshire.security.config.SecurityConfig;

SecurityConfig securityConfig = SecurityConfig.enabled()
    .cedar(SecurityConfig.CedarConfig.builder()
        .serviceUrl("http://localhost:8080")
        .build())
    .authentication(SecurityConfig.AuthenticationConfig.builder()
        .type("token")
        .requireAuth(true)
        .build())
    .build();

CheshireConfig config = CheshireConfig.builder()
    .security(securityConfig)
    .build();

Cheshire cheshire = Cheshire.builder()
    .config(config)
    .build();
```

### MCP Adapter with Authentication

```java
import io.cheshire.adapter.mcp.MCPProtocolAdapter;
import io.cheshire.adapter.mcp.MCPServerConfig;

MCPServerConfig mcpConfig = MCPServerConfig.builder()
    .name("secure-mcp-jetty")
    .version("1.0.0")
    .authToken("my-secret-token")
    .requireAuth(true)
    .build();

MCPProtocolAdapter mcpAdapter = MCPProtocolAdapter.builder()
    .config(mcpConfig)
    .build();

cheshire.adapters().register(mcpAdapter);
```

### HTTP Adapter with Authentication

```java
import io.cheshire.adapter.http.HTTPProtocolAdapter;

HTTPProtocolAdapter httpAdapter = HTTPProtocolAdapter.builder()
    .config(HTTPProtocolAdapter.HTTPAdapterConfig.builder()
        .host("0.0.0.0")
        .port(8080)
        .build())
    .build();

cheshire.adapters().register(httpAdapter);
```

Clients must include the authentication token in the `Authorization` header:

```bash
curl -H "Authorization: Bearer my-secret-token" \
     http://localhost:8080/capabilities/query_users
```

---

## Security Context

The `SecurityContext` class provides thread-local storage for the authenticated **Principal**. Context is propagated
automatically across synchronous and asynchronous pipelines.

```java
import io.cheshire.security.SecurityContext;
import io.cheshire.security.Principal;

// Get current principal
Principal principal = SecurityContext.getPrincipal();

// Check if authenticated
if (SecurityContext.isAuthenticated()) { ... }

// Execute with specific principal in async context
CompletableFuture.runAsync(() -> {
    SecurityContext.withPrincipal(principal, () -> {
        // Async execution with context
    });
});
```

---

## Audit Logging

All authorization decisions should be logged for auditing:

```json
{
  "timestamp": "2025-12-31T12:00:00Z",
  "principal": "User::alice",
  "action": "execute",
  "resource": "Capability::query_users",
  "decision": "Allow",
  "reason": "Policy permits access"
}
```

---

## Error Handling

### Authentication Errors

```java
try { ... }
catch (AuthenticationException e) {
    log.error("Authentication failed: {}", e.getMessage());
}
```

### Authorization Errors

```java
try { ... }
catch (AuthorizationException e) {
    log.error("Access denied: {}", e.getMessage());
}
```

---

## Best Practices

1. **Token Management**

    * Store securely, rotate regularly, use expiration
2. **Policy Design**

    * Use RBAC and ABAC, keep simple and test
3. **Cedar Policy Service**

    * HTTPS, monitor health, proper error handling
4. **Security Configuration**

    * `requireAuth: true`, `allowOnServiceUnavailable: false`
5. **Async Pipelines**

    * Propagate `SecurityContext` in async streams
6. **SPI Modules**

    * Use pluggable authenticators and authorizers for extensibility
7. **Audit Logging**

    * Log all decisions, include principal, action, and resource

---

## Troubleshooting

* **Authentication Not Working**: Check token format, registration, headers, logs
* **Authorization Denied**: Verify principal, policies, resource identifiers, Cedar service availability
* **Cedar Service Unavailable**: Verify URL, connectivity, timeouts, and logs

---

## Integration with Existing Systems

* **Custom Authentication Providers** – Implement `Authenticator`, extract credentials, return Principal
* **Custom Authorization** – Implement `Authorizer` and replace `CedarAuthorizer` in `RequestEngine`

---

## Security Considerations

* Use HTTPS, secure token storage, firewalls, monitoring, audit logs
* Connection pooling, caching, async support for performance

---

## References

* [Cedar Policy Documentation](https://www.cedarpolicy.com/en)
* [Cedar Policy Language Guide](https://docs.cedarpolicy.com/)
* [Cheshire Framework Documentation](../README.md)
