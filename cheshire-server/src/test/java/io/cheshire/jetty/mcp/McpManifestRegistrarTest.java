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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.cheshire.core.capability.Capability;
import io.cheshire.core.config.ActionsConfig;
import io.cheshire.core.config.CheshireConfig;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class McpManifestRegistrarTest {

  @Test
  void registersPresentSectionsWhenOptionalSectionsAreMissing() {
    final McpAsyncServer server = mock(McpAsyncServer.class);
    when(server.addTool(any(McpServerFeatures.AsyncToolSpecification.class)))
        .thenReturn(Mono.empty());
    when(server.addResource(any(McpServerFeatures.AsyncResourceSpecification.class)))
        .thenReturn(Mono.empty());

    final McpManifestRegistrar registrar =
        McpManifestRegistrar.of(
            capabilityWithMissingOptionalSections(), null, server, mock(McpProtocolAdapter.class));

    registrar.register();

    verify(server).addTool(any(McpServerFeatures.AsyncToolSpecification.class));
    verify(server).addResource(any(McpServerFeatures.AsyncResourceSpecification.class));
    verify(server, never()).addPrompt(any(McpServerFeatures.AsyncPromptSpecification.class));
    verify(server, never())
        .addResourceTemplate(any(McpServerFeatures.AsyncResourceTemplateSpecification.class));
  }

  private Capability capabilityWithMissingOptionalSections() {
    final ActionsConfig actions = new ActionsConfig();
    actions.tools = List.of(tool());
    actions.resources = List.of(resource());

    final CheshireConfig.Exposure exposure = new CheshireConfig.Exposure();
    exposure.setBinding("mcp-stdio");

    return new Capability(
        "os",
        "OS observability",
        "observability",
        exposure,
        new CheshireConfig.Transport(),
        List.of("os"),
        "calcite",
        Map.of(),
        actions);
  }

  private ActionsConfig.Tool tool() {
    final ActionsConfig.Tool tool = new ActionsConfig.Tool();
    tool.name = "list_system_info";
    tool.description = "Return host-level system metadata.";
    tool.inputSchema = Map.of("type", "object", "properties", Map.of());
    tool.metadata = Map.of();
    return tool;
  }

  private ActionsConfig.Resource resource() {
    final ActionsConfig.Resource resource = new ActionsConfig.Resource();
    resource.uri = "cheshire://os/tables";
    resource.name = "os_tables";
    resource.description = "OS table catalog.";
    resource.mimeType = "application/json";
    resource.metadata = Map.of();
    return resource;
  }
}
