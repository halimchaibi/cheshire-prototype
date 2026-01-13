# 1. Definition

> ### Capability:

A Capability is a self-contained, business-aligned domain that federates one or more pluggable data sources and exposes a coherent, discoverable set of operations via a common path (e.g., /chinook). Capabilities are designed for reusability and can be accessed through one or more pluggable API patterns (REST, MCP, GraphQL, etc.), allowing flexible integration while maintaining a consistent business interface.

Key Properties:

* Non-executable by itself
* Owns configuration, not behavior
* Groups actions
* Defines boundaries: data sources, query engine, pipelines, exposure model

What it is:

* A feature boundary
* A deployment/runtime unit
* A contract namespace

What it is **NOT**:

* Not an API endpoint
* Not an MCP action
* Not a command

Examples: chinook, northwind, sales


> ### Action:
An action is a concrete invocable operation exposed by a capability that performs a specific query, command, or computation over the underlying data and pipelines: the thing a client actually calls to perform work or retrieve data.

What it is:

* A unit of execution
* A function-like operation
* A contracted behavior

What it maps to:

* REST → endpoint or resource operation
* GraphQL → field or mutation
* MCP → tool/action, resource, prompt
* WS → stream producer

Examples: list_artists, top_artists

# 2. Base Path Design

The base path pattern is designed to supports multiple protocols (REST, MCP, GraphQL, Streaming) while remaining scalable.
The base path adopt a **Protocol-First Versioning** strategy, it acts as the primary "dispatcher" before the request even hits the backend.

### 1. Base Path hierarchy:

The hierarchy should move from **Capability**  **exposure**  **Version**.

```text
/{capability}/{protocol}/{version}/{action}

```

| Segment             | Purpose                      | Examples                               |
|---------------------|------------------------------|----------------------------------------|
| **`/{capability}`** | The "Tenant" or domain.      | `chinook`, `northwind`, `sales`        |
| **`/{exposure}`**   | The Interface Pattern.       | `rest`, `mcp`, `gql`, `ws` (streaming) |
| **`/{version}`**    | API Lifecycle management.    | `v1`, `v2`, `v3-beta`                  |
| **`/{action}`**     | The action (when applicable) | `artists`, `tracks`                    |

---

### 2. Protocol-Specific Layouts

Here is how the specific protocols would look under this design:

#### A. REST (Resource Oriented)

Standard HTTP Verbs (GET, POST, PUT, DELETE).

> `GET /chinook/rest/v1/artists?filter[Name]=Aerosmith`

#### B. MCP (Model Context Protocol / Tooling)

MCP is often used for LLM orchestration. Since it usually relies on JSON-RPC over HTTP/SSE or stdio, the path identifies the "Server" instance.

> `POST /chinook/mcp/v1/tools/list_artists`

#### C. GraphQL (Single Endpoint)

GraphQL typically doesn't use resource paths; it uses a single POST entry point.

> `POST /chinook/gql/v1`

#### D. Streaming / SSE (Real-time)

Streaming invoice updates or track plays.

> `GET /chinook/stream/v1/invoices`

---

## 3. Implementation Logic (The "Plumbing" Perspective)

#### a. The ***TransportServer***

When the ***TransportServer*** receives a request like `HttpServletRequest` or a `CallToolRequest`

``` java
//Pseudo-code
    service(
            HttpServletRequest req,
            HttpServletResponse resp
    ) {

        // ...
        RequestEnvelope envelope = adapter.toRequestEnvelope(req);
        //...
    }
```

Invokes the ***ProtocolAdapter*** to build a ***RequestEnvelope***

---

#### b. The ***ProtocolAdapter***

The ***ProtocolAdapter*** will analyze the request to extract the **capability** to wake up and the **action** to invoke and build a protocol-agnostic Canonical ***RequestEnvelope***

```java
// Pseudo-code
// Build the RequestEnvelope a protocol-agnostic Canonical Envelope that the framework knows.
RequestEnvelope toRequestEnvelope(Request request) {

    String capability = identifyCapability(request);   // chinook
    String action = identifyAction(request);   // artists (if applicable) 
    Object payload = extractPayload(request)
    Object parameters = extractParameters(request)
// .... headers, query ...

    return RequestEnvelope.of(capability, ...)
}

```

Returns the ***RequestEnvelope*** to the ***TransportServer*** <br>

---

#### c. The ***TransportServer***

