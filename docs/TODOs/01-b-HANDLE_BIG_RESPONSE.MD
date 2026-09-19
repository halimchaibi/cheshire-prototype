# Exploration: Handling Large Data Objects in the Processing Pipeline

## Problem Statement

Our current pipeline architecture processes requests through multiple layers:

```
TransportServer → ProtocolAdapter → RequestHandler → Dispatcher
    → RuntimeSession → PipelineExecutor → QueryEngine → SourceProviders
```

**Current Implementation:**

- All data is passed as `Map<String, Object>` for payload/results
- Metadata is passed as `Map<String, Object>`
- Simple, uniform interface across all components

**Problems:**

1. **Memory Exhaustion**: Large result sets (millions of rows) are fully materialized in memory
2. **Latency**: User must wait for entire dataset to be fetched before receiving first byte
3. **Blob Handling**: Binary data (images, documents) are loaded entirely into heap
4. **GC Pressure**: Large collections cause frequent garbage collection pauses
5. **Scalability**: Cannot handle datasets larger than available heap space

**Example Scenarios:**

- Query returning 5M rows × 50 columns = ~500MB-2GB in memory
- Blob retrieval of 100MB PDF file blocks thread until complete
- Federated query across 10 sources materializes all results before merging

---

## Decision Drivers

* **Memory Efficiency**: Avoid loading entire datasets into memory
* **Latency**: Stream first results to client as quickly as possible
* **Backward Compatibility**: Minimize breaking changes to existing pipeline
* **Complexity**: Keep implementation maintainable
* **Flexibility**: Support both small and large datasets efficiently
* **Production Readiness**: Solution must be stable for first production release

---

## Options Considered

### Option 1: Keep Current Approach (Baseline)

**Description:** Continue materializing all data in `Map<String, Object>`.

**Code Sample:**

```java
// Current implementation
public class CanonicalOutput {
    private Map<String, Object> data;
    private Map<String, Object> metadata;

    public Map<String, Object> getData() {
        return data; // All rows materialized
    }
}

// Usage in PipelineExecutor
public CanonicalOutput apply(CanonicalInput input, ExecutionContext ctx) {
    QueryResult rawResult = executor.executeQuery(queryRequest, ctx);

    // Problem: Materializes ALL rows in memory
    List<Map<String, Object>> allRows = new ArrayList<>();
    while (rawResult.hasNext()) {
        allRows.add(rawResult.next()); // Loads everything
    }

    return new CanonicalOutput(Map.of("rows", allRows), metadata);
}
```

**Pros:**

- ✅ Simple, no changes needed
- ✅ Easy to debug and reason about
- ✅ Works for small datasets

**Cons:**

- ❌ OutOfMemoryError for large datasets
- ❌ High latency (wait for complete fetch)
- ❌ Inefficient for blobs
- ❌ Not production-ready for real-world data volumes

**Decision:** ❌ **Rejected** - Not suitable for production use cases

---

### Option 2: Pure Streaming with Iterator Pattern

**Description:** Replace `Map<String, Object>` with `Iterator<Map<String, Object>>` for data, keeping metadata as map.

**Code Sample:**

