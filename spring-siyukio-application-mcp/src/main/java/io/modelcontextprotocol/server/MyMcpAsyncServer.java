package io.modelcontextprotocol.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siyukio.tools.util.IdUtils;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.util.Utils;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * @author Bugee
 */
@Slf4j
public class MyMcpAsyncServer extends McpAsyncServer {

    private final McpServerTransportProvider mcpTransportProvider;

    private final ObjectMapper objectMapper;

    private final McpSchema.ServerCapabilities serverCapabilities;

    private final McpSchema.Implementation serverInfo;

    private final String instructions;

    private final CopyOnWriteArrayList<McpServerFeatures.AsyncToolSpecification> tools = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<McpSchema.ResourceTemplate> resourceTemplates = new CopyOnWriteArrayList<>();

    private final ConcurrentHashMap<String, McpServerFeatures.AsyncResourceSpecification> resources = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, McpServerFeatures.AsyncPromptSpecification> prompts = new ConcurrentHashMap<>();

    // FIXME: this field is deprecated and should be remvoed together with the
    // broadcasting loggingNotification.
    private McpSchema.LoggingLevel minLoggingLevel = McpSchema.LoggingLevel.DEBUG;
    private List<String> protocolVersions = List.of(McpSchema.LATEST_PROTOCOL_VERSION);

    MyMcpAsyncServer(McpServerTransportProvider mcpTransportProvider, ObjectMapper objectMapper,
                     McpServerFeatures.Async features) {
        this.mcpTransportProvider = mcpTransportProvider;
        this.objectMapper = objectMapper;
        this.serverInfo = features.serverInfo();
        this.serverCapabilities = features.serverCapabilities();
        this.instructions = features.instructions();
        this.tools.addAll(features.tools());
        this.resources.putAll(features.resources());
        this.resourceTemplates.addAll(features.resourceTemplates());
        this.prompts.putAll(features.prompts());

        Map<String, McpServerSession.RequestHandler<?>> requestHandlers = new HashMap<>();

        // Initialize request handlers for standard MCP methods

        // Ping MUST respond with an empty data, but not NULL response.
        requestHandlers.put(McpSchema.METHOD_PING, (exchange, params) -> Mono.just(Map.of()));

        // Add tools API handlers if the tool capability is enabled
        if (this.serverCapabilities.tools() != null) {
            requestHandlers.put(McpSchema.METHOD_TOOLS_LIST, toolsListRequestHandler());
            requestHandlers.put(McpSchema.METHOD_TOOLS_CALL, toolsCallRequestHandler());
        }

        // Add resources API handlers if provided
        if (this.serverCapabilities.resources() != null) {
            requestHandlers.put(McpSchema.METHOD_RESOURCES_LIST, resourcesListRequestHandler());
            requestHandlers.put(McpSchema.METHOD_RESOURCES_READ, resourcesReadRequestHandler());
            requestHandlers.put(McpSchema.METHOD_RESOURCES_TEMPLATES_LIST, resourceTemplateListRequestHandler());
        }

        // Add prompts API handlers if provider exists
        if (this.serverCapabilities.prompts() != null) {
            requestHandlers.put(McpSchema.METHOD_PROMPT_LIST, promptsListRequestHandler());
            requestHandlers.put(McpSchema.METHOD_PROMPT_GET, promptsGetRequestHandler());
        }

        // Add logging API handlers if the logging capability is enabled
        if (this.serverCapabilities.logging() != null) {
            requestHandlers.put(McpSchema.METHOD_LOGGING_SET_LEVEL, setLoggerRequestHandler());
        }

        Map<String, McpServerSession.NotificationHandler> notificationHandlers = new HashMap<>();

        notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_INITIALIZED, (exchange, params) -> Mono.empty());

        List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers = features
                .rootsChangeConsumers();

        if (Utils.isEmpty(rootsChangeConsumers)) {
            rootsChangeConsumers = List.of((exchange,
                                            roots) -> Mono.fromRunnable(() -> log.warn(
                    "Roots list changed notification, but no consumers provided. Roots list changed: {}",
                    roots)));
        }

        notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED,
                asyncRootsListChangedNotificationHandler(rootsChangeConsumers));

        mcpTransportProvider
                .setSessionFactory((sessionTransport) -> {
                    {
                        return new MyMcpServerSession(IdUtils.getUUID(), sessionTransport,
                                this::asyncInitializeRequestHandler, Mono::empty,
                                requestHandlers, notificationHandlers);
                    }
                });
    }

    // ---------------------------------------
    // Lifecycle Management
    // ---------------------------------------
    public Mono<McpSchema.InitializeResult> asyncInitializeRequestHandler(
            McpSchema.InitializeRequest initializeRequest) {
        return Mono.defer(() -> {
            log.debug("Client initialize request - Protocol: {}, Capabilities: {}, Info: {}",
                    initializeRequest.protocolVersion(), initializeRequest.capabilities(),
                    initializeRequest.clientInfo());

            // The server MUST respond with the highest protocol version it supports
            // if
            // it does not support the requested (e.g. Client) version.
            String serverProtocolVersion = this.protocolVersions.get(this.protocolVersions.size() - 1);

            if (this.protocolVersions.contains(initializeRequest.protocolVersion())) {
                // If the server supports the requested protocol version, it MUST
                // respond
                // with the same version.
                serverProtocolVersion = initializeRequest.protocolVersion();
            } else {
                log.warn(
                        "Client requested unsupported protocol version: {}, so the server will sugggest the {} version instead",
                        initializeRequest.protocolVersion(), serverProtocolVersion);
            }

            return Mono.just(new McpSchema.InitializeResult(serverProtocolVersion, this.serverCapabilities,
                    this.serverInfo, this.instructions));
        });
    }

    public McpSchema.ServerCapabilities getServerCapabilities() {
        return this.serverCapabilities;
    }

    public McpSchema.Implementation getServerInfo() {
        return this.serverInfo;
    }

    @Override
    public Mono<Void> closeGracefully() {
        return this.mcpTransportProvider.closeGracefully();
    }

    @Override
    public void close() {
        this.mcpTransportProvider.close();
    }

    private McpServerSession.NotificationHandler asyncRootsListChangedNotificationHandler(
            List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers) {
        return (exchange, params) -> exchange.listRoots()
                .flatMap(listRootsResult -> Flux.fromIterable(rootsChangeConsumers)
                        .flatMap(consumer -> consumer.apply(exchange, listRootsResult.roots()))
                        .onErrorResume(error -> {
                            log.error("Error handling roots list change notification", error);
                            return Mono.empty();
                        })
                        .then());
    }

    // ---------------------------------------
    // Tool Management
    // ---------------------------------------

    @Override
    public Mono<Void> addTool(McpServerFeatures.AsyncToolSpecification toolSpecification) {
        if (toolSpecification == null) {
            return Mono.error(new McpError("Tool specification must not be null"));
        }
        if (toolSpecification.tool() == null) {
            return Mono.error(new McpError("Tool must not be null"));
        }
        if (toolSpecification.call() == null) {
            return Mono.error(new McpError("Tool call handler must not be null"));
        }
        if (this.serverCapabilities.tools() == null) {
            return Mono.error(new McpError("Server must be configured with tool capabilities"));
        }

        return Mono.defer(() -> {
            // Check for duplicate tool names
            if (this.tools.stream().anyMatch(th -> th.tool().name().equals(toolSpecification.tool().name()))) {
                return Mono
                        .error(new McpError("Tool with name '" + toolSpecification.tool().name() + "' already exists"));
            }

            this.tools.add(toolSpecification);
            log.debug("Added tool handler: {}", toolSpecification.tool().name());

            if (this.serverCapabilities.tools().listChanged()) {
                return notifyToolsListChanged();
            }
            return Mono.empty();
        });
    }

    @Override
    public Mono<Void> removeTool(String toolName) {
        if (toolName == null) {
            return Mono.error(new McpError("Tool name must not be null"));
        }
        if (this.serverCapabilities.tools() == null) {
            return Mono.error(new McpError("Server must be configured with tool capabilities"));
        }

        return Mono.defer(() -> {
            boolean removed = this.tools
                    .removeIf(toolSpecification -> toolSpecification.tool().name().equals(toolName));
            if (removed) {
                log.debug("Removed tool handler: {}", toolName);
                if (this.serverCapabilities.tools().listChanged()) {
                    return notifyToolsListChanged();
                }
                return Mono.empty();
            }
            return Mono.error(new McpError("Tool with name '" + toolName + "' not found"));
        });
    }

    @Override
    public Mono<Void> notifyToolsListChanged() {
        return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED, null);
    }

    private McpServerSession.RequestHandler<McpSchema.ListToolsResult> toolsListRequestHandler() {
        return (exchange, params) -> {
            List<McpSchema.Tool> tools = this.tools.stream().map(McpServerFeatures.AsyncToolSpecification::tool).toList();

            return Mono.just(new McpSchema.ListToolsResult(tools, null));
        };
    }

    private McpServerSession.RequestHandler<McpSchema.CallToolResult> toolsCallRequestHandler() {
        return (exchange, params) -> {
            McpSchema.CallToolRequest callToolRequest = objectMapper.convertValue(params,
                    new TypeReference<McpSchema.CallToolRequest>() {
                    });

            Optional<McpServerFeatures.AsyncToolSpecification> toolSpecification = this.tools.stream()
                    .filter(tr -> callToolRequest.name().equals(tr.tool().name()))
                    .findAny();

            if (toolSpecification.isEmpty()) {
                return Mono.error(new McpError("Tool not found: " + callToolRequest.name()));
            }

            return toolSpecification.map(tool -> tool.call().apply(exchange, callToolRequest.arguments()))
                    .orElse(Mono.error(new McpError("Tool not found: " + callToolRequest.name())));
        };
    }

    // ---------------------------------------
    // Resource Management
    // ---------------------------------------

    @Override
    public Mono<Void> addResource(McpServerFeatures.AsyncResourceSpecification resourceSpecification) {
        if (resourceSpecification == null || resourceSpecification.resource() == null) {
            return Mono.error(new McpError("Resource must not be null"));
        }

        if (this.serverCapabilities.resources() == null) {
            return Mono.error(new McpError("Server must be configured with resource capabilities"));
        }

        return Mono.defer(() -> {
            if (this.resources.putIfAbsent(resourceSpecification.resource().uri(), resourceSpecification) != null) {
                return Mono.error(new McpError(
                        "Resource with URI '" + resourceSpecification.resource().uri() + "' already exists"));
            }
            log.debug("Added resource handler: {}", resourceSpecification.resource().uri());
            if (this.serverCapabilities.resources().listChanged()) {
                return notifyResourcesListChanged();
            }
            return Mono.empty();
        });
    }

    @Override
    public Mono<Void> removeResource(String resourceUri) {
        if (resourceUri == null) {
            return Mono.error(new McpError("Resource URI must not be null"));
        }
        if (this.serverCapabilities.resources() == null) {
            return Mono.error(new McpError("Server must be configured with resource capabilities"));
        }

        return Mono.defer(() -> {
            McpServerFeatures.AsyncResourceSpecification removed = this.resources.remove(resourceUri);
            if (removed != null) {
                log.debug("Removed resource handler: {}", resourceUri);
                if (this.serverCapabilities.resources().listChanged()) {
                    return notifyResourcesListChanged();
                }
                return Mono.empty();
            }
            return Mono.error(new McpError("Resource with URI '" + resourceUri + "' not found"));
        });
    }

    @Override
    public Mono<Void> notifyResourcesListChanged() {
        return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED, null);
    }

    private McpServerSession.RequestHandler<McpSchema.ListResourcesResult> resourcesListRequestHandler() {
        return (exchange, params) -> {
            var resourceList = this.resources.values()
                    .stream()
                    .map(McpServerFeatures.AsyncResourceSpecification::resource)
                    .toList();
            return Mono.just(new McpSchema.ListResourcesResult(resourceList, null));
        };
    }

    private McpServerSession.RequestHandler<McpSchema.ListResourceTemplatesResult> resourceTemplateListRequestHandler() {
        return (exchange, params) -> Mono
                .just(new McpSchema.ListResourceTemplatesResult(this.resourceTemplates, null));

    }

    private McpServerSession.RequestHandler<McpSchema.ReadResourceResult> resourcesReadRequestHandler() {
        return (exchange, params) -> {
            McpSchema.ReadResourceRequest resourceRequest = objectMapper.convertValue(params,
                    new TypeReference<McpSchema.ReadResourceRequest>() {
                    });
            var resourceUri = resourceRequest.uri();
            McpServerFeatures.AsyncResourceSpecification specification = this.resources.get(resourceUri);
            if (specification != null) {
                return specification.readHandler().apply(exchange, resourceRequest);
            }
            return Mono.error(new McpError("Resource not found: " + resourceUri));
        };
    }

    // ---------------------------------------
    // Prompt Management
    // ---------------------------------------

    @Override
    public Mono<Void> addPrompt(McpServerFeatures.AsyncPromptSpecification promptSpecification) {
        if (promptSpecification == null) {
            return Mono.error(new McpError("Prompt specification must not be null"));
        }
        if (this.serverCapabilities.prompts() == null) {
            return Mono.error(new McpError("Server must be configured with prompt capabilities"));
        }

        return Mono.defer(() -> {
            McpServerFeatures.AsyncPromptSpecification specification = this.prompts
                    .putIfAbsent(promptSpecification.prompt().name(), promptSpecification);
            if (specification != null) {
                return Mono.error(new McpError(
                        "Prompt with name '" + promptSpecification.prompt().name() + "' already exists"));
            }

            log.debug("Added prompt handler: {}", promptSpecification.prompt().name());

            // Servers that declared the listChanged capability SHOULD send a
            // notification,
            // when the list of available prompts changes
            if (this.serverCapabilities.prompts().listChanged()) {
                return notifyPromptsListChanged();
            }
            return Mono.empty();
        });
    }

    @Override
    public Mono<Void> removePrompt(String promptName) {
        if (promptName == null) {
            return Mono.error(new McpError("Prompt name must not be null"));
        }
        if (this.serverCapabilities.prompts() == null) {
            return Mono.error(new McpError("Server must be configured with prompt capabilities"));
        }

        return Mono.defer(() -> {
            McpServerFeatures.AsyncPromptSpecification removed = this.prompts.remove(promptName);

            if (removed != null) {
                log.debug("Removed prompt handler: {}", promptName);
                // Servers that declared the listChanged capability SHOULD send a
                // notification, when the list of available prompts changes
                if (this.serverCapabilities.prompts().listChanged()) {
                    return this.notifyPromptsListChanged();
                }
                return Mono.empty();
            }
            return Mono.error(new McpError("Prompt with name '" + promptName + "' not found"));
        });
    }

    @Override
    public Mono<Void> notifyPromptsListChanged() {
        return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED, null);
    }

    private McpServerSession.RequestHandler<McpSchema.ListPromptsResult> promptsListRequestHandler() {
        return (exchange, params) -> {
            // TODO: Implement pagination
            // McpSchema.PaginatedRequest request = objectMapper.convertValue(params,
            // new TypeReference<McpSchema.PaginatedRequest>() {
            // });

            var promptList = this.prompts.values()
                    .stream()
                    .map(McpServerFeatures.AsyncPromptSpecification::prompt)
                    .toList();

            return Mono.just(new McpSchema.ListPromptsResult(promptList, null));
        };
    }

    private McpServerSession.RequestHandler<McpSchema.GetPromptResult> promptsGetRequestHandler() {
        return (exchange, params) -> {
            McpSchema.GetPromptRequest promptRequest = objectMapper.convertValue(params,
                    new TypeReference<McpSchema.GetPromptRequest>() {
                    });

            // Implement prompt retrieval logic here
            McpServerFeatures.AsyncPromptSpecification specification = this.prompts.get(promptRequest.name());
            if (specification == null) {
                return Mono.error(new McpError("Prompt not found: " + promptRequest.name()));
            }

            return specification.promptHandler().apply(exchange, promptRequest);
        };
    }

    // ---------------------------------------
    // Logging Management
    // ---------------------------------------

    @Override
    public Mono<Void> loggingNotification(McpSchema.LoggingMessageNotification loggingMessageNotification) {

        if (loggingMessageNotification == null) {
            return Mono.error(new McpError("Logging message must not be null"));
        }

        if (loggingMessageNotification.level().level() < minLoggingLevel.level()) {
            return Mono.empty();
        }

        return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_MESSAGE,
                loggingMessageNotification);
    }

    private McpServerSession.RequestHandler<Object> setLoggerRequestHandler() {
        return (exchange, params) -> {
            return Mono.defer(() -> {

                McpSchema.SetLevelRequest newMinLoggingLevel = objectMapper.convertValue(params,
                        new TypeReference<McpSchema.SetLevelRequest>() {
                        });

                exchange.setMinLoggingLevel(newMinLoggingLevel.level());

                // FIXME: this field is deprecated and should be removed together
                // with the broadcasting loggingNotification.
                this.minLoggingLevel = newMinLoggingLevel.level();

                return Mono.just(Map.of());
            });
        };
    }

    // ---------------------------------------
    // Sampling
    // ---------------------------------------

    @Override
    void setProtocolVersions(List<String> protocolVersions) {
        this.protocolVersions = protocolVersions;
    }

}