``` java
//Pseudo-code
    service(
            HttpServletRequest req,
            HttpServletResponse resp
    ) {
        // ...
        ResponseEntity response = handler.handle(envelope);
        //...
    }
```

The ***TransportServer*** sends the  ***RequestEnvelope*** to the ***RequestHandler***

---

#### d. The ***RequestHandler***

Build a ***RequestContext*** that holds identity context, security context ...

```java
// Pseudo-code
ResponsEntitye respone = RequestHandler.handle(RequestEnvelope request) {

// Contract validation(Boundary-Level), Authentication ...
// Build a RequestContext that holds identity context, security context ...

// Hand over to the dispatcher
ResponseEntity response = dispatcher.apply(request, ctx);
}
```

The ***RequestHandler*** invokes the ***Dispatcher*** to dispatch the request

---

#### e. The ***Dispatcher***

The ***Dispatcher***  Builds a ***SessionTask*** , coordinate with the ***RuntimeSession*** for various checks to prepare the execution

``` java
// Pseudo-code
// Checks If the RunetimeSession is ready, if the capability/action exists...
// Check if the capability/action is active and available...
// Can queue the execution ... and inform the handler to notify the caller
// Build a SessionTask and Invokes the RuntimeSession for execution
ResponseEntity dispatch(RequestEnvelope envelope, ctx) {
    TaskResult result = session.execute(TaskBuilder.from(envelope).build(), ctx);
}
```

The ***Dispatcher*** dispatches the task to the ***RuntimeSession***

---

#### f. The ***RuntimeSession***:

***RuntimeSession*** resolve the capability and action ... and builds a ***CanonicalInput***. He owns the execution lifecycle

``` java
// Pseudo-code
    TaskResult execute(String capability, String action, SessionTask sessionTask, ctx) {
    
        // Build a CanonicalInput and invokes the pipeline for execution
        CanonicalOutput output = pipeline.apply(CanonicalInput, ctx);

        return toTaskResult(CanonicalOutput, ctx)
    }
    
```

The ***RuntimeSession*** invokes the ***PipelineExecutor** for exeution

---

#### g. The ***PipelineExecutor***

Here’s a **detailed expansion of section g. “PipelineExecutor”** with **subsections** matching your sequence diagram and flow, formatted for your existing documentation:

---

#### g. The ***PipelineExecutor***

The ***PipelineExecutor*** orchestrates the main execution steps for a task. It is responsible for transforming the canonical input into a canonical output, invoking the query engine, and applying any post-processing logic. The executor is **protocol-agnostic** and does not depend on the transport layer.

```java
CanonicalOutput apply(CanonicalInput input, ExecutionContext ctx) {
    // Invokes three Step(Processors)

    // 1) PreProcessors
    // -----------------
    // - Validate the input
    // - Resolve dynamic parameters
    // - Transform input into a QueryRequest-ready format
    CanonicalInput preprocessedInput = preProcessors.apply(input, ctx);

    // 2) Executor
    // -----------------
    // - Build the QueryRequest from the preprocessed input
    // - Invoke the QueryEngine
    //    - Parse the SQL / domain-neutral query
    //    - Build logical query plan
    //    - Optimize the plan
    //    - Execute queries against Source Providers
    //    - Federate results asynchronously
    QueryRequest queryRequest = executor.buildQueryRequest(preprocessedInput);
    QueryResult rawResult = executor.executeQuery(queryRequest, ctx);

    // - Validate results and convert to CanonicalOutput
    CanonicalOutput stepOutput = executor.mapQueryResult(rawResult, ctx);

    // 3) PostProcessors
    // -----------------
    // - Apply formatting, enrichment, or additional transformations
    CanonicalOutput processedOutput = postProcessors.apply(stepOutput, ctx);

    // Return final CanonicalOutput
    return processedOutput;
}
```

---

### **g.1 PreProcessors**

Responsibilities:

* Validate required fields and types.
* Resolve dynamic parameters (from `RequestEnvelope` or session context).
* Transform domain-neutral input into a structure compatible with the Executor.
* Chain multiple pre-processing steps in a configurable pipeline.

```java
CanonicalInput apply(CanonicalInput input, ExecutionContext ctx) {
    // Example: parameter binding
    input.resolveParams(ctx.getParameters());
    input.validate();
    input.transform(); // e.g., projection mapping
    return input;
}
```

---

### **g.2 Executor**

Responsibilities:

* Construct the `QueryRequest` (SQL or domain-neutral template).
* Invoke the **QueryEngine** (Calcite) for parsing, planning, optimization, and execution.
* Parallel fetch from multiple Source Providers if needed.
* Map `QueryResult` to `CanonicalOutput`.

