// ============================================================================
// PHASE 1: Core Abstractions
// ============================================================================

package com.chinook.pipeline.core;

import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;

/**
 * Discriminated union for canonical outputs.
 * Components must pattern-match to handle both variants.
 */
public sealed interface CanonicalOutput
        permits MaterializedOutput, OffHeapOutput, StreamingOutput {

    Map<String, Object> getMetadata();

    OutputType getType();

    /**
     * Visitor pattern for type-safe handling
     */
    default <R> R match(
            java.util.function.Function<MaterializedOutput, R> onMaterialized,
            java.util.function.Function<StreamingOutput, R> onStreaming
    ) {
        return switch (this) {
            case MaterializedOutput m -> onMaterialized.apply(m);
            case StreamingOutput s -> onStreaming.apply(s);
        };
    }

    enum OutputType {
        MATERIALIZED,  // All data in memory
        STREAMING      // Lazy iteration
    }
}

/**
 * PostProcessor that can work in streaming or batch mode
 */
public interface PostProcessor {

    /**
     * Process a single row (streaming-compatible)
     */
    Map<String, Object> processRow(
            Map<String, Object> row,
            ExecutionContext ctx
    );

    /**
     * Process entire batch (for operations requiring full dataset)
     */
    default List<Map<String, Object>> processBatch(
            List<Map<String, Object>> rows,
            ExecutionContext ctx
    ) {
        return rows.stream()
                .map(row -> processRow(row, ctx))
                .toList();
    }

    /**
     * Does this processor require all data to be materialized?
     */
    default boolean requiresMaterialization() {
        return false;
    }
}

/**
 * Materialized output - all data loaded in memory.
 * Use for: small result sets, results requiring batch operations.
 */
public record MaterializedOutput(
        Map<String, Object> data,
        Map<String, Object> metadata
) implements CanonicalOutput {

    @Override
    public OutputType getType() {
        return OutputType.MATERIALIZED;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public int getRowCount() {
        return data.size();
    }
}

/**
 * Streaming output - lazy iteration over data.
 * Use for: large result sets, real-time data, blob columns.
 */
public record StreamingOutput(
        Iterator<Map<String, Object>> dataStream,
        Map<String, Object> metadata,
        StreamingMetadata streamMetadata
) implements CanonicalOutput {

    @Override
    public OutputType getType() {
        return OutputType.STREAMING;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public record StreamingMetadata(
            Long estimatedRowCount,  // null if unknown
            boolean hasLargeObjects,
            String streamId
    ) {
    }
}

// ============================================================================
// PHASE 2: Configuration & Strategy
// ============================================================================

/**
 * Lazy blob wrapper - streams binary data without loading into memory
 */
public class BlobColumn {
    private final Supplier<InputStream> streamSupplier;
    private final long size;
    private final String contentType;
    private final String sourceId;

    public BlobColumn(
            Supplier<InputStream> supplier,
            long size,
            String contentType,
            String sourceId
    ) {
        this.streamSupplier = supplier;
        this.size = size;
        this.contentType = contentType;
        this.sourceId = sourceId;
    }

    public InputStream openStream() {
        return streamSupplier.get();
    }

    public long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public boolean isLarge() {
        return size > 10 * 1024 * 1024; // > 10MB
    }
}

/**
 * Configuration for streaming behavior
 */
public class StreamingConfig {
    private final int materializationThreshold;
    private final int blobThreshold;
    private final boolean forceStreaming;
    private final Set<String> streamingCapabilities;

    public StreamingConfig(
            int materializationThreshold,
            int blobThreshold,
            boolean forceStreaming,
            Set<String> streamingCapabilities
    ) {
        this.materializationThreshold = materializationThreshold;
        this.blobThreshold = blobThreshold;
        this.forceStreaming = forceStreaming;
        this.streamingCapabilities = streamingCapabilities;
    }

    public static StreamingConfig defaults() {
        return new StreamingConfig(
                1000,                    // Materialize if < 1000 rows
                5 * 1024 * 1024,        // Stream blobs > 5MB
                false,                   // Don't force streaming
                Set.of("query", "export") // Capabilities that support streaming
        );
    }

    public boolean shouldStream(String capability) {
        return forceStreaming || streamingCapabilities.contains(capability);
    }

    public int getMaterializationThreshold() {
        return materializationThreshold;
    }

    public int getBlobThreshold() {
        return blobThreshold;
    }
}

// ============================================================================
// PHASE 3: PipelineExecutor with Hybrid Logic
// ============================================================================

/**
 * Strategy for deciding materialized vs streaming
 */
public class OutputStrategy {
    private final StreamingConfig config;

    public OutputStrategy(StreamingConfig config) {
        this.config = config;
    }

    public boolean shouldStream(CanonicalInput input, QueryResult queryResult) {
        // 1. Check explicit hints from client
        String streamHint = input.getParameter("stream");
        if ("true".equalsIgnoreCase(streamHint)) {
            return true;
        }
        if ("false".equalsIgnoreCase(streamHint)) {
            return false;
        }

        // 2. Check if capability supports streaming
        if (!config.shouldStream(input.getCapability())) {
            return false;
        }

        // 3. Check estimated row count
        Long estimatedRows = queryResult.getEstimatedRowCount();
        if (estimatedRows != null && estimatedRows > config.getMaterializationThreshold()) {
            return true;
        }

        // 4. Check for large object columns
        if (queryResult.hasLargeObjectColumns()) {
            return true;
        }

        // 5. Check if post-processors require materialization
        if (input.hasPostProcessorRequiringMaterialization()) {
            return false;
        }

        // Default: materialize for predictable behavior
        return false;
    }
}

// ============================================================================
// PHASE 4: PostProcessor Interface
// ============================================================================

public class PipelineExecutor {
    private final PreProcessorChain preProcessors;
    private final QueryExecutor executor;
    private final PostProcessorChain postProcessors;
    private final OutputStrategy strategy;
    private final StreamingConfig config;

    public PipelineExecutor(
            PreProcessorChain preProcessors,
            QueryExecutor executor,
            PostProcessorChain postProcessors,
            StreamingConfig config
    ) {
        this.preProcessors = preProcessors;
        this.executor = executor;
        this.postProcessors = postProcessors;
        this.config = config;
        this.strategy = new OutputStrategy(config);
    }

    public CanonicalOutput apply(CanonicalInput input, ExecutionContext ctx) {
        // 1. PreProcessors
        CanonicalInput preprocessed = preProcessors.apply(input, ctx);

        // 2. Build and execute query
        QueryRequest queryRequest = executor.buildQueryRequest(preprocessed);
        QueryResult rawResult = executor.executeQuery(queryRequest, ctx);

        // 3. Decide streaming vs materialization
        if (strategy.shouldStream(input, rawResult)) {
            return createStreamingOutput(rawResult, preprocessed, ctx);
        } else {
            return createMaterializedOutput(rawResult, preprocessed, ctx);
        }
    }

    private StreamingOutput createStreamingOutput(
            QueryResult queryResult,
            CanonicalInput input,
            ExecutionContext ctx
    ) {
        String streamId = UUID.randomUUID().toString();

        // Wrap in iterator that applies row-level post-processing
        Iterator<Map<String, Object>> streamingIterator = new Iterator<>() {
            @Override
            public boolean hasNext() {
                return queryResult.hasNext();
            }

            @Override
            public Map<String, Object> next() {
                Map<String, Object> row = queryResult.next();

                // Apply row-level post-processors
                row = postProcessors.processRow(row, ctx);

                // Handle blob columns
                row = wrapBlobColumns(row, ctx);

                return row;
            }
        };

        StreamingOutput.StreamingMetadata streamMetadata =
                new StreamingOutput.StreamingMetadata(
                        queryResult.getEstimatedRowCount(),
                        queryResult.hasLargeObjectColumns(),
                        streamId
                );

        Map<String, Object> metadata = buildMetadata(queryResult, input);
        metadata.put("streaming", true);
        metadata.put("streamId", streamId);

        return new StreamingOutput(streamingIterator, metadata, streamMetadata);
    }

    private MaterializedOutput createMaterializedOutput(
            QueryResult queryResult,
            CanonicalInput input,
            ExecutionContext ctx
    ) {
        // Materialize all rows
        List<Map<String, Object>> rows = new ArrayList<>();
        while (queryResult.hasNext()) {
            Map<String, Object> row = queryResult.next();
            row = wrapBlobColumns(row, ctx);
            rows.add(row);
        }

        // Apply batch post-processors
        rows = postProcessors.processBatch(rows, ctx);

        Map<String, Object> metadata = buildMetadata(queryResult, input);
        metadata.put("streaming", false);
        metadata.put("rowCount", rows.size());

        return new MaterializedOutput(rows, metadata);
    }

    private Map<String, Object> wrapBlobColumns(
            Map<String, Object> row,
            ExecutionContext ctx
    ) {
        Map<String, Object> processed = new HashMap<>(row);

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getValue() instanceof byte[] bytes) {
                // Wrap byte arrays as lazy blob columns
                if (bytes.length > config.getBlobThreshold()) {
                    processed.put(entry.getKey(), new BlobColumn(
                            () -> new java.io.ByteArrayInputStream(bytes),
                            bytes.length,
                            "application/octet-stream",
                            ctx.generateBlobId()
                    ));
                }
            }
        }

        return processed;
    }

    private Map<String, Object> buildMetadata(
            QueryResult result,
            CanonicalInput input
    ) {
        return Map.of(
                "capability", input.getCapability(),
                "action", input.getAction(),
                "executionTimeMs", result.getExecutionTimeMs(),
                "columnNames", result.getColumnNames()
        );
    }
}