```java
// New canonical structures
public class CanonicalOutput {
    private Iterator<Map<String, Object>> dataStream;
    private Map<String, Object> metadata;

    public Iterator<Map<String, Object>> getDataStream() {
        return dataStream;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}

// PipelineExecutor
public CanonicalOutput apply(CanonicalInput input, ExecutionContext ctx) {
    QueryResult rawResult = executor.executeQuery(queryRequest, ctx);

    // Wrap QueryEngine iterator (lazy evaluation)
    Iterator<Map<String, Object>> streamingIterator = new Iterator<>() {
        @Override
        public boolean hasNext() {
            return rawResult.hasNext();
        }

        @Override
        public Map<String, Object> next() {
            // Fetch row on-demand
            return rawResult.next();
        }
    };

    return new CanonicalOutput(streamingIterator, metadata);
}

// RuntimeSession - streaming pass-through
public TaskResult execute(SessionTask task, ExecutionContext ctx) {
    CanonicalOutput output = pipeline.apply(canonicalInput, ctx);

    // Don't materialize - pass iterator directly
    return TaskResult.success(output.getDataStream(), output.getMetadata());
}

// TransportServer - HTTP streaming example
protected void service(HttpServletRequest req, HttpServletResponse resp) {
    RequestEnvelope envelope = adapter.toRequestEnvelope(req);
    ResponseEntity response = handler.handle(envelope);

    if (response.isSuccess() && response.hasStream()) {
        resp.setContentType("application/json");
        resp.setHeader("Transfer-Encoding", "chunked");

        PrintWriter writer = resp.getWriter();
        writer.write("{\"data\":[");

        Iterator<Map<String, Object>> stream = response.getDataStream();
        boolean first = true;

        while (stream.hasNext()) {
            if (!first) writer.write(",");
            writer.write(toJson(stream.next())); // Stream row-by-row
            writer.flush(); // Push to client immediately
            first = false;
        }

        writer.write("],\"metadata\":");
        writer.write(toJson(response.getMetadata()));
        writer.write("}");
    }
}

// MCP Protocol Adapter - streaming with list_changed notifications
public void streamToMcp(Iterator<Map<String, Object>> stream, McpSession session) {
    List<Map<String, Object>> batch = new ArrayList<>(100);

    while (stream.hasNext()) {
        batch.add(stream.next());

        if (batch.size() >= 100) {
            session.sendNotification("resources/list_changed", Map.of("batch", batch));
            batch.clear();
        }
    }

    if (!batch.isEmpty()) {
        session.sendNotification("resources/list_changed", Map.of("batch", batch));
    }
}
```

**Pros:**

- ✅ Constant memory usage regardless of dataset size
- ✅ Low latency - stream starts immediately
- ✅ Natural fit for SQL cursors and streaming sources
- ✅ Simple abstraction - just Iterator
- ✅ Composable with Java Streams API

**Cons:**

- ❌ Iterator is single-pass (cannot replay)
- ❌ No error recovery mid-stream
- ❌ Harder to implement some PostProcessors (e.g., sorting requires materialization)
- ❌ Breaking change to all pipeline components
- ❌ Metadata must be known upfront (before streaming data)

**Decision:** ✅ **Candidate** - Strong option for streaming

---

### Option 3: Hybrid Approach with Discriminated Union

**Description:** Support both materialized and streaming modes. Components detect which mode to use based on result size
or configuration.

**Code Sample:**

