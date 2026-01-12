package io.cheshire.core;

import io.cheshire.common.utils.MapUtils;
import io.cheshire.core.constant.Key;
import io.cheshire.core.server.protocol.RequestContext;
import io.cheshire.core.server.protocol.RequestEnvelope;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * <h1>TaskBuilder</h1>
 *
 * <p>A <b>Domain Mapper</b> that transforms a protocol-level {@link RequestEnvelope}
 * into an execution-ready {@link SessionTask}. This builder handles the "flattening"
 * of disparate data sources—routing info, payload arguments, and session context—into
 * a unified task structure.</p>
 *
 *
 *
 * <h3>The Extraction Strategy</h3>
 * <ul>
 * <li><b>Argument Extraction:</b> Merges the primary {@code payload.data()} (under the "payload" key)
 * with the flattened {@code payload.parameters()} map.</li>
 * <li><b>Context Extraction:</b> Captures essential traceability (requestId, receivedAt)
 * and routing (capability, action) metadata, merging them with the mutable {@link RequestContext} attributes.</li>
 * </ul>
 *
 * <h3>Design Invariants</h3>
 * <ul>
 * <li><b>Immutability:</b> The resulting {@code SessionTask} is populated with unmodifiable maps
 * via {@link java.util.Map#copyOf(Map)}, ensuring that the task state remains stable during execution.</li>
 * <li><b>Null Safety:</b> Gracefully handles null payloads or contexts by initializing empty maps.</li>
 * </ul>
 *
 * <pre>{@code
 * // Standard usage in a Dispatcher or Adapter
 * SessionTask task = TaskBuilder.from(envelope).build();
 *
 * // The task now contains:
 * // data: { "payload": Object, "arg1": "val1", ... }
 * // metadata: { "requestId": "...", "userId": "...", "capability": "..." }
 * }</pre>
 *
 * @author Cheshire Framework
 * @since 1.0.0
 */
public class TaskBuilder {

    /**
     * Internal reference to the source envelope.
     */
    private final RequestEnvelope envelope;

    private TaskBuilder(RequestEnvelope envelope) {
        this.envelope = envelope;
    }

    /**
     * Entry point for the builder.
     * * @param envelope The protocol-level request container.
     *
     * @return A new TaskBuilder instance.
     */
    public static TaskBuilder from(RequestEnvelope envelope) {
        return new TaskBuilder(envelope);
    }

    /**
     * Finalizes the conversion into a {@link SessionTask}.
     * <p>This method executes the internal mapping logic for arguments and context.</p>
     *
     * @return A fully populated, immutable SessionTask.
     */
    public SessionTask build() {
        return new SessionTask(
                extractData(),
                extractMetadata()
        );
    }

    /**
     * Flattens payload data and parameters into the primary data map.
     */
    private java.util.Map<String, Object> extractData() {
        java.util.Map<String, Object> args = new java.util.HashMap<>();

        if (envelope.payload() != null) {
            args.put(Key.PAYLOAD_DATA.key(), envelope.payload().data());
            args.put(Key.PAYLOAD_PARAMETERS.key(), envelope.payload().parameters());
        }
        return java.util.Map.copyOf(args);
    }

    /**
     * Extracts traceability and session-specific attributes into the metadata map.
     */
    private java.util.Map<String, Object> extractMetadata() {
        Map<String, Object> meta = new HashMap<>();
        MapUtils.putNested(meta, Key.DEBUG.key(), "task-started-at", Instant.now());

        meta.put(Key.ACTION.key(), envelope.action());
        meta.put(Key.CAPABILITY.key(), envelope.capability());
        meta.putAll(envelope.payload().metadata());

        if (envelope.context() != null) {
            var context = envelope.context();
            if (context.userId() != null) meta.put(Key.USER_ID.key(), context.userId());
            MapUtils.putNested(meta, Key.DEBUG.key(), Key.DEBUG_CTX_TASK.key(), context);
        }
        return java.util.Map.copyOf(meta);
    }
}