/**
 * Example: Field projection processor (streaming-safe)
 */
public class ProjectionPostProcessor implements PostProcessor {
    private final Set<String> includedFields;

    public ProjectionPostProcessor(Set<String> includedFields) {
        this.includedFields = includedFields;
    }

    @Override
    public Map<String, Object> processRow(
            Map<String, Object> row,
            ExecutionContext ctx
    ) {
        if (includedFields == null || includedFields.isEmpty()) {
            return row;
        }

        Map<String, Object> projected = new HashMap<>();
        for (String field : includedFields) {
            if (row.containsKey(field)) {
                projected.put(field, row.get(field));
            }
        }
        return projected;
    }
}

/**
 * Example: Sorting processor (requires materialization)
 */
public class SortingPostProcessor implements PostProcessor {
    private final String sortField;
    private final boolean ascending;

    public SortingPostProcessor(String sortField, boolean ascending) {
        this.sortField = sortField;
        this.ascending = ascending;
    }

    @Override
    public Map<String, Object> processRow(
            Map<String, Object> row,
            ExecutionContext ctx
    ) {
        throw new UnsupportedOperationException(
                "Sorting requires materialization. Use processBatch instead."
        );
    }

    @Override
    public List<Map<String, Object>> processBatch(
            List<Map<String, Object>> rows,
            ExecutionContext ctx
    ) {
        Comparator<Map<String, Object>> comparator =
                Comparator.comparing(r -> r.get(sortField).toString());

        if (!ascending) {
            comparator = comparator.reversed();
        }

        return rows.stream().sorted(comparator).toList();
    }

    @Override
    public boolean requiresMaterialization() {
        return true;
    }
}

/**
 * Chain of post-processors
 */
public class PostProcessorChain {
    private final List<PostProcessor> processors;

    public PostProcessorChain(List<PostProcessor> processors) {
        this.processors = processors;
    }

    public Map<String, Object> processRow(
            Map<String, Object> row,
            ExecutionContext ctx
    ) {
        Map<String, Object> result = row;
        for (PostProcessor processor : processors) {
            result = processor.processRow(result, ctx);
        }
        return result;
    }

    public List<Map<String, Object>> processBatch(
            List<Map<String, Object>> rows,
            ExecutionContext ctx
    ) {
        List<Map<String, Object>> result = rows;
        for (PostProcessor processor : processors) {
            result = processor.processBatch(result, ctx);
        }
        return result;
    }

    public boolean requiresMaterialization() {
        return processors.stream()
                .anyMatch(PostProcessor::requiresMaterialization);
    }
}

// ============================================================================
// PHASE 5: Transport Layer Handling
// ============================================================================

/**
 * HTTP Transport with streaming support
 */
public class HttpTransportServer extends HttpServlet {
    private final ProtocolAdapter adapter;
    private final RequestHandler handler;

    @Override
    protected void service(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException {
        RequestEnvelope envelope = adapter.toRequestEnvelope(req);
        ResponseEntity response = handler.handle(envelope);

        if (!response.isSuccess()) {
            writeError(response, resp);
            return;
        }

        CanonicalOutput output = response.getOutput();

        // Pattern match on output type
        output.match(
                materialized -> {
                    writeMaterialized(materialized, resp);
                    return null;
                },
                streaming -> {
                    writeStreaming(streaming, resp);
                    return null;
                }
        );
    }

    private void writeMaterialized(
            MaterializedOutput output,
            HttpServletResponse resp
    ) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> response = Map.of(
                "data", output.data(),
                "metadata", output.getMetadata()
        );

        resp.getWriter().write(toJson(response));
    }

    private void writeStreaming(
            StreamingOutput output,
            HttpServletResponse resp
    ) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Transfer-Encoding", "chunked");
        resp.setHeader("X-Stream-Id", output.streamMetadata().streamId());

        PrintWriter writer = resp.getWriter();

        // Start JSON structure
        writer.write("{\"data\":[");

        Iterator<Map<String, Object>> stream = output.dataStream();
        boolean first = true;
        int rowCount = 0;

        try {
            while (stream.hasNext()) {
                Map<String, Object> row = stream.next();

                if (!first) {
                    writer.write(",");
                }

                // Handle blob columns specially
                row = serializeBlobColumns(row);

                writer.write(toJson(row));
                writer.flush(); // Push to client immediately

                first = false;
                rowCount++;

                // Log progress for monitoring
                if (rowCount % 1000 == 0) {
                    logStreamingProgress(output.streamMetadata().streamId(), rowCount);
                }
            }
        } catch (Exception e) {
            // Error mid-stream - send error marker
            writer.write(",{\"error\":\"Stream interrupted: " + e.getMessage() + "\"}");
        }

        // Complete JSON structure
        writer.write("],\"metadata\":");
        Map<String, Object> metadata = new HashMap<>(output.getMetadata());
        metadata.put("streamedRowCount", rowCount);
        writer.write(toJson(metadata));
        writer.write("}");
    }

    private Map<String, Object> serializeBlobColumns(Map<String, Object> row) {
        Map<String, Object> serialized = new HashMap<>(row);

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getValue() instanceof BlobColumn blob) {
                // For small blobs, inline base64
                if (!blob.isLarge()) {
                    try (InputStream in = blob.openStream()) {
                        byte[] bytes = in.readAllBytes();
                        serialized.put(entry.getKey(), Map.of(
                                "type", "blob",
                                "contentType", blob.getContentType(),
                                "size", blob.getSize(),
                                "data", Base64.getEncoder().encodeToString(bytes)
                        ));
                    } catch (IOException e) {
                        serialized.put(entry.getKey(), Map.of(
                                "type", "blob",
                                "error", "Failed to read blob"
                        ));
                    }
                } else {
                    // For large blobs, provide download URL
                    serialized.put(entry.getKey(), Map.of(
                            "type", "blob",
                            "contentType", blob.getContentType(),
                            "size", blob.getSize(),
                            "downloadUrl", "/api/blobs/" + blob.getSourceId()
                    ));
                }
            }
        }

        return serialized;
    }

    private String toJson(Object obj) {
        // Use your JSON library (Jackson, Gson, etc.)
        return "{}"; // Placeholder
    }

    private void writeError(ResponseEntity response, HttpServletResponse resp) {
        // Implementation
    }

    private void logStreamingProgress(String streamId, int rowCount) {
        // Implementation
    }
}