```java
QueryResult executeQuery(QueryRequest request, ExecutionContext ctx) {
    // 1. Parse the query
    RelNode logicalPlan = calcite.parse(request.getQuery());

    // 2. Optimize the plan
    RelNode optimizedPlan = calcite.optimize(logicalPlan);

    // 3. Execute against data sources (async if multiple)
    CompletableFuture<List<Row>> resultsFuture = sourceProvider.execute(optimizedPlan);

    // 4. Merge/federate results asynchronously
    List<Row> mergedResults = resultsFuture.join();

    return new QueryResult(mergedResults);
}
```

* **Supports streaming**: if the source provider returns iterators or streams, the executor can yield rows incrementally instead of collecting all at once.

---

### **g.3 PostProcessors**

Responsibilities:

* Format results (e.g., map database fields to API names).
* Enrich data if needed (e.g., attach metadata, annotations).
* Apply additional transformations or filters.
* Chain multiple post-processing steps.

```java
CanonicalOutput apply(CanonicalOutput output, ExecutionContext ctx) {
    // Example: enrich each row with computed fields
    for (Row row : output.rows()) {
        row.put("computedField", computeValue(row));
    }
    return output;
}
```

* PostProcessors are executed **after the QueryEngine finishes**, ensuring the raw data is converted into the **final canonical output**.

---

If you want, I can **also draft a `g.5` subsection specifically for streaming large datasets**, showing **row-by-row streaming through the pipeline**, which you can integrate with MCP or chunked HTTP.

Do you want me to do that next?

Return a ***CanonicalOutput*** to the ***RuntimeSession***

---

#### h. The ***RuntimeSession***

The *RuntimeSession* build a TaskResult from CanonicalOutput

``` java
// Pseudo-code
    TaskResult TaskResultFeom(CanonicalOutput , ctx) {
        // Build a TaskResult
        TaskResult result  = TaskResult.from(output)
        return result;
    }
```

The ***RuntimeSession*** returns the ***TaskResult*** to the ***Dispatcher***

---

#### i. The ***Dispatcher***

The ***Dispatcher*** builds a canonical protocol-agnostic ***ResponseEntity***

``` java
// Pseudo-code
    public ResponseEntity dispatch(RequestEnvelope envelope) {
        try {
            TaskResult result = session.execute(TaskBuilder.from(envelope).build());
            
            //...

            return switch (result) {
                case TaskResult.Success s -> ResponseEntity.ok(s.output());
                case TaskResult.Failure f -> ResponseEntity.error(f.status(), f.cause());
            };
        } catch (Exception e) {
            return ResponseEntity.error(ResponseEntity.Status.EXECUTION_FAILED, e);
        }
    }
```

---

#### j. The *RequestHandler*

As of now only passthrough the ***ResponseEntity*** to The ***ServerTransport***

``` java
// Pseudo-code
    ResponseEntity handle(RequestEnvelope request) {
            ResponseEntity response = dispatcher.apply(request);
            
            //...

            return response;
    }
```

---

#### j. The ***ServerTransport***:

The ***ServerTransport*** invokes the The ***ProtocolAdapter*** to build the final response

``` java
// Pseudo-code
    protected void service(
            HttpServletRequest req,
            HttpServletResponse resp
    ) {
            //...
            public Map<String, Object> resfromProcessingResult(ResponseEntity result) 
            //..

    }

```

---

#### k. The ***ProtocolAdapter***

The ***ProtocolAdapter*** builds the final response for the ***ServerTransport***

``` java
// Pseudo-code
    public Map<String, Object> fromProcessingResult(ResponseEntity result) {
        // For REST, we likely return a Map that the Handler converts to JSON
        return switch (result) {
            case ResponseEntity.Success success -> Map.of(
                    "success", true,
                    "data", success.data()
            );
            case ResponseEntity.Failure failure -> Map.of(
                    "success", false,
                    "error", Map.of(
                            "type", failure.status().name(),
                            "message", failure.error().getMessage()
                    )
            );
        };
    }

```

---

#### l. The ***ServerTransport***

The ***ServerTransport*** Returns the final response to the client

``` java
// Pseudo-code
    protected void service(
            HttpServletRequest req,
            HttpServletResponse resp
    ) {
            //...
            public Map<String, Object> resp = fromProcessingResult(ResponseEntity result)

            writeResponse(resp, response);

    }

```

