package io.modelcontextprotocol.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * @author Bugee
 */
public class MySyncSpecification {

    private static final McpSchema.Implementation DEFAULT_SERVER_INFO = new McpSchema.Implementation("mcp-server",
            "1.0.0");

    private final McpServerTransportProvider transportProvider;
    /**
     * The Model Context Protocol (MCP) allows servers to expose tools that can be
     * invoked by language models. Tools enable models to interact with external
     * systems, such as querying databases, calling APIs, or performing computations.
     * Each tool is uniquely identified by a name and includes metadata describing its
     * schema.
     */
    private final List<McpServerFeatures.SyncToolSpecification> tools = new ArrayList<>();
    /**
     * The Model Context Protocol (MCP) provides a standardized way for servers to
     * expose resources to clients. Resources allow servers to share data that
     * provides context to language models, such as files, database schemas, or
     * application-specific information. Each resource is uniquely identified by a
     * URI.
     */
    private final Map<String, McpServerFeatures.SyncResourceSpecification> resources = new HashMap<>();
    private final List<McpSchema.ResourceTemplate> resourceTemplates = new ArrayList<>();
    /**
     * The Model Context Protocol (MCP) provides a standardized way for servers to
     * expose prompt templates to clients. Prompts allow servers to provide structured
     * messages and instructions for interacting with language models. Clients can
     * discover available prompts, retrieve their contents, and provide arguments to
     * customize them.
     */
    private final Map<String, McpServerFeatures.SyncPromptSpecification> prompts = new HashMap<>();
    private final List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeHandlers = new ArrayList<>();
    private ObjectMapper objectMapper;
    private McpSchema.Implementation serverInfo = DEFAULT_SERVER_INFO;
    private McpSchema.ServerCapabilities serverCapabilities;
    private String instructions;

    public MySyncSpecification(McpServerTransportProvider transportProvider) {
        Assert.notNull(transportProvider, "Transport provider must not be null");
        this.transportProvider = transportProvider;
    }

    /**
     * Sets the server implementation information that will be shared with clients
     * during connection initialization. This helps with version compatibility,
     * debugging, and server identification.
     *
     * @param serverInfo The server implementation details including name and version.
     *                   Must not be null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if serverInfo is null
     */
    public MySyncSpecification serverInfo(McpSchema.Implementation serverInfo) {
        Assert.notNull(serverInfo, "Server info must not be null");
        this.serverInfo = serverInfo;
        return this;
    }

    /**
     * Sets the server implementation information using name and version strings. This
     * is a convenience method alternative to
     * {@link #serverInfo(McpSchema.Implementation)}.
     *
     * @param name    The server name. Must not be null or empty.
     * @param version The server version. Must not be null or empty.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if name or version is null or empty
     * @see #serverInfo(McpSchema.Implementation)
     */
    public MySyncSpecification serverInfo(String name, String version) {
        Assert.hasText(name, "Name must not be null or empty");
        Assert.hasText(version, "Version must not be null or empty");
        this.serverInfo = new McpSchema.Implementation(name, version);
        return this;
    }

    /**
     * Sets the server instructions that will be shared with clients during connection
     * initialization. These instructions provide guidance to the client on how to
     * interact with this server.
     *
     * @param instructions The instructions text. Can be null or empty.
     * @return This builder instance for method chaining
     */
    public MySyncSpecification instructions(String instructions) {
        this.instructions = instructions;
        return this;
    }

    /**
     * Sets the server capabilities that will be advertised to clients during
     * connection initialization. Capabilities define what features the server
     * supports, such as:
     * <ul>
     * <li>Tool execution
     * <li>Resource access
     * <li>Prompt handling
     * </ul>
     *
     * @param serverCapabilities The server capabilities configuration. Must not be
     *                           null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if serverCapabilities is null
     */
    public MySyncSpecification capabilities(McpSchema.ServerCapabilities serverCapabilities) {
        Assert.notNull(serverCapabilities, "Server capabilities must not be null");
        this.serverCapabilities = serverCapabilities;
        return this;
    }

    /**
     * Adds a single tool with its implementation handler to the server. This is a
     * convenience method for registering individual tools without creating a
     * {@link McpServerFeatures.SyncToolSpecification} explicitly.
     *
     * <p>
     * Example usage: <pre>{@code
     * .tool(
     *     new Tool("calculator", "Performs calculations", schema),
     *     (exchange, args) -> new CallToolResult("Result: " + calculate(args))
     * )
     * }</pre>
     *
     * @param tool    The tool definition including name, description, and schema. Must
     *                not be null.
     * @param handler The function that implements the tool's logic. Must not be null.
     *                The function's first argument is an {@link McpSyncServerExchange} upon which
     *                the server can interact with the connected client. The second argument is the
     *                list of arguments passed to the tool.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if tool or handler is null
     */
    public MySyncSpecification tool(McpSchema.Tool tool,
                                    BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> handler) {
        Assert.notNull(tool, "Tool must not be null");
        Assert.notNull(handler, "Handler must not be null");

        this.tools.add(new McpServerFeatures.SyncToolSpecification(tool, handler));

        return this;
    }