// ============================================================================
// PHASE 6: Usage Examples
// ============================================================================

/**
 * Example: Configuration in application startup
 */
public class Application {
    public static void main(String[] args) {
        // Configure streaming behavior
        StreamingConfig config = new StreamingConfig(
                1000,                           // Materialize if < 1000 rows
                5 * 1024 * 1024,               // Stream blobs > 5MB
                false,                          // Don't force streaming
                Set.of("query", "export")      // Streaming-enabled capabilities
        );

        // Build pipeline
        PipelineExecutor executor = new PipelineExecutor(
                new PreProcessorChain(List.of(/* ... */)),
                new QueryExecutor(/* ... */),
                new PostProcessorChain(List.of(
                        new ProjectionPostProcessor(Set.of("id", "name")),
                        new SortingPostProcessor("name", true)  // Forces materialization
                )),
                config
        );

        // Start jetty
        HttpTransportServer server = new HttpTransportServer(
                new RestProtocolAdapter(),
                new RequestHandler(new Dispatcher(executor))
        );
    }
}

/**
 * Example: Client requesting streaming
 */
// HTTP Request with streaming hint
// GET /api/query/large-dataset?stream=true

// Response (chunked)
// HTTP/1.1 200 OK
// Transfer-Encoding: chunked
// Content-Type: application/json
// X-Stream-Id: 550e8400-e29b-41d4-a716-446655440000
//
// {"data":[
//   {"id":1,"name":"Alice"},
//   {"id":2,"name":"Bob"},
//   ...
// ],"metadata":{"streamedRowCount":50000}}

// ============================================================================
// Monitoring & Metrics for Hybrid Streaming
// ============================================================================

import io.micrometer.core.instrument .*;
        import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics collection for streaming operations
 */
public class StreamingMetrics {
    private static final Logger log = LoggerFactory.getLogger(StreamingMetrics.class);

    private final MeterRegistry registry;

    // Counters
    private final Counter materializedOutputs;
    private final Counter streamingOutputs;
    private final Counter streamingErrors;
    private final Counter materializedErrors;

    // Timers
    private final Timer materializedExecutionTime;
    private final Timer streamingExecutionTime;
    private final Timer firstByteTime;

    // Gauges
    private final AtomicLong activeStreams;
    private final AtomicLong totalBytesStreamed;

    // Distributions
    private final DistributionSummary materializedRowCount;
    private final DistributionSummary streamedRowCount;

    public StreamingMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Initialize counters
        this.materializedOutputs = Counter.builder("pipeline.output.materialized")
                .description("Number of materialized outputs")
                .register(registry);

        this.streamingOutputs = Counter.builder("pipeline.output.streaming")
                .description("Number of streaming outputs")
                .register(registry);

        this.streamingErrors = Counter.builder("pipeline.streaming.errors")
                .description("Number of streaming errors")
                .tag("type", "mid_stream")
                .register(registry);

        this.materializedErrors = Counter.builder("pipeline.materialized.errors")
                .description("Number of materialized errors")
                .register(registry);

        // Initialize timers
        this.materializedExecutionTime = Timer.builder("pipeline.execution.time")
                .description("Total execution time")
                .tag("mode", "materialized")
                .register(registry);

        this.streamingExecutionTime = Timer.builder("pipeline.execution.time")
                .description("Total execution time")
                .tag("mode", "streaming")
                .register(registry);

        this.firstByteTime = Timer.builder("pipeline.streaming.first_byte")
                .description("Time to first byte in streaming mode")
                .register(registry);

        // Initialize gauges
        this.activeStreams = new AtomicLong(0);
        Gauge.builder("pipeline.streaming.active", activeStreams, AtomicLong::get)
                .description("Number of active streams")
                .register(registry);

        this.totalBytesStreamed = new AtomicLong(0);
        Gauge.builder("pipeline.streaming.bytes_total", totalBytesStreamed, AtomicLong::get)
                .description("Total bytes streamed")
                .baseUnit("bytes")
                .register(registry);

        // Initialize distributions
        this.materializedRowCount = DistributionSummary.builder("pipeline.materialized.row_count")
                .description("Number of rows in materialized outputs")
                .register(registry);

        this.streamedRowCount = DistributionSummary.builder("pipeline.streaming.row_count")
                .description("Number of rows in streaming outputs")
                .register(registry);
    }

    public void recordMaterializedOutput(int rowCount, Duration executionTime) {
        materializedOutputs.increment();
        materializedRowCount.record(rowCount);
        materializedExecutionTime.record(executionTime);
    }

    public void recordStreamingOutput(long rowCount, Duration executionTime) {
        streamingOutputs.increment();
        streamedRowCount.record(rowCount);
        streamingExecutionTime.record(executionTime);
    }

    public void recordFirstByte(Duration timeToFirstByte) {
        firstByteTime.record(timeToFirstByte);
    }

    public void streamStarted() {
        activeStreams.incrementAndGet();
    }

    public void streamCompleted(long bytesStreamed) {
        activeStreams.decrementAndGet();
        totalBytesStreamed.addAndGet(bytesStreamed);
    }

    public void recordStreamingError(String errorType) {
        streamingErrors.increment();
        log.error("Streaming error occurred: type={}", errorType);
    }

    public void recordMaterializedError() {
        materializedErrors.increment();
    }
}

/**
 * Instrumented PipelineExecutor with metrics
 */
public class InstrumentedPipelineExecutor extends PipelineExecutor {
    private static final Logger log = LoggerFactory.getLogger(InstrumentedPipelineExecutor.class);

    private final StreamingMetrics metrics;

    public InstrumentedPipelineExecutor(
            PreProcessorChain preProcessors,
            QueryExecutor executor,
            PostProcessorChain postProcessors,
            StreamingConfig config,
            StreamingMetrics metrics
    ) {
        super(preProcessors, executor, postProcessors, config);
        this.metrics = metrics;
    }

    @Override
    public CanonicalOutput apply(CanonicalInput input, ExecutionContext ctx) {
        long startTime = System.nanoTime();

        try {
            CanonicalOutput output = super.apply(input, ctx);

            Duration executionTime = Duration.ofNanos(System.nanoTime() - startTime);

            // Record metrics based on output type
            output.match(
                    materialized -> {
                        metrics.recordMaterializedOutput(
                                materialized.getRowCount(),
                                executionTime
                        );
                        log.info("Materialized output: rows={}, time={}ms",
                                materialized.getRowCount(),
                                executionTime.toMillis());
                        return null;
                    },
                    streaming -> {
                        // Wrap stream to count rows
                        String streamId = streaming.streamMetadata().streamId();
                        metrics.streamStarted();

                        Iterator<Map<String, Object>> instrumentedStream =
                                new InstrumentedIterator(
                                        streaming.dataStream(),
                                        streamId,
                                        metrics,
                                        executionTime
                                );

                        return new StreamingOutput(
                                instrumentedStream,
                                streaming.getMetadata(),
                                streaming.streamMetadata()
                        );
                    }
            );

            return output;

        } catch (Exception e) {
            if (ctx.isStreaming()) {
                metrics.recordStreamingError(e.getClass().getSimpleName());
            } else {
                metrics.recordMaterializedError();
            }
            throw e;
        }
    }
}