```java
// Discriminated union for output types
public sealed interface CanonicalOutput
    permits MaterializedOutput, StreamingOutput, OffHeapOutput {

    Map<String, Object> getMetadata();
    OutputType getType();

    enum OutputType { MATERIALIZED, STREAMING }
}

// Materialized variant (for small results)
public record MaterializedOutput(
    List<Map<String, Object>> data,
    Map<String, Object> metadata
) implements CanonicalOutput {
    @Override
    public OutputType getType() { return OutputType.MATERIALIZED; }
    @Override
    public Map<String, Object> getMetadata() { return metadata; }
}

// Streaming variant (for large results)
public record StreamingOutput(
    Iterator<Map<String, Object>> dataStream,
    Map<String, Object> metadata
) implements CanonicalOutput {
    @Override
    public OutputType getType() { return OutputType.STREAMING; }
    @Override
    public Map<String, Object> getMetadata() { return metadata; }
}

// Smart executor that chooses mode
public class PipelineExecutor {
    private static final int STREAMING_THRESHOLD = 1000;

    public CanonicalOutput apply(CanonicalInput input, ExecutionContext ctx) {
        CanonicalInput preprocessed = preProcessors.apply(input, ctx);
        QueryRequest queryRequest = buildQueryRequest(preprocessed);
        QueryResult rawResult = executeQuery(queryRequest, ctx);

        // Check if we should stream
        if (shouldStream(rawResult, input)) {
            return createStreamingOutput(rawResult, ctx);
        } else {
            return createMaterializedOutput(rawResult, ctx);
        }
    }

    private boolean shouldStream(QueryResult result, CanonicalInput input) {
        // Strategy 1: Check estimated row count
        if (result.getEstimatedRowCount() > STREAMING_THRESHOLD) {
            return true;
        }

        // Strategy 2: Check input hints
        if (input.getHint("stream").equals("true")) {
            return true;
        }

        // Strategy 3: Check for blob columns
        if (result.hasLargeObjectColumns()) {
            return true;
        }

        return false;
    }

    private StreamingOutput createStreamingOutput(
        QueryResult result,
        ExecutionContext ctx
    ) {
        Iterator<Map<String, Object>> iterator = new Iterator<>() {
            @Override
            public boolean hasNext() {
                return result.hasNext();
            }

            @Override
            public Map<String, Object> next() {
                Map<String, Object> row = result.next();
                // Apply row-level post-processing
                return postProcessRow(row, ctx);
            }
        };

        return new StreamingOutput(iterator, buildMetadata(result));
    }

    private MaterializedOutput createMaterializedOutput(
        QueryResult result,
        ExecutionContext ctx
    ) {
        List<Map<String, Object>> rows = new ArrayList<>();
        while (result.hasNext()) {
            rows.add(result.next());
        }

        // Can apply batch post-processors (sorting, aggregation)
        rows = postProcessors.applyBatch(rows, ctx);

        return new MaterializedOutput(rows, buildMetadata(result));
    }
}

// PostProcessors adapted for hybrid mode
public interface PostProcessor {
    // Row-by-row processing (works for streaming)
    Map<String, Object> processRow(Map<String, Object> row, ExecutionContext ctx);

    // Batch processing (requires materialization)
    default List<Map<String, Object>> processBatch(
        List<Map<String, Object>> rows,
        ExecutionContext ctx
    ) {
        return rows.stream()
            .map(row -> processRow(row, ctx))
            .toList();
    }

    // Declares if this processor requires materialization
    default boolean requiresMaterialization() {
        return false;
    }
}

// Example: Sorting post-processor
public class SortingPostProcessor implements PostProcessor {
    @Override
    public Map<String, Object> processRow(Map<String, Object> row, ExecutionContext ctx) {
        throw new UnsupportedOperationException("Sorting requires batch mode");
    }

    @Override
    public List<Map<String, Object>> processBatch(
        List<Map<String, Object>> rows,
        ExecutionContext ctx
    ) {
        String sortField = ctx.getParameter("sort_by");
        return rows.stream()
            .sorted(Comparator.comparing(r -> r.get(sortField).toString()))
            .toList();
    }

    @Override
    public boolean requiresMaterialization() {
        return true; // Forces materialized mode
    }
}

// TransportServer handles both modes
protected void service(HttpServletRequest req, HttpServletResponse resp) {
    ResponseEntity response = handler.handle(envelope);

    switch (response.getOutput()) {
        case MaterializedOutput mat -> {
            // Simple JSON serialization
            resp.setContentType("application/json");
            resp.getWriter().write(toJson(Map.of(
                "data", mat.data(),
                "metadata", mat.getMetadata()
            )));
        }
        case StreamingOutput stream -> {
            // Chunked streaming
            resp.setContentType("application/json");
            resp.setHeader("Transfer-Encoding", "chunked");
            streamJsonArray(stream.dataStream(), resp.getWriter());
        }
    }
}
```

**Pros:**

- ✅ Best of both worlds - optimal for each use case
- ✅ Backward compatible (small results work as before)
- ✅ Explicit about streaming vs materialization
- ✅ Post-processors can declare their requirements
- ✅ Easy to reason about performance
- ✅ Gradual migration path

**Cons:**

- ❌ More complex implementation
- ❌ Components must handle both variants
- ❌ Risk of accidentally materializing large datasets
- ❌ Threshold tuning required

**Decision:** ✅ **RECOMMENDED** - Best balance for production

---

### Option 4: Reactive Streams (Project Reactor)

**Description:** Use `Flux<Map<String, Object>>` for backpressure-aware streaming.

**Code Sample:**

