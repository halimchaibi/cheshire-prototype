# MCP vs REST — Paths, Actions, and How to Call Them

This document explains **how capability and action are expressed and invoked** when using **REST APIs** versus **MCP APIs**

---

## 1. Core Concepts

### Capability

A **capability** represents a domain or tenant boundary, for example:

* `artists`
* `catalog`
* `billing`

Capabilities are:

* isolated
* versioned independently
* deployed independently

---

### Action

An **action** is the operation being invoked:

* REST: HTTP method + path
* MCP: tool name

---

## 2. REST Model

### REST Routing Rule

```
/capability/action
```

### Example

```
POST /artists/create
```

* **Capability**: `artists`
* **Action**: `create`

### How REST Resolves Requests

1. Path prefix selects the capability
2. Path + HTTP method select the action
3. Dispatcher invokes the capability handler

### Example cURL

```bash
curl -X POST http://host:9000/artists/create \
  -H "Content-Type: application/json" \
  -d '{ "name": "Radiohead" }'
```

---

## 3. MCP Model

### MCP Routing Rule

```
/mcp/v1/{capability}/sse
```

* **Capability** is selected by the HTTP path
* **Action** is selected by the MCP tool name

There is **one MCP server per capability**.

---

### Example MCP Paths

| Capability | MCP Base Path   |
|------------|-----------------|
| artists    | /mcp/v1/artists |
| catalog    | /mcp/v1/catalog |
| billing    | /mcp/v1/billing |

---

## 4. MCP Session Lifecycle

### Step 1: Open SSE Stream

The client must open a **long-lived SSE connection**.

```bash
curl -N http://host:9000/mcp/v1/artists  \
  -H "Accept: text/event-stream" \
  -H "Mcp-Session-Id: <session-id>"
```

This binds the session to the **artists capability**.

---

### Step 2: Call a Tool (Action)

Tool calls are sent as **JSON-RPC 2.0 messages**.

```bash
curl -X POST http://host:9000/mcp/v1/artists \
  -H "Content-Type: application/json" \
  -H "Mcp-Session-Id: <session-id>" \
  -d '{
    "jsonrpc": "2.0",
    "id": "1",
    "method": "tools/call",
    "params": {
      "name": "create_artist",
      "arguments": {
        "name": "Radiohead"
      }
    }
  }'
```

* **Capability**: resolved by `/artists`
* **Action**: `create_artist`

---

## 5. Comparison Summary

| Aspect               | REST               | MCP          |
|----------------------|--------------------|--------------|
| Capability selection | URL path           | URL path     |
| Action selection     | HTTP method + path | Tool name    |
| Protocol             | HTTP               | JSON-RPC     |
| Session              | Stateless          | Stateful     |
| Routing layer        | Transport          | Server scope |

---

## 6. Design Rules

* One MCP server per capability
* One servlet context per capability
* Capability expressed in HTTP path
* Action expressed as MCP tool name

---

## 7. Mental Model

```
HTTP Path        → Capability
MCP Tool Name   → Action

Capability boundary = MCP server instance
```
---