/**
 * Iterator wrapper that collects metrics during streaming
 */
class InstrumentedIterator implements Iterator<Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(InstrumentedIterator.class);

    private final Iterator<Map<String, Object>> delegate;
    private final String streamId;
    private final StreamingMetrics metrics;
    private final long startNanos;

    private long rowCount = 0;
    private long bytesEstimate = 0;
    private boolean firstRowSent = false;
    private boolean completed = false;

    public InstrumentedIterator(
            Iterator<Map<String, Object>> delegate,
            String streamId,
            StreamingMetrics metrics,
            Duration executionTime
    ) {
        this.delegate = delegate;
        this.streamId = streamId;
        this.metrics = metrics;
        this.startNanos = System.nanoTime();
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = delegate.hasNext();

        if (!hasNext && !completed) {
            // Stream completed
            completed = true;
            Duration totalTime = Duration.ofNanos(System.nanoTime() - startNanos);

            metrics.recordStreamingOutput(rowCount, totalTime);
            metrics.streamCompleted(bytesEstimate);

            log.info("Stream completed: id={}, rows={}, bytes={}, time={}ms",
                    streamId, rowCount, bytesEstimate, totalTime.toMillis());
        }

        return hasNext;
    }

    @Override
    public Map<String, Object> next() {
        Map<String, Object> row = delegate.next();

        if (!firstRowSent) {
            Duration firstByteTime = Duration.ofNanos(System.nanoTime() - startNanos);
            metrics.recordFirstByte(firstByteTime);
            firstRowSent = true;

            log.debug("First row sent: id={}, time={}ms",
                    streamId, firstByteTime.toMillis());
        }

        rowCount++;
        bytesEstimate += estimateRowSize(row);

        // Log progress every 10K rows
        if (rowCount % 10000 == 0) {
            log.info("Streaming progress: id={}, rows={}, bytes={}",
                    streamId, rowCount, bytesEstimate);
        }

        return row;
    }

    private long estimateRowSize(Map<String, Object> row) {
        long size = 0;
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            size += entry.getKey().length();

            Object value = entry.getValue();
            if (value instanceof String s) {
                size += s.length();
            } else if (value instanceof BlobColumn blob) {
                size += blob.getSize();
            } else {
                size += 50; // Rough estimate for other types
            }
        }
        return size;
    }
}

// ============================================================================
// Health Checks
// ============================================================================

/**
 * Health check for streaming subsystem
 */
public class StreamingHealthIndicator {
    private final StreamingMetrics metrics;
    private final StreamingConfig config;

    public StreamingHealthIndicator(
            StreamingMetrics metrics,
            StreamingConfig config
    ) {
        this.metrics = metrics;
        this.config = config;
    }

    public HealthStatus checkHealth() {
        // Check if too many active streams
        long activeStreams = metrics.activeStreams.get();
        if (activeStreams > 100) {
            return HealthStatus.degraded(
                    "Too many active streams: " + activeStreams
            );
        }

        // Check error rate
        double errorRate = calculateErrorRate();
        if (errorRate > 0.05) { // 5% error rate
            return HealthStatus.unhealthy(
                    "High streaming error rate: " + String.format("%.2f%%", errorRate * 100)
            );
        }

        return HealthStatus.healthy();
    }

    private double calculateErrorRate() {
        double totalStreams = metrics.streamingOutputs.count();
        double errors = metrics.streamingErrors.count();

        if (totalStreams == 0) {
            return 0.0;
        }

        return errors / totalStreams;
    }

    public static class HealthStatus {
        private final Status status;
        private final String message;

        private HealthStatus(Status status, String message) {
            this.status = status;
            this.message = message;
        }

        public static HealthStatus healthy() {
            return new HealthStatus(Status.HEALTHY, "OK");
        }

        public static HealthStatus degraded(String message) {
            return new HealthStatus(Status.DEGRADED, message);
        }

        public static HealthStatus unhealthy(String message) {
            return new HealthStatus(Status.UNHEALTHY, message);
        }

        public Status getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public enum Status {HEALTHY, DEGRADED, UNHEALTHY}
    }
}

// ============================================================================
// Logging Configuration
// ============================================================================

/**
 * Structured logging for streaming operations
 */
public class StreamingLogger {
    private static final Logger log = LoggerFactory.getLogger(StreamingLogger.class);

    public void logStreamStart(
            String streamId,
            String capability,
            String action,
            Long estimatedRows
    ) {
        log.info("Stream started: id={}, capability={}, action={}, estimatedRows={}",
                streamId, capability, action, estimatedRows);
    }

    public void logStreamProgress(
            String streamId,
            long rowsStreamed,
            long bytesStreamed,
            Duration elapsed
    ) {
        log.info("Stream progress: id={}, rows={}, bytes={}, elapsed={}ms",
                streamId, rowsStreamed, bytesStreamed, elapsed.toMillis());
    }

    public void logStreamComplete(
            String streamId,
            long totalRows,
            long totalBytes,
            Duration totalTime
    ) {
        double throughputMBps = (totalBytes / 1024.0 / 1024.0) /
                (totalTime.toMillis() / 1000.0);
        double rowsPerSec = totalRows / (totalTime.toMillis() / 1000.0);

        log.info("Stream completed: id={}, rows={}, bytes={}, time={}ms, " +
                        "throughput={} MB/s, rowsPerSec={}",
                streamId, totalRows, totalBytes, totalTime.toMillis(),
                String.format("%.2f", throughputMBps),
                String.format("%.0f", rowsPerSec));
    }

    public void logStreamError(
            String streamId,
            long rowsBeforeError,
            Throwable error
    ) {
        log.error("Stream error: id={}, rowsBeforeError={}, error={}",
                streamId, rowsBeforeError, error.getMessage(), error);
    }

    public void logMaterializationForced(
            String reason,
            String capability,
            String action
    ) {
        log.info("Materialization forced: reason={}, capability={}, action={}",
                reason, capability, action);
    }
}

// ============================================================================
// Dashboard Queries (Prometheus/Grafana)
// ============================================================================

/**
 * Example Prometheus queries for monitoring dashboards
 */
public class DashboardQueries {

    // Streaming adoption rate
    public static final String STREAMING_ADOPTION_RATE =
            "rate(pipeline_output_streaming_total[5m]) / " +
                    "(rate(pipeline_output_streaming_total[5m]) + rate(pipeline_output_materialized_total[5m]))";

    // Average time to first byte
    public static final String AVG_FIRST_BYTE_TIME =
            "rate(pipeline_streaming_first_byte_sum[5m]) / " +
                    "rate(pipeline_streaming_first_byte_count[5m])";

    // Streaming error rate
    public static final String STREAMING_ERROR_RATE =
            "rate(pipeline_streaming_errors_total[5m]) / " +
                    "rate(pipeline_output_streaming_total[5m])";

    // Throughput (rows/sec)
    public static final String STREAMING_THROUGHPUT =
            "rate(pipeline_streaming_row_count_sum[5m]) / " +
                    "rate(pipeline_streaming_row_count_count[5m])";

    // Active streams gauge
    public static final String ACTIVE_STREAMS =
            "pipeline_streaming_active";

    // Memory pressure comparison
    public static final String MEMORY_PRESSURE =
            "rate(jvm_memory_used_bytes{area=\"heap\"}[5m])";

    // P95 latency comparison
    public static final String P95_LATENCY_MATERIALIZED =
            "histogram_quantile(0.95, pipeline_execution_time_bucket{mode=\"materialized\"})";

    public static final String P95_LATENCY_STREAMING =
            "histogram_quantile(0.95, pipeline_execution_time_bucket{mode=\"streaming\"})";
}

// ============================================================================
// Unit Tests for Hybrid Streaming
// ============================================================================

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions .*;
        import static org.mockito.Mockito .*;