```java
// Dependencies: implementation 'io.projectreactor:reactor-core:3.6.0'

public record CanonicalOutput(
    Flux<Map<String, Object>> dataFlux,
    Map<String, Object> metadata
) {
    // Flux provides: lazy evaluation, backpressure, error handling, operators
}

// PipelineExecutor
public CanonicalOutput apply(CanonicalInput input, ExecutionContext ctx) {
    QueryResult rawResult = executor.executeQuery(queryRequest, ctx);

    Flux<Map<String, Object>> dataFlux = Flux.create(sink -> {
        try {
            while (rawResult.hasNext()) {
                Map<String, Object> row = rawResult.next();
                sink.next(row); // Backpressure-aware emission
            }
            sink.complete();
        } catch (Exception e) {
            sink.error(e); // Error propagation
        }
    }, FluxSink.OverflowStrategy.BUFFER);

    // Apply post-processing operators
    dataFlux = dataFlux
        .map(row -> postProcessRow(row, ctx))
        .filter(row -> filterRow(row, ctx))
        .onErrorResume(e -> Flux.just(createErrorRow(e)));

    return new CanonicalOutput(dataFlux, metadata);
}

// HTTP Transport with Spring WebFlux
@GetMapping(value = "/query", produces = MediaType.APPLICATION_NDJSON_VALUE)
public Flux<Map<String, Object>> handleQuery(@RequestBody Map<String, Object> request) {
    RequestEnvelope envelope = adapter.toRequestEnvelope(request);
    ResponseEntity response = handler.handle(envelope);

    return response.getOutput().dataFlux()
        .doOnNext(row -> log.debug("Streaming row: {}", row))
        .doOnComplete(() -> log.info("Stream completed"))
        .doOnError(e -> log.error("Stream error", e));
}
```

**Pros:**

- ✅ Industry-standard reactive streams
- ✅ Built-in backpressure handling
- ✅ Rich operator library (map, filter, buffer, window)
- ✅ Excellent error handling
- ✅ Composable and testable
- ✅ Integration with Spring WebFlux, R2DBC

**Cons:**

- ❌ Steep learning curve
- ❌ Major architectural change
- ❌ Additional dependency (reactor-core)
- ❌ Overkill for simple streaming
- ❌ Requires reactive drivers (R2DBC, not JDBC)

**Decision:** ❌ **Rejected** - Too complex for first production version

---

### Option 5: Chunked Processing with Pagination

**Description:** Fetch data in fixed-size chunks, expose pagination metadata.

**Code Sample:**

```java
public record CanonicalOutput(
    List<Map<String, Object>> data,
    Map<String, Object> metadata,
    PaginationInfo pagination
) {
    public record PaginationInfo(
        int pageSize,
        int currentPage,
        boolean hasMore,
        String nextPageToken
    ) {}
}

// PipelineExecutor with pagination
public CanonicalOutput apply(CanonicalInput input, ExecutionContext ctx) {
    int pageSize = input.getParameterInt("pageSize", 100);
    String pageToken = input.getParameter("pageToken");

    QueryResult rawResult = executor.executeQuery(queryRequest, ctx);

    // Skip to page
    if (pageToken != null) {
        rawResult.skipTo(decodePageToken(pageToken));
    }

    // Fetch one page + 1 (to check if there's more)
    List<Map<String, Object>> page = new ArrayList<>(pageSize);
    for (int i = 0; i < pageSize + 1 && rawResult.hasNext(); i++) {
        page.add(rawResult.next());
    }

    // Check if there's more data
    boolean hasMore = page.size() > pageSize;
    if (hasMore) {
        page.remove(page.size() - 1); // Remove extra row
    }

    PaginationInfo pagination = new PaginationInfo(
        pageSize,
        getCurrentPage(pageToken),
        hasMore,
        hasMore ? createNextPageToken(page.get(page.size() - 1)) : null
    );

    return new CanonicalOutput(page, metadata, pagination);
}

// Client usage (HTTP)
GET /api/query/artists?pageSize=100
Response:
{
  "data": [ /* 100 rows */ ],
  "pagination": {
    "pageSize": 100,
    "currentPage": 1,
    "hasMore": true,
    "nextPageToken": "eyJvZmZzZXQiOjEwMH0="
  }
}

GET /api/query/artists?pageSize=100&pageToken=eyJvZmZzZXQiOjEwMH0=
Response:
{
  "data": [ /* next 100 rows */ ],
  "pagination": {
    "pageSize": 100,
    "currentPage": 2,
    "hasMore": true,
    "nextPageToken": "eyJvZmZzZXQiOjIwMH0="
  }
}
```

**Pros:**

- ✅ Predictable memory usage per request
- ✅ Client controls page size
- ✅ Easy to implement with SQL LIMIT/OFFSET
- ✅ Familiar pattern for API consumers
- ✅ Can cache pages

**Cons:**

- ❌ Multiple round-trips for large datasets
- ❌ Stateful cursor management for some DBs
- ❌ Total latency higher (sum of all pages)
- ❌ OFFSET-based pagination inefficient for large offsets
- ❌ Client complexity (must handle pagination)