---

## 4. Why this design works

### 1. Logical Isolation

Each protocol handler (`rest`, `mcp`, `gql`, `ws`) is isolated behind its own request-handling layer.

* You can update or replace the **REST engine** without touching MCP or GraphQL.
* Protocol-specific concerns stay contained.
* No cross-protocol regressions.

This enforces a clean **adapter boundary**.

---

### 2. Security

Authentication and authorization can be **protocol-specific** while enforcing the same action-level policy model.

Examples:

* REST → JWT / OAuth2
* MCP → tool tokens / caller identity
* GraphQL → session-based or API keys

The request handler:

* Extracts identity
* Enforces access at *capability/action* level
* Passes a normalized security context downstream

No protocol leaks into business logic.

---

### 3. Multi-Tenancy / Domain Expansion

Leading with `/{capability}` makes the system **natively multi-domain**.

* `/chinook/rest/v1/...`
* `/adventureworks/rest/v1/...`
* `/sales/mcp/v1/...`

New domains are added by **configuration**, not framework changes.
Each capability remains:

* Independently versioned
* Independently deployed (if needed)
* Independently governed

---

### 4. Action Reuse and Packaging (Missing Point)

Because **actions are canonical and protocol-agnostic**, you can **package and re-expose existing actions without duplication**.

#### What this enables

* Create **packages** (or bundles) of actions:

    * Per protocol
    * Per client type
    * Per exposure surface
* Reuse the same action across:

    * REST
    * MCP
    * GraphQL
    * Internal composition

#### Examples

* A `public` package exposing read-only actions
* An `admin` package exposing mutating actions
* An `mcp-lite` package exposing only LLM-safe actions
* A `graphql-analytics` package aggregating analytical actions

No new pipelines. No forked logic. Just different exposure maps.

---

### 5. Controlled Composition (Derived Capabilities)

Because actions are first-class, you can:

* Compose **new capabilities** from existing actions
* Create higher-level APIs without touching data sources

Example:

* `chinook` exposes core actions
* `chinook-analytics` packages selected actions + derived ones
* Both reuse the same execution layer

This keeps growth **additive**, not entropic.

---

## Summary

> Capabilities define *what exists*, actions define *what can be executed*, request handlers define *how it is exposed*, and packages define *which actions are visible where*.



---

# 5. Reference - Chinook Rest API – Exhaustive Query & URL

> **Assumption**: This document describes a canonical REST API layered on top of the classic **Chinook sample database** (Artists, Albums, Tracks, Customers, Invoices, etc.).
> If your implementation differs (GraphQL, gRPC, custom endpoints), this still gives a *complete functional surface* you can map to a unified internal query model.

---

## 1. Global API Conventions

### Base URL

```
/api/v1
```

### Common Query Parameters (All Collection Endpoints)

| Parameter           | Type   | Description                             |
|---------------------|--------|-----------------------------------------|
| `limit`             | int    | Page size                               |
| `offset`            | int    | Pagination offset                       |
| `sort`              | string | Field name (e.g. `name`, `price`)       |
| `order`             | enum   | `asc`                                   | `desc`                          |
| `fields`            | csv    | Sparse field selection                  |
| `expand`            | csv    | Join/expand related entities            |
| `filter[field]`     | string | Equality filter                         |
| `filter[field][op]` | string | Advanced ops (`gt`, `lt`, `like`, `in`) |
| `q`                 | string | Full-text search (when supported)       |

### Standard Operators

| Operator  | Meaning          |
|-----------|------------------|
| `eq`      | equals           |
| `ne`      | not equals       |
| `gt`      | greater than     |
| `gte`     | greater or equal |
| `lt`      | less than        |
| `lte`     | less or equal    |
| `like`    | SQL LIKE         |
| `in`      | value list       |
| `between` | range            |

### Range Queries (Explicit)

Range is a **first‑class concept** and should not be treated as a special case of filters.

#### Supported Range Forms

**1. Closed / Open Numeric Ranges**

```
GET /tracks?range[UnitPrice]=0.99,1.99
GET /tracks?range[Milliseconds]=[60000,300000)
```

| Syntax  | Meaning             |
|---------|---------------------|
| `a,b`   | inclusive‑inclusive |
| `[a,b)` | inclusive‑exclusive |
| `(a,b]` | exclusive‑inclusive |
| `(a,b)` | exclusive‑exclusive |

**2. Date / Time Ranges**

```
GET /invoices?range[InvoiceDate]=2023-01-01,2023-12-31
```