/**
 * Test OutputStrategy decision logic
 */
class OutputStrategyTest {

    private StreamingConfig config;
    private OutputStrategy strategy;

    @BeforeEach
    void setUp() {
        config = StreamingConfig.defaults();
        strategy = new OutputStrategy(config);
    }

    @Test
    void shouldStream_whenRowCountExceedsThreshold() {
        // Given
        CanonicalInput input = mockInput("query", "artists");
        QueryResult result = mockResult(2000L, false);

        // When
        boolean shouldStream = strategy.shouldStream(input, result);

        // Then
        assertTrue(shouldStream, "Should stream when row count > threshold");
    }

    @Test
    void shouldMaterialize_whenRowCountBelowThreshold() {
        // Given
        CanonicalInput input = mockInput("query", "artists");
        QueryResult result = mockResult(500L, false);

        // When
        boolean shouldStream = strategy.shouldStream(input, result);

        // Then
        assertFalse(shouldStream, "Should materialize when row count < threshold");
    }

    @Test
    void shouldStream_whenHasLargeObjects() {
        // Given
        CanonicalInput input = mockInput("query", "documents");
        QueryResult result = mockResult(100L, true); // Has blobs

        // When
        boolean shouldStream = strategy.shouldStream(input, result);

        // Then
        assertTrue(shouldStream, "Should stream when result has large objects");
    }

    @Test
    void shouldStream_whenExplicitHint() {
        // Given
        CanonicalInput input = mockInputWithHint("query", "artists", "stream", "true");
        QueryResult result = mockResult(100L, false);

        // When
        boolean shouldStream = strategy.shouldStream(input, result);

        // Then
        assertTrue(shouldStream, "Should respect explicit stream hint");
    }

    @Test
    void shouldMaterialize_whenPostProcessorRequiresMaterialization() {
        // Given
        CanonicalInput input = mockInputWithSorting("query", "artists");
        QueryResult result = mockResult(2000L, false);

        // When
        boolean shouldStream = strategy.shouldStream(input, result);

        // Then
        assertFalse(shouldStream, "Should materialize when sorting required");
    }

    private CanonicalInput mockInput(String capability, String action) {
        CanonicalInput input = mock(CanonicalInput.class);
        when(input.getCapability()).thenReturn(capability);
        when(input.getAction()).thenReturn(action);
        when(input.getParameter("stream")).thenReturn(null);
        return input;
    }

    private QueryResult mockResult(Long estimatedRows, boolean hasLargeObjects) {
        QueryResult result = mock(QueryResult.class);
        when(result.getEstimatedRowCount()).thenReturn(estimatedRows);
        when(result.hasLargeObjectColumns()).thenReturn(hasLargeObjects);
        return result;
    }
}

/**
 * Test PipelineExecutor with both output modes
 */
class PipelineExecutorTest {

    private PipelineExecutor executor;
    private StreamingConfig config;

    @Mock
    private PreProcessorChain preProcessors;
    @Mock
    private QueryExecutor queryExecutor;
    @Mock
    private PostProcessorChain postProcessors;

    @BeforeEach
    void setUp() {
        config = new StreamingConfig(1000, 5 * 1024 * 1024, false, Set.of("query"));
        executor = new PipelineExecutor(preProcessors, queryExecutor, postProcessors, config);
    }

    @Test
    void apply_shouldReturnMaterializedOutput_forSmallDataset() {
        // Given
        CanonicalInput input = createInput("query", "artists");
        QueryResult queryResult = createSmallQueryResult(500);

        when(preProcessors.apply(any(), any())).thenReturn(input);
        when(queryExecutor.executeQuery(any(), any())).thenReturn(queryResult);
        when(postProcessors.processBatch(any(), any())).thenAnswer(i -> i.getArgument(0));

        // When
        CanonicalOutput output = executor.apply(input, new ExecutionContext());

        // Then
        assertInstanceOf(MaterializedOutput.class, output);
        MaterializedOutput materialized = (MaterializedOutput) output;
        assertEquals(500, materialized.data().size());
        verify(postProcessors).processBatch(any(), any());
    }

    @Test
    void apply_shouldReturnStreamingOutput_forLargeDataset() {
        // Given
        CanonicalInput input = createInput("query", "large_table");
        QueryResult queryResult = createLargeQueryResult(10000);

        when(preProcessors.apply(any(), any())).thenReturn(input);
        when(queryExecutor.executeQuery(any(), any())).thenReturn(queryResult);

        // When
        CanonicalOutput output = executor.apply(input, new ExecutionContext());

        // Then
        assertInstanceOf(StreamingOutput.class, output);
        StreamingOutput streaming = (StreamingOutput) output;
        assertNotNull(streaming.dataStream());
        assertEquals(10000L, streaming.streamMetadata().estimatedRowCount());
        verify(postProcessors, never()).processBatch(any(), any());
    }

    @Test
    void streaming_shouldApplyRowLevelPostProcessors() {
        // Given
        CanonicalInput input = createInputWithHint("query", "artists", "stream", "true");
        QueryResult queryResult = createQueryResultWithRows(
                List.of(
                        Map.of("id", 1, "name", "Alice", "hidden", "secret"),
                        Map.of("id", 2, "name", "Bob", "hidden", "secret")
                )
        );

        when(preProcessors.apply(any(), any())).thenReturn(input);
        when(queryExecutor.executeQuery(any(), any())).thenReturn(queryResult);
        when(postProcessors.processRow(any(), any())).thenAnswer(invocation -> {
            Map<String, Object> row = invocation.getArgument(0);
            Map<String, Object> filtered = new HashMap<>(row);
            filtered.remove("hidden");
            return filtered;
        });

        // When
        CanonicalOutput output = executor.apply(input, new ExecutionContext());

        // Then
        StreamingOutput streaming = (StreamingOutput) output;
        Iterator<Map<String, Object>> stream = streaming.dataStream();

        Map<String, Object> row1 = stream.next();
        assertFalse(row1.containsKey("hidden"), "Hidden field should be filtered");
        assertEquals("Alice", row1.get("name"));

        verify(postProcessors, times(1)).processRow(any(), any());
    }

    private CanonicalInput createInput(String capability, String action) {
        // Mock implementation
        return null;
    }

    private QueryResult createSmallQueryResult(int rowCount) {
        // Mock implementation
        return null;
    }
}

// ============================================================================
// Integration Tests
// ============================================================================

/**
 * End-to-end integration tests
 */
class StreamingIntegrationTest {

    private TestTransportServer server;
    private TestDatabase database;

    @BeforeEach
    void setUp() {
        database = new TestDatabase();
        database.insertTestData(10000); // 10K rows

        server = new TestTransportServer();
    }

    @Test
    void endToEnd_streamingLargeDataset() throws Exception {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/query/large_table"))
                .header("Accept", "application/json")
                .GET()
                .build();

        // When
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());
        assertEquals("chunked", response.headers().firstValue("Transfer-Encoding").orElse(""));

        String body = response.body();
        assertTrue(body.contains("\"streaming\":true"));

        // Verify all rows received
        int rowCount = countJsonArrayElements(body);
        assertEquals(10000, rowCount);
    }

    @Test
    void endToEnd_materializingSmallDataset() throws Exception {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/query/small_table"))
                .header("Accept", "application/json")
                .GET()
                .build();

        // When
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());

        String body = response.body();
        assertTrue(body.contains("\"streaming\":false"));

        // Should have rowCount in metadata
        assertTrue(body.contains("\"rowCount\":"));
    }

    @Test
    void streaming_shouldHandleMidStreamError() throws Exception {
        // Given: Database will fail after 5000 rows
        database.configureCrashAfter(5000);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/query/large_table"))
                .GET()
                .build();

        // When
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        // Then: Response should be 200 (started successfully)
        assertEquals(200, response.statusCode());

        // But body should contain error marker
        String body = response.body();
        assertTrue(body.contains("\"error\":\"Stream interrupted"));

        // Should have received ~5000 rows before error
        int rowCount = countJsonArrayElements(body);
        assertTrue(rowCount >= 4900 && rowCount <= 5100,
                "Should have ~5000 rows before error");
    }

    private int countJsonArrayElements(String json) {
        // Simple JSON array element counter
        return json.split("\\{\"id\":").length - 1;
    }
}