**Decision:** ✅ **Complementary** - Use alongside streaming for REST API

---

### Option 6: Blob Handling with Lazy Suppliers

**Description:** For binary data, use `Supplier<InputStream>` to defer loading.

**Code Sample:**

```java
// Special handling for blob columns
public class BlobColumn {
    private final Supplier<InputStream> streamSupplier;
    private final long size;
    private final String contentType;

    public BlobColumn(Supplier<InputStream> supplier, long size, String type) {
        this.streamSupplier = supplier;
        this.size = size;
        this.contentType = type;
    }

    public InputStream openStream() {
        return streamSupplier.get(); // Lazy - only open when needed
    }
}

// In result row
Map<String, Object> row = Map.of(
    "id", 123,
    "filename", "document.pdf",
    "document", new BlobColumn(
        () -> sourceProvider.getBlobStream("documents", 123),
        1024 * 1024 * 50, // 50MB
        "application/pdf"
    )
);

// TransportServer handling blobs
protected void service(HttpServletRequest req, HttpServletResponse resp) {
    ResponseEntity response = handler.handle(envelope);

    Object documentValue = response.getData().get("document");
    if (documentValue instanceof BlobColumn blob) {
        // Stream directly without loading into memory
        resp.setContentType(blob.getContentType());
        resp.setContentLengthLong(blob.size());

        try (InputStream in = blob.openStream();
             OutputStream out = resp.getOutputStream()) {
            in.transferTo(out); // Java 9+ efficient streaming
        }
    }
}

// MCP Protocol - blob as resource
public Map<String, Object> toMcpResource(BlobColumn blob) {
    return Map.of(
        "uri", "blob://" + generateBlobId(),
        "mimeType", blob.getContentType(),
        "blob", Base64.getEncoder().encode(blob.openStream().readAllBytes())
        // Note: MCP requires base64, but we minimize memory by streaming encode
    );
}
```

**Pros:**

- ✅ Efficient blob handling
- ✅ Lazy evaluation - only load when needed
- ✅ Can stream directly to response
- ✅ Works with existing Map structure

**Cons:**

- ❌ Special case handling
- ❌ Type checking required (`instanceof`)
- ❌ MCP still requires full materialization (base64)
- ❌ Streams not reusable

**Decision:** ✅ **Complementary** - Use for blob columns

---

## Decision Outcome

### **Chosen Option: Option 3 - Hybrid Approach with Discriminated Union

** + Option 5 (Pagination for REST) + Option 6 (Blobs)

**Rationale:**

1. **Production Ready**: Provides safe defaults (materialized for small data) while supporting streaming for large
   datasets
2. **Gradual Adoption**: Teams can adopt streaming incrementally without breaking existing code
3. **Explicit**: Type system makes it clear when streaming vs materialization
4. **Flexible**: Post-processors declare their requirements
5. **Performance**: Optimal for each use case

### Implementation Plan---

## Consequences

### Positive

✅ **Memory Efficiency**

- Constant memory usage for streaming queries (O(1) vs O(n))
- Large datasets (millions of rows) handled safely
- Blob streaming without heap pressure

✅ **Performance**

- Time-to-first-byte improved for large queries
- Reduced GC pressure
- Better throughput for concurrent requests

✅ **Backward Compatibility**

- Small datasets work exactly as before (materialized)
- Gradual adoption - no forced migration
- Existing tests continue to pass

✅ **Flexibility**

- Post-processors can declare materialization requirements
- Client can control behavior via hints
- Easy to add new streaming strategies

✅ **Observability**

- Stream IDs for tracking
- Progress logging built-in
- Clear distinction in metrics

### Negative

❌ **Complexity**

- More code paths to test
- Components must handle both variants
- Learning curve for developers

❌ **Testing Overhead**

- Must test both materialized and streaming paths
- Stream error handling requires careful testing
- Integration tests more complex

❌ **Partial Results Risk**

- Streaming can fail mid-stream
- Client sees partial data before error
- Requires client-side error handling

❌ **PostProcessor Limitations**

- Operations like sorting/grouping force materialization
- Developer must understand which operations are streaming-safe
- Documentation critical

---

## Migration Strategy