ISO‑8601 only. Timezone explicit or UTC by default.

**3. Half‑Open Ranges**

```
GET /tracks?range[UnitPrice]=,1.00
GET /tracks?range[Milliseconds]=60000,
```

**4. Multi‑Field Ranges**

```
GET /tracks?range[UnitPrice]=0.5,1.5&range[Milliseconds]=60000,240000
```

---

## 2. Artists

### Endpoints

```
GET    /artists
GET    /artists/{artistId}
POST   /artists
PUT    /artists/{artistId}
DELETE /artists/{artistId}
```

### Query Examples

```
GET /artists?sort=Name&order=asc
GET /artists?filter[Name][like]=Metal
GET /artists?expand=albums
```

---

## 3. Albums

### Endpoints

```
GET    /albums
GET    /albums/{albumId}
POST   /albums
PUT    /albums/{albumId}
DELETE /albums/{albumId}
```

### Supported Filters

| Field      | Ops    |
|------------|--------|
| `ArtistId` | eq, in |
| `Title`    | like   |

### Examples

```
GET /albums?filter[ArtistId]=12
GET /albums?expand=artist,tracks
```

---

## 4. Tracks

### Endpoints

```
GET    /tracks
GET    /tracks/{trackId}
POST   /tracks
PUT    /tracks/{trackId}
DELETE /tracks/{trackId}
```

### Supported Filters

| Field          | Ops             |
|----------------|-----------------|
| `AlbumId`      | eq, in          |
| `GenreId`      | eq              |
| `MediaTypeId`  | eq              |
| `Milliseconds` | gt, lt, between |
| `UnitPrice`    | gt, lt          |

### Examples

```
GET /tracks?filter[GenreId]=1
GET /tracks?filter[UnitPrice][lt]=1.0
GET /tracks?expand=album,artist,genre
```

---

## 5. Genres

### Endpoints

```
GET    /genres
GET    /genres/{genreId}
```

### Examples

```
GET /genres
GET /genres?expand=tracks
```

---

## 6. Media Types

### Endpoints

```
GET /media-types
GET /media-types/{mediaTypeId}
```

---

## 7. Customers

### Endpoints

```
GET    /customers
GET    /customers/{customerId}
POST   /customers
PUT    /customers/{customerId}
DELETE /customers/{customerId}
```

### Supported Filters

| Field     | Ops  |
|-----------|------|
| `Country` | eq   |
| `City`    | eq   |
| `Email`   | like |

### Examples

```
GET /customers?filter[Country]=France
GET /customers?expand=invoices
```

---

## 8. Employees

### Endpoints

```
GET /employees
GET /employees/{employeeId}
```

### Relationships

* ReportsTo (self-reference)

### Examples

```
GET /employees?expand=manager,subordinates
```

---

## 9. Invoices

### Endpoints

```
GET    /invoices
GET    /invoices/{invoiceId}
POST   /invoices
```

### Supported Filters

| Field         | Ops     |
|---------------|---------|
| `CustomerId`  | eq      |
| `InvoiceDate` | between |
| `Total`       | gt, lt  |

### Examples

```
GET /invoices?filter[CustomerId]=5
GET /invoices?expand=lines,customer
```

---

## 10. Invoice Lines

### Endpoints

```
GET /invoice-lines
GET /invoice-lines/{lineId}
```

### Filters

| Field       | Ops |
|-------------|-----|
| `InvoiceId` | eq  |
| `TrackId`   | eq  |

---

## 11. Playlists

### Endpoints

```
GET    /playlists
GET    /playlists/{playlistId}
POST   /playlists
DELETE /playlists/{playlistId}
```

### Relationships

* Many-to-many with Tracks

### Examples

```
GET /playlists?expand=tracks
```

---

## 12. Cross-Entity Queries (Derived Views)

### Top Tracks by Sales

```
GET /analytics/top-tracks?limit=10
```

### Revenue by Country

```
GET /analytics/revenue-by-country
```

### Customer Lifetime Value

```
GET /analytics/customers/{id}/clv
```

---

## 13. Unified Internal Query Model (What This Implies)

From this API surface, **all queries reduce to**:

```
Entity
+ Filters (field, operator, value)
+ Pagination (limit, offset)
+ Sorting (field, direction)
+ Projection (fields)
+ Expansion (joins)
+ Aggregation (group, metric)
```

TODO: 
- Nested queries.
- To be reconsidered if Apache Calcite will the only supported engine.


