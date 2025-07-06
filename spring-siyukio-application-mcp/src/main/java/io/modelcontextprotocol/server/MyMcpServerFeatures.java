package io.modelcontextprotocol.server;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.Utils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author Bugee
 */
public class MyMcpServerFeatures {

    /**
     * Asynchronous server features specification.
     *
     * @param serverInfo           The server implementation details
     * @param serverCapabilities   The server capabilities
     * @param tools                The list of tool specifications
     * @param resources            The map of resource specifications
     * @param resourceTemplates    The list of resource templates
     * @param prompts              The map of prompt specifications
     * @param rootsChangeConsumers The list of consumers that will be notified when the
     *                             roots list changes
     * @param instructions         The server instructions text
     */
    record Async(McpSchema.Implementation serverInfo, McpSchema.ServerCapabilities serverCapabilities,
                 List<McpServerFeatures.AsyncToolSpecification> tools,
                 Map<String, McpServerFeatures.AsyncResourceSpecification> resources,
                 List<McpSchema.ResourceTemplate> resourceTemplates,
                 Map<String, McpServerFeatures.AsyncPromptSpecification> prompts,
                 Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions,
                 List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers,
                 String instructions) {

        /**
         * Create an instance and validate the arguments.
         *
         * @param serverInfo           The server implementation details
         * @param serverCapabilities   The server capabilities
         * @param tools                The list of tool specifications
         * @param resources            The map of resource specifications
         * @param resourceTemplates    The list of resource templates
         * @param prompts              The map of prompt specifications
         * @param rootsChangeConsumers The list of consumers that will be notified when
         *                             the roots list changes
         * @param instructions         The server instructions text
         */
        Async(McpSchema.Implementation serverInfo, McpSchema.ServerCapabilities serverCapabilities,
              List<McpServerFeatures.AsyncToolSpecification> tools, Map<String, McpServerFeatures.AsyncResourceSpecification> resources,
              List<McpSchema.ResourceTemplate> resourceTemplates,
              Map<String, McpServerFeatures.AsyncPromptSpecification> prompts,
              Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions,
              List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers,
              String instructions) {

            Assert.notNull(serverInfo, "Server info must not be null");

            this.serverInfo = serverInfo;
            this.serverCapabilities = (serverCapabilities != null) ? serverCapabilities
                    : new McpSchema.ServerCapabilities(null, // completions
                    null, // experimental
                    new McpSchema.ServerCapabilities.LoggingCapabilities(), // Enable
                    // logging
                    // by
                    // default
                    !Utils.isEmpty(prompts) ? new McpSchema.ServerCapabilities.PromptCapabilities(false) : null,
                    !Utils.isEmpty(resources)
                            ? new McpSchema.ServerCapabilities.ResourceCapabilities(false, false) : null,
                    !Utils.isEmpty(tools) ? new McpSchema.ServerCapabilities.ToolCapabilities(false) : null);

            this.tools = (tools != null) ? tools : List.of();
            this.resources = (resources != null) ? resources : Map.of();
            this.resourceTemplates = (resourceTemplates != null) ? resourceTemplates : List.of();
            this.prompts = (prompts != null) ? prompts : Map.of();
            this.completions = (completions != null) ? completions : Map.of();
            this.rootsChangeConsumers = (rootsChangeConsumers != null) ? rootsChangeConsumers : List.of();
            this.instructions = instructions;
        }

        /**
         * Convert a synchronous specification into an asynchronous one and provide
         * blocking code offloading to prevent accidental blocking of the non-blocking
         * transport.
         *
         * @param syncSpec a potentially blocking, synchronous specification.
         * @return a specification which is protected from blocking calls specified by the
         * user.
         */
        static McpServerFeatures.Async fromSync(McpServerFeatures.Sync syncSpec) {
            List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();
            for (var tool : syncSpec.tools()) {
                tools.add(AsyncToolSpecification.fromSync(tool));
            }

            Map<String, McpServerFeatures.AsyncResourceSpecification> resources = new HashMap<>();
            syncSpec.resources().forEach((key, resource) -> {
                resources.put(key, McpServerFeatures.AsyncResourceSpecification.fromSync(resource));
            });

            Map<String, McpServerFeatures.AsyncPromptSpecification> prompts = new HashMap<>();
            syncSpec.prompts().forEach((key, prompt) -> {
                prompts.put(key, McpServerFeatures.AsyncPromptSpecification.fromSync(prompt));
            });

            Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions = new HashMap<>();
            syncSpec.completions().forEach((key, completion) -> {
                completions.put(key, McpServerFeatures.AsyncCompletionSpecification.fromSync(completion));
            });

            List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootChangeConsumers = new ArrayList<>();

            for (var rootChangeConsumer : syncSpec.rootsChangeConsumers()) {
                rootChangeConsumers.add((exchange, list) -> Mono
                        .<Void>fromRunnable(() -> rootChangeConsumer.accept(new McpSyncServerExchange(exchange), list))
                        .subscribeOn(Schedulers.boundedElastic()));
            }

            return new McpServerFeatures.Async(syncSpec.serverInfo(), syncSpec.serverCapabilities(), tools, resources,
                    syncSpec.resourceTemplates(), prompts, completions, rootChangeConsumers, syncSpec.instructions());
        }
    }

    /**
     * Specification of a tool with its asynchronous handler function. Tools are the
     * primary way for MCP servers to expose functionality to AI models. Each tool
     * represents a specific capability, such as:
     * <ul>
     * <li>Performing calculations
     * <li>Accessing external APIs
     * <li>Querying databases
     * <li>Manipulating files
     * <li>Executing system commands
     * </ul>
     *
     * <p>
     * Example tool specification: <pre>{@code
     * new McpServerFeatures.AsyncToolSpecification(
     *     new Tool(
     *         "calculator",
     *         "Performs mathematical calculations",
     *         new JsonSchemaObject()
     *             .required("expression")
     *             .property("expression", JsonSchemaType.STRING)
     *     ),
     *     (exchange, args) -> {
     *         String expr = (String) args.get("expression");
     *         return Mono.fromSupplier(() -> evaluate(expr))
     *             .map(result -> new CallToolResult("Result: " + result));
     *     }
     * )
     * }</pre>
     *
     * @param tool The tool definition including name, description, and parameter schema
     * @param call The function that implements the tool's logic, receiving arguments and
     *             returning results. The function's first argument is an
     *             {@link McpAsyncServerExchange} upon which the server can interact with the
     *             connected client. The second arguments is a map of tool arguments.
     */
    public record AsyncToolSpecification(McpSchema.Tool tool,
                                         BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call) {

        static McpServerFeatures.AsyncToolSpecification fromSync(McpServerFeatures.SyncToolSpecification tool) {
            // FIXME: This is temporary, proper validation should be implemented
            if (tool == null) {
                return null;
            }
            return new McpServerFeatures.AsyncToolSpecification(tool.tool(),
                    (exchange, map) -> Mono
                            .fromCallable(() -> tool.call().apply(new MyMcpSyncServerExchange(exchange), map))
                            .subscribeOn(Schedulers.boundedElastic()));
        }
    }
}
