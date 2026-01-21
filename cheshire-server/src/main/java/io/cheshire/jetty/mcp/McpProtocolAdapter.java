/*-
 * #%L
 * Cheshire :: Servers
 * %%
 * Copyright (C) 2026 Halim Chaibi
 * %%
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * #L%
 */

package io.cheshire.jetty.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cheshire.core.server.ResponseEntity;
import io.cheshire.core.server.protocol.*;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class McpProtocolAdapter implements ProtocolAdapter<McpSchema.Request, McpSchema.Result> {

  private final AtomicReference<String> sessionId =
      new AtomicReference<>(UUID.randomUUID().toString());

  private record ParsedUri(String pipelineName, Map<String, Object> params) {}

  public static final ObjectMapper mapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  // .enable(SerializationFeature.INDENT_OUTPUT);

  /**
   * Maps an external protocol request to the internal {@link RequestEnvelope}.
   *
   * @param request of McpSchema.Request
   */
  @Override
  public RequestEnvelope toRequestEnvelope(McpSchema.Request request, String capability)
      throws ProtocolAdapterException {

    return switch (request) {
      case McpSchema.CallToolRequest(var name, var arguments, var meta) ->
          new RequestEnvelope(
              UUID.randomUUID().toString(),
              // TODO: this can be confusing what had been defined as capability in Cheshire is
              // basically a domain
              capability,
              name, // Action (The specific tool name)
              ProtocolMetadata.ofMcp(McpSchema.METHOD_TOOLS_CALL),
              RequestPayload.of(arguments, meta),
              // TODO: Empty context for now, needs to be fully created here.
              RequestContext.empty(),
              Instant.now());
      case McpSchema.ReadResourceRequest(var uri, var meta) -> {
        ParsedUri parsed = parseMcpUri(uri);

        yield new RequestEnvelope(
            UUID.randomUUID().toString(),
            capability,
            parsed.pipelineName(),
            ProtocolMetadata.ofMcp(McpSchema.METHOD_RESOURCES_READ),
            RequestPayload.of(parsed.params(), meta),
            RequestContext.empty(),
            Instant.now());
      }
      case McpSchema.GetPromptRequest(var name, var arguments, var meta) ->
          new RequestEnvelope(
              UUID.randomUUID().toString(),
              capability,
              name,
              ProtocolMetadata.ofMcp(McpSchema.METHOD_PROMPT_GET),
              RequestPayload.of(arguments, meta),
              // TODO: Empty context for now, needs to be fully created here.
              RequestContext.empty(),
              Instant.now());
      case McpSchema.InitializeRequest(
              var protocolVersion,
              var capabilities,
              var clientInfo,
              var meta) -> {
        // TODO: Weird stuff. Just to test session management
        yield new RequestEnvelope(
            UUID.randomUUID().toString(),
            capability,
            McpSchema.METHOD_INITIALIZE,
            ProtocolMetadata.ofMcp(McpSchema.METHOD_INITIALIZE),
            RequestPayload.of(
                Map.of("client", clientInfo.name(), "sessionId", sessionId), Map.of()),
            // TODO: Empty context for now, needs to be fully created here.
            RequestContext.empty(),
            Instant.now());
      }
      default ->
          throw new ProtocolAdapterException(
              "Unsupported MCP request type: " + request.getClass().getName());
    };
  }

  @Override
  public <T> T fromProcessingResult(McpSchema.Request request, ResponseEntity result)
      throws ProtocolAdapterException {
    return (T)
        switch (request) {
          case McpSchema.CallToolRequest req -> toResultTool(req, result);
          case McpSchema.ReadResourceRequest req -> toReadResourceResult(req, result);
          case McpSchema.GetPromptRequest req -> toGetPromptResult(req, result);
          default ->
              throw new IllegalArgumentException("Unknown MCP request type: " + request.getClass());
        };
  }

  /**
   * Maps the internal {@link ResponseEntity} to the protocol-specific response.
   *
   * @param result of ResponseEntity
   * @return McpSchema.CallToolResult
   */
  // private McpSchema.CallToolResult toResultTool(McpSchema.CallToolRequest request, ResponseEntity
  // result) {
  // return switch (result) {
  // case ResponseEntity.Success(var data, var metadata) -> McpSchema.CallToolResult.builder()
  // // TODO: Assume the ResponseEntity data Object can be constructed by MCP otherwise fallback to
  // Standard text
  // response
  // .structuredContent(data)
  // .meta(metadata)
  // .isError(false)
  // .build();
  // case ResponseEntity.Failure(var status, _, String message) ->
  // McpSchema.CallToolResult.builder()
  // .structuredContent(Map.of(status, message))
  // .isError(true)
  // .build();
  // };
  // }
  private McpSchema.CallToolResult toResultTool(
      McpSchema.CallToolRequest request, ResponseEntity result) {
    return switch (result) {
      case ResponseEntity.Success(var data, var metadata) -> {
        String dataString;
        try {
          dataString = mapper.writeValueAsString(data);
        } catch (Exception e) {
          dataString = String.valueOf(data);
        }

        yield McpSchema.CallToolResult.builder()
            .content(List.of(new McpSchema.TextContent(dataString)))
            .structuredContent(data)
            .meta(metadata)
            .isError(false)
            .build();
      }
      case ResponseEntity.Failure(var status, _, String message) ->
          McpSchema.CallToolResult.builder()
              .content(List.of(new McpSchema.TextContent("Error: " + message)))
              .isError(true)
              .build();
    };
  }

  /**
   * Maps your internal ResponseEntity to the MCP Resource result. A Resource result expects a list
   * of ResourceContents.
   */
  public McpSchema.ReadResourceResult toReadResourceResult(
      McpSchema.ReadResourceRequest req, ResponseEntity result) {
    return switch (result) {
      case ResponseEntity.Success(var data, _) -> {
        McpSchema.ResourceContents content =
            new McpSchema.TextResourceContents(req.uri(), null, String.valueOf(data));
        yield new McpSchema.ReadResourceResult(List.of(content));
      }
      case ResponseEntity.Failure(var status, _, var msg) ->
          throw new RuntimeException("Resource access denied [" + status + "]: " + msg);
    };
  }

  /**
   * Maps your internal ResponseEntity to the MCP Prompt result. A Prompt result expects a list of
   * Messages (role + content).
   */
  public McpSchema.GetPromptResult toGetPromptResult(
      McpSchema.GetPromptRequest request, ResponseEntity result) {
    return switch (result) {
      case ResponseEntity.Success(var data, _) -> {
        McpSchema.PromptMessage message =
            new McpSchema.PromptMessage(
                McpSchema.Role.USER, new McpSchema.TextContent(String.valueOf(data)));
        yield new McpSchema.GetPromptResult("Generated Prompt", List.of(message), null);
      }
      case ResponseEntity.Failure(var status, _, String message) ->
          throw new RuntimeException("Prompt Generation Failed [" + status + "]: " + message);
    };
  }

  /** Get the protocol type this adapter handles. */
  @Override
  public String getProtocolType() {
    return "MCP";
  }

  /** Get the content types this adapter supports. */
  @Override
  public String[] getSupportedContentTypes() {
    return ProtocolAdapter.super.getSupportedContentTypes();
  }

  private ParsedUri parseMcpUri(String uri) {
    if (!uri.contains("?")) {
      return new ParsedUri(uri, Map.of("uri", uri));
    }

    String[] parts = uri.split("\\?", 2);
    String pipelineName = parts[0];
    Map<String, Object> params = new java.util.HashMap<>();
    params.put("uri", uri); // Preserve full URI for traceability

    String query = parts[1];
    for (String pair : query.split("&")) {
      String[] kv = pair.split("=", 2);
      if (kv.length == 2) {
        String key = decode(kv[0]);
        Object value = tryParse(decode(kv[1]));
        params.put(key, value);
      }
    }
    return new ParsedUri(pipelineName, params);
  }

  private String decode(String value) {
    try {
      return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
    } catch (Exception e) {
      return value;
    }
  }

  private Object tryParse(String value) {
    if (value == null || value.isBlank()) return value;
    if (value.equalsIgnoreCase("true")) return true;
    if (value.equalsIgnoreCase("false")) return false;

    try {
      if (value.contains(".")) return Double.parseDouble(value);
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return value;
    }
  }
}