    /**
     * Adds multiple tools with their handlers to the server using a List. This method
     * is useful when tools are dynamically generated or loaded from a configuration
     * source.
     *
     * @param toolSpecifications The list of tool specifications to add. Must not be
     *                           null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if toolSpecifications is null
     * @see #tools(McpServerFeatures.SyncToolSpecification...)
     */
    public MySyncSpecification tools(List<McpServerFeatures.SyncToolSpecification> toolSpecifications) {
        Assert.notNull(toolSpecifications, "Tool handlers list must not be null");
        this.tools.addAll(toolSpecifications);
        return this;
    }

    /**
     * Adds multiple tools with their handlers to the server using varargs. This
     * method provides a convenient way to register multiple tools inline.
     *
     * <p>
     * Example usage: <pre>{@code
     * .tools(
     *     new ToolSpecification(calculatorTool, calculatorHandler),
     *     new ToolSpecification(weatherTool, weatherHandler),
     *     new ToolSpecification(fileManagerTool, fileManagerHandler)
     * )
     * }</pre>
     *
     * @param toolSpecifications The tool specifications to add. Must not be null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if toolSpecifications is null
     * @see #tools(List)
     */
    public MySyncSpecification tools(McpServerFeatures.SyncToolSpecification... toolSpecifications) {
        Assert.notNull(toolSpecifications, "Tool handlers list must not be null");
        for (McpServerFeatures.SyncToolSpecification tool : toolSpecifications) {
            this.tools.add(tool);
        }
        return this;
    }

    /**
     * Registers multiple resources with their handlers using a Map. This method is
     * useful when resources are dynamically generated or loaded from a configuration
     * source.
     *
     * @param resourceSpecifications Map of resource name to specification. Must not
     *                               be null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if resourceSpecifications is null
     * @see #resources(McpServerFeatures.SyncResourceSpecification...)
     */
    public MySyncSpecification resources(
            Map<String, McpServerFeatures.SyncResourceSpecification> resourceSpecifications) {
        Assert.notNull(resourceSpecifications, "Resource handlers map must not be null");
        this.resources.putAll(resourceSpecifications);
        return this;
    }

    /**
     * Registers multiple resources with their handlers using a List. This method is
     * useful when resources need to be added in bulk from a collection.
     *
     * @param resourceSpecifications List of resource specifications. Must not be
     *                               null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if resourceSpecifications is null
     * @see #resources(McpServerFeatures.SyncResourceSpecification...)
     */
    public MySyncSpecification resources(List<McpServerFeatures.SyncResourceSpecification> resourceSpecifications) {
        Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
        for (McpServerFeatures.SyncResourceSpecification resource : resourceSpecifications) {
            this.resources.put(resource.resource().uri(), resource);
        }
        return this;
    }

    /**
     * Registers multiple resources with their handlers using varargs. This method
     * provides a convenient way to register multiple resources inline.
     *
     * <p>
     * Example usage: <pre>{@code
     * .resources(
     *     new ResourceSpecification(fileResource, fileHandler),
     *     new ResourceSpecification(dbResource, dbHandler),
     *     new ResourceSpecification(apiResource, apiHandler)
     * )
     * }</pre>
     *
     * @param resourceSpecifications The resource specifications to add. Must not be
     *                               null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if resourceSpecifications is null
     */
    public MySyncSpecification resources(McpServerFeatures.SyncResourceSpecification... resourceSpecifications) {
        Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
        for (McpServerFeatures.SyncResourceSpecification resource : resourceSpecifications) {
            this.resources.put(resource.resource().uri(), resource);
        }
        return this;
    }

    /**
     * Sets the resource templates that define patterns for dynamic resource access.
     * Templates use URI patterns with placeholders that can be filled at runtime.
     *
     * <p>
     * Example usage: <pre>{@code
     * .resourceTemplates(
     *     new ResourceTemplate("file://{path}", "Access files by path"),
     *     new ResourceTemplate("db://{table}/{id}", "Access database records")
     * )
     * }</pre>
     *
     * @param resourceTemplates List of resource templates. If null, clears existing
     *                          templates.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if resourceTemplates is null.
     * @see #resourceTemplates(McpSchema.ResourceTemplate...)
     */
    public MySyncSpecification resourceTemplates(List<McpSchema.ResourceTemplate> resourceTemplates) {
        Assert.notNull(resourceTemplates, "Resource templates must not be null");
        this.resourceTemplates.addAll(resourceTemplates);
        return this;
    }

    /**
     * Sets the resource templates using varargs for convenience. This is an
     * alternative to {@link #resourceTemplates(List)}.
     *
     * @param resourceTemplates The resource templates to set.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if resourceTemplates is null
     * @see #resourceTemplates(List)
     */
    public MySyncSpecification resourceTemplates(McpSchema.ResourceTemplate... resourceTemplates) {
        Assert.notNull(resourceTemplates, "Resource templates must not be null");
        for (McpSchema.ResourceTemplate resourceTemplate : resourceTemplates) {
            this.resourceTemplates.add(resourceTemplate);
        }
        return this;
    }

