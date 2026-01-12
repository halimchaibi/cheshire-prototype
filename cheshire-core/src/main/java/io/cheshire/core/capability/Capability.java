package io.cheshire.core.capability;

import io.cheshire.core.config.ActionsConfig;
import io.cheshire.core.config.CheshireConfig;
import io.cheshire.spi.pipeline.*;

import java.util.List;
import java.util.Map;

/**
 * Represents a self-contained business capability within the Cheshire framework.
 * <p>
 * <strong>What is a Capability?</strong>
 * <p>
 * A capability is a cohesive business domain that federates:
 * <ul>
 *   <li><strong>Data Sources:</strong> Databases, APIs, file systems</li>
 *   <li><strong>Actions:</strong> Operations (tools/endpoints) exposed to clients</li>
 *   <li><strong>Pipelines:</strong> Processing logic (preprocess → execute → postprocess)</li>
 *   <li><strong>Exposure:</strong> How it's accessed (REST API, MCP stdio, MCP HTTP)</li>
 *   <li><strong>Transport:</strong> Network protocol (HTTP, stdio)</li>
 * </ul>
 * <p>
 * <strong>Examples:</strong>
 * <ul>
 *   <li><strong>Authors:</strong> Manages author CRUD operations</li>
 *   <li><strong>Articles:</strong> Handles article publishing, search, and retrieval</li>
 *   <li><strong>Comments:</strong> Manages article comments and moderation</li>
 * </ul>
 * <p>
 * <strong>Architecture:</strong>
 * Each capability is independently deployable and can be exposed through
 * multiple protocols simultaneously. The framework routes requests to the
 * appropriate capability based on the request context.
 * <p>
 * <strong>Immutability:</strong>
 * This record is immutable by design, ensuring thread-safety and preventing
 * accidental modifications during runtime.
 *
 * @param name        unique identifier for this capability (e.g., "blogrest", "authors")
 * @param description human-readable description of the capability's purpose
 * @param domain      business domain this capability belongs to (e.g., "blog", "ecommerce")
 * @param exposure    configuration for how this capability is exposed (REST, MCP)
 * @param transport   network transport configuration (HTTP, stdio)
 * @param sources     list of data source names this capability uses
 * @param engine      query engine name used for data operations
 * @param pipelines   map of action names to their processing pipelines
 * @param actions     configuration of all actions (tools/endpoints) in this capability
 * @see io.cheshire.core.manager.CapabilityManager
 * @see ActionsConfig
 * @see CheshireConfig.Exposure
 * @see CheshireConfig.Transport
 * @since 1.0.0
 */
public record Capability(
        String name,
        String description,
        String domain,
        CheshireConfig.Exposure exposure,
        CheshireConfig.Transport transport,
        List<String> sources,
        String engine,
        Map<String, PipelineProcessor<CanonicalInput<?>, CanonicalOutput<?>>> pipelines,
        ActionsConfig actions
) {
}
