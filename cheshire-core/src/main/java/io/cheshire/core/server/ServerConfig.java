package io.cheshire.core.server;

/**
 * <h1>ServerConfig</h1>
 *
 * <p>The <b>Standardized Configuration Contract</b> for Cheshire jetty instances.
 * This interface defines the minimum metadata required to identify, name, and
 * configure a specific protocol listener (e.g., MCP-STDIO, HTTP-SSE).</p>
 *
 *
 *
 * <h3>Architectural Role</h3>
 * <p>In a multi-jetty environment, the {@code ServerConfig} allows the
 * bootstrap logic to determine which {@code CheshireServer} implementation to
 * instantiate based on the {@link #type()} identifier. It preserves the
 * <b>Raw Configuration</b> to ensure that custom implementation-specific
 * properties remain accessible to the specialized jetty builders.</p>
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 * <li><b>Type Mapping:</b> Correlates the configuration block with a specific
 * {@code CheshireServer} provider.</li>
 * <li><b>Identity:</b> Provides an optional logical name for logging,
 * metrics, and debugging multi-jetty deployments.</li>
 * <li><b>Data Integrity:</b> Exposes an immutable view of the underlying
 * configuration parameters.</li>
 * </ul>
 *
 * <pre>{@code
 * // Example: Accessing config in a Server Provider
 * public CheshireServer create(ServerConfig config) {
 * String serverName = config.name().orElse("Default-" + config.type());
 * int port = (int) config.raw().getOrDefault("port", 8080);
 * return new MyServer(serverName, port);
 * }
 * }</pre>
 *
 * @author Cheshire Framework
 * @since 1.0.0
 */
public interface ServerConfig {

    /**
     * Returns the unique protocol or transport identifier.
     * <p>This value must match the identifier returned by the corresponding
     * jetty provider's type check (e.g., "REST", "MCP-STDIO", "MCP-SSE").</p>
     *
     * @return The mandatory protocol type string.
     */
    String type();

    /**
     * Returns an optional logical name for the jetty instance.
     * <p>Useful in scenarios where multiple servers of the same type are
     * running simultaneously (e.g., two different MCP servers on different ports).</p>
     *
     * @return An {@link java.util.Optional} containing the jetty name.
     */
    java.util.Optional<String> name();

    /**
     * Returns the raw, unmodifiable configuration map.
     * <p>This map contains all properties parsed from the configuration source,
     * allowing protocol-specific implementations to extract the values they need
     * using a {@code ConfigAdapter}.</p>
     *
     * @return A read-only {@link java.util.Map} of configuration parameters.
     */
    java.util.Map<String, Object> raw();
}