    /**
     * Registers multiple prompts with their handlers using a Map. This method is
     * useful when prompts are dynamically generated or loaded from a configuration
     * source.
     *
     * <p>
     * Example usage: <pre>{@code
     * Map<String, PromptSpecification> prompts = new HashMap<>();
     * prompts.put("analysis", new PromptSpecification(
     *     new Prompt("analysis", "Code analysis template"),
     *     (exchange, request) -> new GetPromptResult(generateAnalysisPrompt(request))
     * ));
     * .prompts(prompts)
     * }</pre>
     *
     * @param prompts Map of prompt name to specification. Must not be null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if prompts is null
     */
    public MySyncSpecification prompts(Map<String, McpServerFeatures.SyncPromptSpecification> prompts) {
        Assert.notNull(prompts, "Prompts map must not be null");
        this.prompts.putAll(prompts);
        return this;
    }

    /**
     * Registers multiple prompts with their handlers using a List. This method is
     * useful when prompts need to be added in bulk from a collection.
     *
     * @param prompts List of prompt specifications. Must not be null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if prompts is null
     * @see #prompts(McpServerFeatures.SyncPromptSpecification...)
     */
    public MySyncSpecification prompts(List<McpServerFeatures.SyncPromptSpecification> prompts) {
        Assert.notNull(prompts, "Prompts list must not be null");
        for (McpServerFeatures.SyncPromptSpecification prompt : prompts) {
            this.prompts.put(prompt.prompt().name(), prompt);
        }
        return this;
    }

    /**
     * Registers multiple prompts with their handlers using varargs. This method
     * provides a convenient way to register multiple prompts inline.
     *
     * <p>
     * Example usage: <pre>{@code
     * .prompts(
     *     new PromptSpecification(analysisPrompt, analysisHandler),
     *     new PromptSpecification(summaryPrompt, summaryHandler),
     *     new PromptSpecification(reviewPrompt, reviewHandler)
     * )
     * }</pre>
     *
     * @param prompts The prompt specifications to add. Must not be null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if prompts is null
     */
    public MySyncSpecification prompts(McpServerFeatures.SyncPromptSpecification... prompts) {
        Assert.notNull(prompts, "Prompts list must not be null");
        for (McpServerFeatures.SyncPromptSpecification prompt : prompts) {
            this.prompts.put(prompt.prompt().name(), prompt);
        }
        return this;
    }

    /**
     * Registers a consumer that will be notified when the list of roots changes. This
     * is useful for updating resource availability dynamically, such as when new
     * files are added or removed.
     *
     * @param handler The handler to register. Must not be null. The function's first
     *                argument is an {@link McpSyncServerExchange} upon which the server can interact
     *                with the connected client. The second argument is the list of roots.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if consumer is null
     */
    public MySyncSpecification rootsChangeHandler(BiConsumer<McpSyncServerExchange, List<McpSchema.Root>> handler) {
        Assert.notNull(handler, "Consumer must not be null");
        this.rootsChangeHandlers.add(handler);
        return this;
    }

    /**
     * Registers multiple consumers that will be notified when the list of roots
     * changes. This method is useful when multiple consumers need to be registered at
     * once.
     *
     * @param handlers The list of handlers to register. Must not be null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if consumers is null
     * @see #rootsChangeHandler(BiConsumer)
     */
    public MySyncSpecification rootsChangeHandlers(
            List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> handlers) {
        Assert.notNull(handlers, "Handlers list must not be null");
        this.rootsChangeHandlers.addAll(handlers);
        return this;
    }

    /**
     * Registers multiple consumers that will be notified when the list of roots
     * changes using varargs. This method provides a convenient way to register
     * multiple consumers inline.
     *
     * @param handlers The handlers to register. Must not be null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if consumers is null
     * @see #rootsChangeHandlers(List)
     */
    public MySyncSpecification rootsChangeHandlers(
            BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>... handlers) {
        Assert.notNull(handlers, "Handlers list must not be null");
        return this.rootsChangeHandlers(List.of(handlers));
    }

    /**
     * Sets the object mapper to use for serializing and deserializing JSON messages.
     *
     * @param objectMapper the instance to use. Must not be null.
     * @return This builder instance for method chaining.
     * @throws IllegalArgumentException if objectMapper is null
     */
    public MySyncSpecification objectMapper(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        this.objectMapper = objectMapper;
        return this;
    }

    /**
     * Builds a synchronous MCP server that provides blocking operations.
     *
     * @return A new instance of {@link McpSyncServer} configured with this builder's
     * settings.
     */
    public McpSyncServer build() {
        McpServerFeatures.Sync syncFeatures = new McpServerFeatures.Sync(this.serverInfo, this.serverCapabilities,
                this.tools, this.resources, this.resourceTemplates, this.prompts, this.rootsChangeHandlers,
                this.instructions);
        McpServerFeatures.Async asyncFeatures = MyMcpServerFeatures.Async.fromSync(syncFeatures);
        var mapper = this.objectMapper != null ? this.objectMapper : new ObjectMapper();
        var asyncServer = new MyMcpAsyncServer(this.transportProvider, mapper, asyncFeatures);

        return new McpSyncServer(asyncServer);
    }

}