// ============================================================================
// Load Tests
// ============================================================================

/**
 * Performance and load testing
 */
class StreamingLoadTest {

    @Test
    void loadTest_concurrentStreams() throws Exception {
        int numThreads = 50;
        int rowsPerQuery = 100000;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Start concurrent requests
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    HttpResponse<String> response = makeStreamingRequest();
                    if (response.statusCode() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all requests to complete
        latch.await(5, TimeUnit.MINUTES);
        executor.shutdown();

        // Verify
        assertEquals(numThreads, successCount.get() + failureCount.get());
        assertTrue(successCount.get() > numThreads * 0.95,
                "At least 95% should succeed");

        // Check memory didn't explode
        long usedMemory = Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        double memoryUsage = (double) usedMemory / maxMemory;

        assertTrue(memoryUsage < 0.80,
                "Memory usage should stay below 80%: " + memoryUsage);
    }

    @Test
    void performanceTest_streamingVsMaterialized() {
        int rowCount = 100000;

        // Test materialized
        long materializeStart = System.currentTimeMillis();
        MaterializedOutput materialized = executeMaterializedQuery(rowCount);
        long materializeTime = System.currentTimeMillis() - materializeStart;

        // Test streaming
        long streamStart = System.currentTimeMillis();
        StreamingOutput streaming = executeStreamingQuery(rowCount);
        long firstByteTime = System.currentTimeMillis() - streamStart;

        // Consume stream
        int streamedRows = 0;
        while (streaming.dataStream().hasNext()) {
            streaming.dataStream().next();
            streamedRows++;
        }
        long totalStreamTime = System.currentTimeMillis() - streamStart;

        // Assertions
        assertEquals(rowCount, materialized.data().size());
        assertEquals(rowCount, streamedRows);

        // Streaming should have much faster first byte
        assertTrue(firstByteTime < materializeTime / 10,
                "Streaming first byte should be 10x faster: " +
                        "firstByte=" + firstByteTime + "ms, materialize=" + materializeTime + "ms");

        System.out.println("Materialized: " + materializeTime + "ms");
        System.out.println("Streaming first byte: " + firstByteTime + "ms");
        System.out.println("Streaming total: " + totalStreamTime + "ms");
    }

    private HttpResponse<String> makeStreamingRequest() throws Exception {
        // Implementation
        return null;
    }

    private MaterializedOutput executeMaterializedQuery(int rowCount) {
        // Implementation
        return null;
    }

    private StreamingOutput executeStreamingQuery(int rowCount) {
        // Implementation
        return null;
    }
}

// ============================================================================
// Property-Based Tests
// ============================================================================

/**
 * Property-based tests using jqwik
 */
class StreamingPropertyTest {

    @Property
    void streaming_shouldProduceSameRowsAsMaterialized(
            @ForAll @IntRange(min = 1, max = 10000) int rowCount
    ) {
        // Given
        CanonicalInput input = createInput("query", "test_table");
        List<Map<String, Object>> expectedRows = generateTestRows(rowCount);
        QueryResult queryResult = createQueryResult(expectedRows);

        // When: Execute in materialized mode
        CanonicalOutput materializedOutput =
                executorWithoutStreaming.apply(input, ctx);

        // When: Execute in streaming mode
        CanonicalOutput streamingOutput =
                executorWithStreaming.apply(input, ctx);

        // Then: Should produce identical rows
        List<Map<String, Object>> materializedRows =
                ((MaterializedOutput) materializedOutput).data();

        List<Map<String, Object>> streamedRows = new ArrayList<>();
        StreamingOutput streaming = (StreamingOutput) streamingOutput;
        streaming.dataStream().forEachRemaining(streamedRows::add);

        assertEquals(materializedRows.size(), streamedRows.size());
        assertEquals(materializedRows, streamedRows);
    }

    @Property
    void blobHandling_shouldNotLoadLargeBlobsIntoMemory(
            @ForAll @IntRange(min = 1, max = 100) int numBlobs,
            @ForAll @IntRange(min = 10, max = 100) int blobSizeMB
    ) {
        // Given
        long initialMemory = getUsedMemory();
        List<BlobColumn> blobs = new ArrayList<>();

        // When: Create blob columns (should not load)
        for (int i = 0; i < numBlobs; i++) {
            blobs.add(createLargeBlob(blobSizeMB * 1024 * 1024));
        }

        long afterCreationMemory = getUsedMemory();

        // Then: Memory should not increase significantly
        long memoryIncrease = afterCreationMemory - initialMemory;
        long maxExpectedIncrease = 10 * 1024 * 1024; // 10MB overhead

        assertTrue(memoryIncrease < maxExpectedIncrease,
                "Memory should not increase significantly when creating lazy blobs: " +
                        "increase=" + memoryIncrease);

        // When: Actually open streams
        long beforeOpenMemory = getUsedMemory();
        for (BlobColumn blob : blobs) {
            try (InputStream stream = blob.openStream()) {
                stream.read(); // Read one byte
            }
        }
        long afterOpenMemory = getUsedMemory();

        // Then: Should still be reasonable (streams are lazy)
        memoryIncrease = afterOpenMemory - beforeOpenMemory;
        assertTrue(memoryIncrease < maxExpectedIncrease * 2);
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private List<Map<String, Object>> generateTestRows(int count) {
        // Implementation
        return null;
    }
}

// ============================================================================
// Test Utilities
// ============================================================================

/**
 * Mock query result for testing
 */
class MockQueryResult implements QueryResult {
    private final List<Map<String, Object>> rows;
    private int currentIndex = 0;

    public MockQueryResult(List<Map<String, Object>> rows) {
        this.rows = new ArrayList<>(rows);
    }

    @Override
    public boolean hasNext() {
        return currentIndex < rows.size();
    }

    @Override
    public Map<String, Object> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return rows.get(currentIndex++);
    }

    @Override
    public Long getEstimatedRowCount() {
        return (long) rows.size();
    }

    @Override
    public boolean hasLargeObjectColumns() {
        return rows.stream()
                .flatMap(row -> row.values().stream())
                .anyMatch(v -> v instanceof BlobColumn);
    }

    @Override
    public List<String> getColumnNames() {
        if (rows.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(rows.get(0).keySet());
    }

    @Override
    public long getExecutionTimeMs() {
        return 100;
    }
}

/**
 * Test data builder
 */
class TestDataBuilder {

    public static List<Map<String, Object>> createRows(int count) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(Map.of(
                    "id", i,
                    "name", "Row " + i,
                    "value", Math.random() * 1000
            ));
        }
        return rows;
    }

    public static BlobColumn createBlob(int sizeBytes) {
        byte[] data = new byte[sizeBytes];
        new Random().nextBytes(data);

        return new BlobColumn(
                () -> new ByteArrayInputStream(data),
                sizeBytes,
                "application/octet-stream",
                UUID.randomUUID().toString()
        );
    }

    public static List<Map<String, Object>> createRowsWithBlobs(
            int count,
            int blobSize
    ) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(Map.of(
                    "id", i,
                    "name", "Row " + i,
                    "blob", createBlob(blobSize)
            ));
        }
        return rows;
    }
}

// ============================================================================
// Unit Tests for Hybrid Streaming
// ============================================================================

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions .*;
        import static org.mockito.Mockito .*;

