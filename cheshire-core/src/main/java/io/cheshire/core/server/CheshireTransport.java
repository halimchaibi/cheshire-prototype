package io.cheshire.core.server;

/**
 * Represents the physical network infrastructure (e.g., Jetty Engine, Netty Boss/Worker).
 * It is responsible for the socket lifecycle and thread management.
 */
public interface CheshireTransport {

    /**
     * Starts the physical server/engine if not already running.
     */
    void start() throws Exception;

    /**
     * Stops the physical engine (usually reference-counted).
     */
    void stop() throws Exception;

    /**
     * Registers a content that will be manager by this container
     */
    void register(Object content);

    /**
     * Returns true if the hardware socket is open and listening.
     */
    boolean isRunning();

    void attach();

    enum Binding {
        HTTP_JSON,
        MCP_JSON_RPC,
        MCP_STDIO;

        public static CheshireTransport.Binding from(String value) {
            return switch (value.toLowerCase()) {
                case "http-json" -> HTTP_JSON;
                case "mcp-json-rpc" -> MCP_JSON_RPC;
                case "mcp-stdio" -> MCP_STDIO;
                default -> throw new IllegalArgumentException("Unknown binding: " + value);
            };
        }

        public static boolean requiresNetwork(Binding value) {
            return switch (value) {
                case HTTP_JSON, MCP_JSON_RPC -> Boolean.TRUE;
                case MCP_STDIO -> Boolean.FALSE;
            };
        }
    }
}