### Phase 1: Foundation (Week 1-2)

1. Implement core abstractions (`CanonicalOutput` variants)
2. Add `OutputStrategy` and `StreamingConfig`
3. Update `PipelineExecutor` with hybrid logic
4. Write unit tests for both paths

### Phase 2: Transport Layer (Week 3)

1. Update `TransportServer` to handle both output types
2. Implement HTTP chunked transfer encoding
3. Add blob column handling
4. Add stream ID tracking and logging

### Phase 3: PostProcessor Migration (Week 4)

1. Audit existing post-processors
2. Mark processors requiring materialization
3. Implement streaming-safe variants where possible
4. Document streaming limitations

### Phase 4: Testing & Validation (Week 5-6)

1. Integration tests with large datasets
2. Load testing with concurrent streams
3. Error injection testing (mid-stream failures)
4. Performance benchmarking

### Phase 5: Documentation & Rollout (Week 7)

1. Developer documentation
2. API documentation for clients
3. Gradual rollout with feature flags
4. Monitor metrics and adjust thresholds

---

## Monitoring & Observability---

## Testing Strategy---

## Risks and Mitigation

| Risk                                | Impact | Likelihood | Mitigation                                                                                                   |
|-------------------------------------|--------|------------|--------------------------------------------------------------------------------------------------------------|
| **Partial results on stream error** | High   | Medium     | 1. Add checksum/row count in metadata<br>2. Document client retry strategy<br>3. Log stream ID for debugging |
| **Developer confusion**             | Medium | High       | 1. Clear documentation<br>2. IDE hints via annotations<br>3. Training sessions                               |
| **Performance regression**          | High   | Low        | 1. Benchmark before/after<br>2. Gradual rollout with metrics<br>3. Feature flags for rollback                |
| **Memory leak in iterators**        | High   | Low        | 1. Thorough testing<br>2. Memory profiling<br>3. Iterator timeout mechanisms                                 |
| **PostProcessor incompatibility**   | Medium | Medium     | 1. Audit all processors<br>2. Clear requiresMaterialization() contract<br>3. Runtime validation              |

---

## Alternative Approaches Not Chosen

### Cursor-based Pagination Only

**Why rejected:** Still requires multiple round trips, doesn't solve blob streaming, adds API complexity

### Async/Await Pattern (CompletableFuture)

**Why rejected:** Doesn't solve memory problem, adds complexity without streaming benefits

### GraphQL with Deferred/Stream Directives

**Why rejected:** Major protocol change, requires GraphQL adoption, overkill for internal pipeline

### gRPC Streaming

**Why rejected:** Transport-specific, requires proto definitions, doesn't help with internal pipeline

### Custom Binary Protocol

**Why rejected:** High maintenance burden, tooling needed, JSON works fine for first version

---

## Related Decisions

**ADR-001: Pipeline Architecture**

- This decision extends the canonical pipeline with streaming support
- Maintains protocol-agnostic design principle

**ADR-005: Query Engine Integration (Apache Calcite)**

- QueryEngine already returns iterators - this leverages that design
- No changes needed to Calcite integration

**ADR-012: MCP Protocol Support**

- MCP notifications will use batched streaming
- resources/list_changed for incremental updates

---

## References

### Documentation