/**
 * Test OutputStrategy decision logic
 */
class OutputStrategyTest {

    private StreamingConfig config;
    private OutputStrategy strategy;

    @BeforeEach
    void setUp() {
        config = StreamingConfig.defaults();
        strategy = new OutputStrategy(config);
    }

    @Test
    void shouldStream_whenRowCountExceedsThreshold() {
        // Given
        CanonicalInput input = mockInput("query", "artists");
        QueryResult result = mockResult(2000L, false);

        // When
        boolean shouldStream = strategy.shouldStream(input, result);

        // Then
        assertTrue(shouldStream, "Should stream when row count > threshold");
    }

    @Test
    void shouldMaterialize_whenRowCountBelowThreshold() {
        // Given
        CanonicalInput input = mockInput("query", "artists");
        QueryResult result = mockResult(500L, false);

        // When
        boolean shouldStream = strategy.shouldStream(input, result);

        // Then
        assertFalse(shouldStream, "Should materialize when row count < threshold");
    }

    @Test
    void shouldStream_whenHasLargeObjects() {
        // Given
        CanonicalInput input = mockInput("query", "documents");
        QueryResult result = mockResult(100L, true); // Has blobs

        // When
        boolean shouldStream = strategy.shouldStream(input, result);

        // Then
        assertTrue(shouldStream, "Should stream when result has large objects");
    }

    @Test
    void shouldStream_whenExplicitHint() {
        // Given
        CanonicalInput input = mockInputWithHint("query", "artists", "stream", "true");
        QueryResult result = mockResult(100L, false);

        // When
        boolean shouldStream = strategy.shouldStream(input, result);

        // Then
        assertTrue(shouldStream, "Should respect explicit stream hint");
    }

    @Test
    void shouldMaterialize_whenPostProcessorRequiresMaterialization() {
        // Given
        CanonicalInput input = mockInputWithSorting("query", "artists");
        QueryResult result = mockResult(2000L, false);

        // When
        boolean shouldStream = strategy.shouldStream(input, result);

        // Then
        assertFalse(shouldStream, "Should materialize when sorting required");
    }

    private CanonicalInput mockInput(String capability, String action) {
        CanonicalInput input = mock(CanonicalInput.class);
        when(input.getCapability()).thenReturn(capability);
        when(input.getAction()).thenReturn(action);
        when(input.getParameter("stream")).thenReturn(null);
        return input;
    }

    private QueryResult mockResult(Long estimatedRows, boolean hasLargeObjects) {
        QueryResult result = mock(QueryResult.class);
        when(result.getEstimatedRowCount()).thenReturn(estimatedRows);
        when(result.hasLargeObjectColumns()).thenReturn(hasLargeObjects);
        return result;
    }
}

/**
 * Test PipelineExecutor with both output modes
 */
class PipelineExecutorTest {

    private PipelineExecutor executor;
    private StreamingConfig config;

    @Mock
    private PreProcessorChain preProcessors;
    @Mock
    private QueryExecutor queryExecutor;
    @Mock
    private PostProcessorChain postProcessors;

    @BeforeEach
    void setUp() {
        config = new StreamingConfig(1000, 5 * 1024 * 1024, false, Set.of("query"));
        executor = new PipelineExecutor(preProcessors, queryExecutor, postProcessors, config);
    }

    @Test
    void apply_shouldReturnMaterializedOutput_forSmallDataset() {
        // Given
        CanonicalInput input = createInput("query", "artists");
        QueryResult queryResult = createSmallQueryResult(500);

        when(preProcessors.apply(any(), any())).thenReturn(input);
        when(queryExecutor.executeQuery(any(), any())).thenReturn(queryResult);
        when(postProcessors.processBatch(any(), any())).thenAnswer(i -> i.getArgument(0));

        // When
        CanonicalOutput output = executor.apply(input, new ExecutionContext());

        // Then
        assertInstanceOf(MaterializedOutput.class, output);
        MaterializedOutput materialized = (MaterializedOutput) output;
        assertEquals(500, materialized.data().size());
        verify(postProcessors).processBatch(any(), any());
    }

    @Test
    void apply_shouldReturnStreamingOutput_forLargeDataset() {
        // Given
        CanonicalInput input = createInput("query", "large_table");
        QueryResult queryResult = createLargeQueryResult(10000);

        when(preProcessors.apply(any(), any())).thenReturn(input);
        when(queryExecutor.executeQuery(any(), any())).thenReturn(queryResult);

        // When
        CanonicalOutput output = executor.apply(input, new ExecutionContext());

        // Then
        assertInstanceOf(StreamingOutput.class, output);
        StreamingOutput streaming = (StreamingOutput) output;
        assertNotNull(streaming.dataStream());
        assertEquals(10000L, streaming.streamMetadata().estimatedRowCount());
        verify(postProcessors, never()).processBatch(any(), any());
    }

    @Test
    void streaming_shouldApplyRowLevelPostProcessors() {
        // Given
        CanonicalInput input = createInputWithHint("query", "artists", "stream", "true");
        QueryResult queryResult = createQueryResultWithRows(
                List.of(
                        Map.of("id", 1, "name", "Alice", "hidden", "secret"),
                        Map.of("id", 2, "name", "Bob", "hidden", "secret")
                )
        );

        when(preProcessors.apply(any(), any())).thenReturn(input);
        when(queryExecutor.executeQuery(any(), any())).thenReturn(queryResult);
        when(postProcessors.processRow(any(), any())).thenAnswer(invocation -> {
            Map<String, Object> row = invocation.getArgument(0);
            Map<String, Object> filtered = new HashMap<>(row);
            filtered.remove("hidden");
            return filtered;
        });

        // When
        CanonicalOutput output = executor.apply(input, new ExecutionContext());

        // Then
        StreamingOutput streaming = (StreamingOutput) output;
        Iterator<Map<String, Object>> stream = streaming.dataStream();

        Map<String, Object> row1 = stream.next();
        assertFalse(row1.containsKey("hidden"), "Hidden field should be filtered");
        assertEquals("Alice", row1.get("name"));

        verify(postProcessors, times(1)).processRow(any(), any());
    }

    private CanonicalInput createInput(String capability, String action) {
        // Mock implementation
        return null;
    }

    private QueryResult createSmallQueryResult(int rowCount) {
        // Mock implementation
        return null;
    }
}

// ============================================================================
// Integration Tests
// ============================================================================

/**
 * End-to-end integration tests
 */
class StreamingIntegrationTest {

    private TestTransportServer server;
    private TestDatabase database;

    @BeforeEach
    void setUp() {
        database = new TestDatabase();
        database.insertTestData(10000); // 10K rows

        server = new TestTransportServer();
    }

    @Test
    void endToEnd_streamingLargeDataset() throws Exception {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/query/large_table"))
                .header("Accept", "application/json")
                .GET()
                .build();

        // When
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());
        assertEquals("chunked", response.headers().firstValue("Transfer-Encoding").orElse(""));

        String body = response.body();
        assertTrue(body.contains("\"streaming\":true"));

        // Verify all rows received
        int rowCount = countJsonArrayElements(body);
        assertEquals(10000, rowCount);
    }

    @Test
    void endToEnd_materializingSmallDataset() throws Exception {
        // Given
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/query/small_table"))
                .header("Accept", "application/json")
                .GET()
                .build();

        // When
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        // Then
        assertEquals(200, response.statusCode());

        String body = response.body();
        assertTrue(body.contains("\"streaming\":false"));

        // Should have rowCount in metadata
        assertTrue(body.contains("\"rowCount\":"));
    }

    @Test
    void streaming_shouldHandleMidStreamError() throws Exception {
        // Given: Database will fail after 5000 rows
        database.configureCrashAfter(5000);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/query/large_table"))
                .GET()
                .build();

        // When
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        // Then: Response should be 200 (started successfully)
        assertEquals(200, response.statusCode());

        // But body should contain error marker
        String body = response.body();
        assertTrue(body.contains("\"error\":\"Stream interrupted"));

        // Should have received ~5000 rows before error
        int rowCount = countJsonArrayElements(body);
        assertTrue(rowCount >= 4900 && rowCount <= 5100,
                "Should have ~5000 rows before error");
    }

    private int countJsonArrayElements(String json) {
        // Simple JSON array element counter
        return json.split("\\{\"id\":").length - 1;
    }
}

