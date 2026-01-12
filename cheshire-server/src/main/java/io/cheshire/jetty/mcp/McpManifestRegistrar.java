package io.cheshire.jetty.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cheshire.core.capability.Capability;
import io.cheshire.core.config.ActionsConfig;
import io.cheshire.core.server.CheshireDispatcher;
import io.cheshire.core.server.protocol.ProtocolAdapterException;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

//TODO: Refactor to split the tool / resource / prompt registration into separate classes to reduce complexity
@Slf4j
public record McpManifestRegistrar(
        Capability capability,
        CheshireDispatcher dispatcher,
        McpAsyncServer server,
        McpProtocolAdapter protocolAdapter
) {

    public static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static McpManifestRegistrar of(
            Capability capability,
            CheshireDispatcher dispatcher,
            McpAsyncServer server,
            McpProtocolAdapter protocolAdapter
    ) {
        return new McpManifestRegistrar(
                capability,
                dispatcher,
                server,
                protocolAdapter
        );
    }

    public void register() {
        log.debug("Registering capability: {}", capability.name());

        if (capability.actions().getTools() != null) {
            registerTools(capability.actions().getTools(), server);
        }

        if (capability.actions().getResources() != null) {
            registerResources(capability.actions().getResources(), server);
        }

        if (capability.actions().getPrompts() != null) {
            registerPrompts(capability.actions().getPrompts(), server);
        }

        if (capability.actions().getResourceTemplates() != null) {
            registerResourceTemplates(capability.actions().getResourceTemplates(), server);
        }

        log.info("Capability '{}' registered with {} tools, {} resources, {} resource templates, and {} prompts",
                capability.name(),
                capability.actions().tools.size(),
                capability.actions().resources.size(),
                capability.actions().resourceTemplates.size(),
                capability.actions().prompts.size());
    }

    private void registerTools(List<ActionsConfig.Tool> tools, McpAsyncServer server) {
        for (ActionsConfig.Tool tool : tools) {
            try {
                McpServerFeatures.AsyncToolSpecification toolSpec = createToolSpecification(tool);
                server.addTool(toolSpec)
                        .doOnSuccess(v -> log.info("Successfully bound tool: {}", tool.name))
                        .doOnError(e -> log.error("Failed to bind tool '{}' for capability '{}'", tool.name, capability.name(), e))
                        .subscribe(); //Welcome to the hell, with reactive programming. Nothing will happen until we subscribe; the addTool returns a Mono.
                log.debug("Registered tool: {}", tool.name);
            } catch (Exception e) {
                log.error("Failed to register tool '{}' for capability '{}' during specification creation", tool.name, capability.name(), e);
            }
        }
    }

    private McpServerFeatures.AsyncToolSpecification createToolSpecification(ActionsConfig.Tool tool) {
        McpSchema.Tool mcpTool = McpSchema.Tool.builder()
                .name(tool.name)
                .description(tool.description)
                .inputSchema(SchemaConverter.convertToJsonSchema(tool.inputSchema))
                .meta(tool.metadata)
                .build();

        return new McpServerFeatures.AsyncToolSpecification.Builder()
                .tool(mcpTool)
                .callHandler((exchange, request) -> {
                    return Mono.fromCallable(() -> dispatcher.dispatch(protocolAdapter.toRequestEnvelope(request, capability.name())))
                            .subscribeOn(Schedulers.boundedElastic())
                            .handle((response, sink) -> {
                                try {
                                    McpSchema.CallToolResult result = protocolAdapter.fromProcessingResult(request, response);
                                    log.debug(mapper.writeValueAsString(result));
                                    sink.next(result);
                                } catch (Exception e) {
                                    log.debug("Error during tool response handling", e);
                                    sink.error(new RuntimeException("Protocol mapping failed for tool request '%s' in capability '%s': %s".formatted(request.name(), capability.name(), e.getMessage()), e));
                                }
                            });
                })
                .build();
    }

    private void registerResources(List<ActionsConfig.Resource> resources, McpAsyncServer server) {
        for (ActionsConfig.Resource resource : resources) {
            try {
                McpServerFeatures.AsyncResourceSpecification resourceSpec = createResourceSpecification(resource);
                server.addResource(resourceSpec)
                        .doOnSuccess(v -> log.info("Successfully bound resource: {}", resource.name))
                        .doOnError(e -> log.error("Failed to bind resource '{}' for capability '{}'", resource.name, capability.name(), e))
                        .subscribe();
                log.debug("Registered resource: {}", resource.name);
            } catch (Exception e) {
                log.error("Failed to register resource '{}' for capability '{}' during specification creation", resource.name, capability.name(), e);
            }
        }
    }

    private McpServerFeatures.AsyncResourceSpecification createResourceSpecification(ActionsConfig.Resource resource) {
        McpSchema.Resource mcpResource = McpSchema.Resource.builder()
                .name(resource.name)
                .uri(resource.uri)
                .description(resource.description)
                .mimeType(resource.mimeType)
                .build();

        return new McpServerFeatures.AsyncResourceSpecification(
                mcpResource,
                (exchange, request) ->
                        Mono.fromCallable(() -> dispatcher.dispatch(protocolAdapter.toRequestEnvelope(request, capability.name())))
                                .subscribeOn(Schedulers.boundedElastic())
                                .handle((response, sink) -> {
                                    try {
                                        McpSchema.ReadResourceResult result = protocolAdapter.fromProcessingResult(request, response);
                                        sink.next(result);
                                    } catch (ProtocolAdapterException e) {
                                        sink.error(new RuntimeException("Protocol mapping failed for resource request '%s' in capability '%s': %s".formatted(request.uri(), capability.name(), e.getMessage()), e));
                                    }
                                })

        );

    }

    private void registerPrompts(List<ActionsConfig.Prompt> prompts, McpAsyncServer server) {
        for (ActionsConfig.Prompt prompt : prompts) {
            try {
                McpServerFeatures.AsyncPromptSpecification promptSpec = createPromptSpecification(prompt);
                server.addPrompt(promptSpec)
                        .doOnSuccess(v -> log.info("Successfully bound prompt: {}", prompt.name))
                        .doOnError(e -> log.error("Failed to bind prompt '{}' for capability '{}'", prompt.name, capability.name(), e))
                        .subscribe();
            } catch (Exception e) {
                log.error("Failed to register prompt '{}' for capability '{}' during specification creation", prompt.name, capability.name(), e);
            }
        }
    }

    private McpServerFeatures.AsyncPromptSpecification createPromptSpecification(ActionsConfig.Prompt prompt) {
        List<McpSchema.PromptArgument> mcpArgs = (prompt.arguments != null)
                ? prompt.arguments.stream()
                .map(arg -> new McpSchema.PromptArgument(arg.name, arg.description, arg.required))
                .toList()
                : new ArrayList<>();

        McpSchema.Prompt mcpPrompt = new McpSchema.Prompt(
                prompt.name,
                null, // title
                prompt.description,
                mcpArgs
        );

        return new McpServerFeatures.AsyncPromptSpecification(
                mcpPrompt,
                (exchange, request) -> Mono.fromCallable(() ->
                                dispatcher.dispatch(protocolAdapter.toRequestEnvelope(request, capability.name()))
                        )
                        .subscribeOn(Schedulers.boundedElastic())
                        .handle(
                                (response, sink) -> {
                                    try {
                                        McpSchema.GetPromptResult result = protocolAdapter.fromProcessingResult(request, response);
                                        sink.next(result);
                                    } catch (ProtocolAdapterException e) {
                                        sink.error(new RuntimeException("Protocol mapping failed for prompt request '%s' in capability '%s': %s".formatted(request.name(), capability.name(), e.getMessage()), e));
                                    }
                                }
                        )
        );
    }

    private void registerResourceTemplates(List<ActionsConfig.ResourceTemplate> templates, McpAsyncServer server) {
        for (ActionsConfig.ResourceTemplate template : templates) {
            try {
                var spec = createResourceTemplateSpecification(template);
                server.addResourceTemplate(spec)
                        .doOnSuccess(v -> log.info("Successfully bound resource template: {}", template.name))
                        .doOnError(e -> log.error("Failed to bind resource template '{}'", template.name, e))
                        .subscribe(); // The reactive subscription
            } catch (Exception e) {
                log.error("Failed to register resource template '{}' during specification creation", template.name, e);
            }
        }
    }

    private McpServerFeatures.AsyncResourceTemplateSpecification createResourceTemplateSpecification(ActionsConfig.ResourceTemplate template) {
        McpSchema.ResourceTemplate mcpTemplate = McpSchema.ResourceTemplate.builder()
                .uriTemplate(template.uriTemplate)
                .name(template.name)
                .description(template.description)
                .mimeType(template.mimeType)
                .meta(template.metadata) // Assuming your metadata maps to the internal meta Map
                .build();
        return new McpServerFeatures.AsyncResourceTemplateSpecification(
                mcpTemplate,
                (exchange, request) ->
                        Mono.fromCallable(() -> {
                                    return dispatcher.dispatch(
                                            protocolAdapter.toRequestEnvelope(request, capability.name())
                                    );
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                                .handle((response, sink) -> {
                                    try {
                                        // Map the pipeline processing result back to an MCP ReadResourceResult
                                        McpSchema.ReadResourceResult result = protocolAdapter.fromProcessingResult(request, response);
                                        sink.next(result);
                                    } catch (ProtocolAdapterException e) {
                                        sink.error(new RuntimeException(
                                                "Protocol mapping failed for resource template request '%s' in capability '%s': %s"
                                                        .formatted(request.uri(), capability.name(), e.getMessage()), e));
                                    }
                                })
        );
    }
}