- [Reactive Streams Specification](https://www.reactive-streams.org/)
- [HTTP Chunked Transfer Encoding](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Transfer-Encoding)
- [Apache Calcite - Streaming](https://calcite.apache.org/docs/stream.html)

### Code Examples

- [Spring WebFlux Streaming](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-codecs-streaming)
- [PostgreSQL JDBC Cursor](https://jdbc.postgresql.org/documentation/query/#getting-results-based-on-a-cursor)

### Similar Implementations

- GraphQL Defer/Stream: https://graphql.org/blog/2020-12-08-improving-latency-with-defer-and-stream-directives/
- gRPC Server Streaming: https://grpc.io/docs/what-is-grpc/core-concepts/#server-streaming-rpc

---

## Decision Timeline

| Date       | Event                            |
|------------|----------------------------------|
| 2025-01-01 | ADR proposed                     |
| 2025-01-05 | Team review completed            |
| 2025-01-08 | Architecture approved            |
| 2025-01-15 | Implementation started (Phase 1) |
| 2025-02-15 | Production deployment (target)   |

---

## Appendix: Complete Example Sequence

### Materialized Flow (Small Dataset)

```
1. Client → TransportServer: GET /api/query/artists
2. TransportServer → ProtocolAdapter: toRequestEnvelope()
3. ProtocolAdapter → RequestHandler: RequestEnvelope{capability="query", action="artists"}
4. RequestHandler → Dispatcher: handle(envelope)
5. Dispatcher → RuntimeSession: execute(task)
6. RuntimeSession → PipelineExecutor: apply(input)
7. PipelineExecutor → QueryEngine: executeQuery()
8. QueryEngine → SourceProvider: SELECT * FROM artists
9. SourceProvider → QueryEngine: 100 rows
10. QueryEngine → PipelineExecutor: QueryResult (iterator)
11. PipelineExecutor: Decides MATERIALIZED (< 1000 rows)
12. PipelineExecutor: Materializes all 100 rows into List
13. PipelineExecutor → PostProcessors: processBatch(rows)
14. PostProcessors → PipelineExecutor: Transformed 100 rows
15. PipelineExecutor → RuntimeSession: MaterializedOutput{data=[100 rows]}
16. RuntimeSession → Dispatcher: TaskResult.success()
17. Dispatcher → RequestHandler: ResponseEntity.ok()
18. RequestHandler → TransportServer: ResponseEntity
19. TransportServer → ProtocolAdapter: fromProcessingResult()
20. ProtocolAdapter → TransportServer: JSON Map
21. TransportServer → Client: HTTP 200 + Complete JSON
```

### Streaming Flow (Large Dataset)

```
1. Client → TransportServer: GET /api/query/large_table?stream=true
2. TransportServer → ProtocolAdapter: toRequestEnvelope()
3. ProtocolAdapter → RequestHandler: RequestEnvelope{capability="query", action="large_table"}
4. RequestHandler → Dispatcher: handle(envelope)
5. Dispatcher → RuntimeSession: execute(task)
6. RuntimeSession → PipelineExecutor: apply(input)
7. PipelineExecutor → QueryEngine: executeQuery()
8. QueryEngine → SourceProvider: SELECT * FROM large_table
9. SourceProvider → QueryEngine: Iterator (lazy)
10. QueryEngine → PipelineExecutor: QueryResult (iterator, estimated=100K)
11. PipelineExecutor: Decides STREAMING (> 1000 rows)
12. PipelineExecutor: Wraps iterator with row-level post-processors
13. PipelineExecutor → RuntimeSession: StreamingOutput{stream=iterator}
14. RuntimeSession → Dispatcher: TaskResult.success()
15. Dispatcher → RequestHandler: ResponseEntity.ok()
16. RequestHandler → TransportServer: ResponseEntity
17. TransportServer: Sets Transfer-Encoding: chunked
18. TransportServer: Writes JSON opening: {"data":[
19. Loop starts:
    19a. Iterator.next() → Fetch 1 row from DB
    19b. Apply row-level post-processors
    19c. Write row as JSON
    19d. Flush to client (client receives data)
    19e. Repeat for next row
20. Iterator exhausted
21. TransportServer: Writes JSON closing: ],"metadata":{...}}
22. Connection closed
```

---

## Approval

**Approved by:**

- [ ] Tech Lead
- [ ] Principal Engineer
- [ ] VP Engineering

**Status:** ✅ **APPROVED** - Ready for implementation

**Date:** 2025-01-01

---

## Revision History

| Version | Date       | Author           | Changes          |
|---------|------------|------------------|------------------|
| 1.0     | 2025-01-01 | Engineering Team | Initial proposal |

---

## Summary & Recommendation

**Problem:** Current pipeline materializes all data in memory, causing OOM errors and high latency for large datasets.

**Solution:** Implement hybrid approach with discriminated union (`MaterializedOutput` | `StreamingOutput`), allowing
the system to automatically choose optimal strategy based on dataset size, column types, and post-processor
requirements.

**Key Benefits:**

1. **Production-ready:** Safe defaults with opt-in streaming
2. **Memory efficient:** Constant memory for large datasets
3. **Low latency:** Stream first results immediately
4. **Backward compatible:** Existing code continues to work
5. **Flexible:** Supports both small and large use cases

**Recommendation:** Approve and proceed with Phase 1 implementation.