// ============================================================================
// Load Tests
// ============================================================================

/**
 * Performance and load testing
 */
class StreamingLoadTest {

    @Test
    void loadTest_concurrentStreams() throws Exception {
        int numThreads = 50;
        int rowsPerQuery = 100000;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Start concurrent requests
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    HttpResponse<String> response = makeStreamingRequest();
                    if (response.statusCode() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all requests to complete
        latch.await(5, TimeUnit.MINUTES);
        executor.shutdown();

        // Verify
        assertEquals(numThreads, successCount.get() + failureCount.get());
        assertTrue(successCount.get() > numThreads * 0.95,
                "At least 95% should succeed");

        // Check memory didn't explode
        long usedMemory = Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        double memoryUsage = (double) usedMemory / maxMemory;

        assertTrue(memoryUsage < 0.80,
                "Memory usage should stay below 80%: " + memoryUsage);
    }

    @Test
    void performanceTest_streamingVsMaterialized() {
        int rowCount = 100000;

        // Test materialized
        long materializeStart = System.currentTimeMillis();
        MaterializedOutput materialized = executeMaterializedQuery(rowCount);
        long materializeTime = System.currentTimeMillis() - materializeStart;

        // Test streaming
        long streamStart = System.currentTimeMillis();
        StreamingOutput streaming = executeStreamingQuery(rowCount);
        long firstByteTime = System.currentTimeMillis() - streamStart;

        // Consume stream
        int streamedRows = 0;
        while (streaming.dataStream().hasNext()) {
            streaming.dataStream().next();
            streamedRows++;
        }
        long totalStreamTime = System.currentTimeMillis() - streamStart;

        // Assertions
        assertEquals(rowCount, materialized.data().size());
        assertEquals(rowCount, streamedRows);

        // Streaming should have much faster first byte
        assertTrue(firstByteTime < materializeTime / 10,
                "Streaming first byte should be 10x faster: " +
                        "firstByte=" + firstByteTime + "ms, materialize=" + materializeTime + "ms");

        System.out.println("Materialized: " + materializeTime + "ms");
        System.out.println("Streaming first byte: " + firstByteTime + "ms");
        System.out.println("Streaming total: " + totalStreamTime + "ms");
    }

    private HttpResponse<String> makeStreamingRequest() throws Exception {
        // Implementation
        return null;
    }

    private MaterializedOutput executeMaterializedQuery(int rowCount) {
        // Implementation
        return null;
    }

    private StreamingOutput executeStreamingQuery(int rowCount) {
        // Implementation
        return null;
    }
}

// ============================================================================
// Property-Based Tests
// ============================================================================

/**
 * Property-based tests using jqwik
 */
class StreamingPropertyTest {

    @Property
    void streaming_shouldProduceSameRowsAsMaterialized(
            @ForAll @IntRange(min = 1, max = 10000) int rowCount
    ) {
        // Given
        CanonicalInput input = createInput("query", "test_table");
        List<Map<String, Object>> expectedRows = generateTestRows(rowCount);
        QueryResult queryResult = createQueryResult(expectedRows);

        // When: Execute in materialized mode
        CanonicalOutput materializedOutput =
                executorWithoutStreaming.apply(input, ctx);

        // When: Execute in streaming mode
        CanonicalOutput streamingOutput =
                executorWithStreaming.apply(input, ctx);

        // Then: Should produce identical rows
        List<Map<String, Object>> materializedRows =
                ((MaterializedOutput) materializedOutput).data();

        List<Map<String, Object>> streamedRows = new ArrayList<>();
        StreamingOutput streaming = (StreamingOutput) streamingOutput;
        streaming.dataStream().forEachRemaining(streamedRows::add);

        assertEquals(materializedRows.size(), streamedRows.size());
        assertEquals(materializedRows, streamedRows);
    }

    @Property
    void blobHandling_shouldNotLoadLargeBlobsIntoMemory(
            @ForAll @IntRange(min = 1, max = 100) int numBlobs,
            @ForAll @IntRange(min = 10, max = 100) int blobSizeMB
    ) {
        // Given
        long initialMemory = getUsedMemory();
        List<BlobColumn> blobs = new ArrayList<>();

        // When: Create blob columns (should not load)
        for (int i = 0; i < numBlobs; i++) {
            blobs.add(createLargeBlob(blobSizeMB * 1024 * 1024));
        }

        long afterCreationMemory = getUsedMemory();

        // Then: Memory should not increase significantly
        long memoryIncrease = afterCreationMemory - initialMemory;
        long maxExpectedIncrease = 10 * 1024 * 1024; // 10MB overhead

        assertTrue(memoryIncrease < maxExpectedIncrease,
                "Memory should not increase significantly when creating lazy blobs: " +
                        "increase=" + memoryIncrease);

        // When: Actually open streams
        long beforeOpenMemory = getUsedMemory();
        for (BlobColumn blob : blobs) {
            try (InputStream stream = blob.openStream()) {
                stream.read(); // Read one byte
            }
        }
        long afterOpenMemory = getUsedMemory();

        // Then: Should still be reasonable (streams are lazy)
        memoryIncrease = afterOpenMemory - beforeOpenMemory;
        assertTrue(memoryIncrease < maxExpectedIncrease * 2);
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private List<Map<String, Object>> generateTestRows(int count) {
        // Implementation
        return null;
    }
}

// ============================================================================
// Test Utilities
// ============================================================================

/**
 * Mock query result for testing
 */
class MockQueryResult implements QueryResult {
    private final List<Map<String, Object>> rows;
    private int currentIndex = 0;

    public MockQueryResult(List<Map<String, Object>> rows) {
        this.rows = new ArrayList<>(rows);
    }

    @Override
    public boolean hasNext() {
        return currentIndex < rows.size();
    }

    @Override
    public Map<String, Object> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return rows.get(currentIndex++);
    }

    @Override
    public Long getEstimatedRowCount() {
        return (long) rows.size();
    }

    @Override
    public boolean hasLargeObjectColumns() {
        return rows.stream()
                .flatMap(row -> row.values().stream())
                .anyMatch(v -> v instanceof BlobColumn);
    }

    @Override
    public List<String> getColumnNames() {
        if (rows.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(rows.get(0).keySet());
    }

    @Override
    public long getExecutionTimeMs() {
        return 100;
    }
}

/**
 * Test data builder
 */
class TestDataBuilder {

    public static List<Map<String, Object>> createRows(int count) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(Map.of(
                    "id", i,
                    "name", "Row " + i,
                    "value", Math.random() * 1000
            ));
        }
        return rows;
    }

    public static BlobColumn createBlob(int sizeBytes) {
        byte[] data = new byte[sizeBytes];
        new Random().nextBytes(data);

        return new BlobColumn(
                () -> new ByteArrayInputStream(data),
                sizeBytes,
                "application/octet-stream",
                UUID.randomUUID().toString()
        );
    }

    public static List<Map<String, Object>> createRowsWithBlobs(
            int count,
            int blobSize
    ) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(Map.of(
                    "id", i,
                    "name", "Row " + i,
                    "blob", createBlob(blobSize)
            ));
        }
        return rows;
    }
}
