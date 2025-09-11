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
         * @param syncSpec           a potentially blocking, synchronous specification.
         * @param immediateExecution when true, do not offload. Do NOT set to true when
         *                           using a non-blocking transport.
         * @return a specification which is protected from blocking calls specified by the
         * user.
         */
        static McpServerFeatures.Async fromSync(McpServerFeatures.Sync syncSpec, boolean immediateExecution) {
            List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();
            for (var tool : syncSpec.tools()) {
                tools.add(MyMcpServerFeatures.AsyncToolSpecification.fromSync(tool, immediateExecution));
            }

            Map<String, McpServerFeatures.AsyncResourceSpecification> resources = new HashMap<>();
            syncSpec.resources().forEach((key, resource) -> {
                resources.put(key, MyMcpServerFeatures.AsyncResourceSpecification.fromSync(resource, immediateExecution));
            });

            Map<String, McpServerFeatures.AsyncPromptSpecification> prompts = new HashMap<>();
            syncSpec.prompts().forEach((key, prompt) -> {
                prompts.put(key, MyMcpServerFeatures.AsyncPromptSpecification.fromSync(prompt, immediateExecution));
            });

            Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions = new HashMap<>();
            syncSpec.completions().forEach((key, completion) -> {
                completions.put(key, MyMcpServerFeatures.AsyncCompletionSpecification.fromSync(completion, immediateExecution));
            });

            List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootChangeConsumers = new ArrayList<>();

            for (var rootChangeConsumer : syncSpec.rootsChangeConsumers()) {
                rootChangeConsumers.add((exchange, list) -> Mono
                        .<Void>fromRunnable(() -> rootChangeConsumer.accept(new MyMcpSyncServerExchange(exchange), list))
                        .subscribeOn(Schedulers.boundedElastic()));
            }

            return new McpServerFeatures.Async(syncSpec.serverInfo(), syncSpec.serverCapabilities(), tools, resources,
                    syncSpec.resourceTemplates(), prompts, completions, rootChangeConsumers, syncSpec.instructions());
        }
    }

    /**
     * Specification of a tool with its asynchronous handler function. Tools are the
     * primary way for MCP servers to expose functionality to AI models. Each tool
     * represents a specific capability.
     *
     * @param tool        The tool definition including name, description, and parameter schema
     * @param call        Deprecated. Use the {@link McpServerFeatures.AsyncToolSpecification#callHandler} instead.
     * @param callHandler The function that implements the tool's logic, receiving a
     *                    {@link McpAsyncServerExchange} and a
     *                    {@link io.modelcontextprotocol.spec.McpSchema.CallToolRequest} and returning
     *                    results. The function's first argument is an {@link McpAsyncServerExchange} upon
     *                    which the server can interact with the connected client. The second arguments is a
     *                    map of tool arguments.
     */
    public record AsyncToolSpecification(McpSchema.Tool tool,
                                         @Deprecated BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call,
                                         BiFunction<McpAsyncServerExchange, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> callHandler) {

        /**
         * @deprecated Use {@link McpServerFeatures.AsyncToolSpecification (McpSchema.Tool, null,
         * BiFunction)} instead.
         **/
        @Deprecated
        public AsyncToolSpecification(McpSchema.Tool tool,
                                      BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> call) {
            this(tool, call, (exchange, toolReq) -> call.apply(exchange, toolReq.arguments()));
        }

        static McpServerFeatures.AsyncToolSpecification fromSync(McpServerFeatures.SyncToolSpecification syncToolSpec) {
            return fromSync(syncToolSpec, false);
        }

        static McpServerFeatures.AsyncToolSpecification fromSync(McpServerFeatures.SyncToolSpecification syncToolSpec, boolean immediate) {

            // FIXME: This is temporary, proper validation should be implemented
            if (syncToolSpec == null) {
                return null;
            }

            BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> deprecatedCall = (syncToolSpec
                    .call() != null) ? (exchange, map) -> {
                var toolResult = Mono
                        .fromCallable(() -> syncToolSpec.call().apply(new MyMcpSyncServerExchange(exchange), map));
                return immediate ? toolResult : toolResult.subscribeOn(Schedulers.boundedElastic());
            } : null;

            BiFunction<McpAsyncServerExchange, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> callHandler = (
                    exchange, req) -> {
                var toolResult = Mono
                        .fromCallable(() -> syncToolSpec.callHandler().apply(new MyMcpSyncServerExchange(exchange), req));
                return immediate ? toolResult : toolResult.subscribeOn(Schedulers.boundedElastic());
            };

            return new McpServerFeatures.AsyncToolSpecification(syncToolSpec.tool(), deprecatedCall, callHandler);
        }

        /**
         * Creates a new builder instance.
         *
         * @return a new Builder instance
         */
        public static McpServerFeatures.AsyncToolSpecification.Builder builder() {
            return new McpServerFeatures.AsyncToolSpecification.Builder();
        }

    }

    /**
     * Specification of a resource with its asynchronous handler function. Resources
     * provide context to AI models by exposing data such as:
     * <ul>
     * <li>File contents
     * <li>Database records
     * <li>API responses
     * <li>System information
     * <li>Application state
     * </ul>
     *
     * <p>
     * Example resource specification:
     *
     * <pre>{@code
     * new McpServerFeatures.AsyncResourceSpecification(
     * 		new Resource("docs", "Documentation files", "text/markdown"),
     * 		(exchange, request) -> Mono.fromSupplier(() -> readFile(request.getPath()))
     * 				.map(ReadResourceResult::new))
     * }</pre>
     *
     * @param resource    The resource definition including name, description, and MIME type
     * @param readHandler The function that handles resource read requests. The function's
     *                    first argument is an {@link McpAsyncServerExchange} upon which the server can
     *                    interact with the connected client. The second arguments is a
     *                    {@link io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest}.
     */
    public record AsyncResourceSpecification(McpSchema.Resource resource,
                                             BiFunction<McpAsyncServerExchange, McpSchema.ReadResourceRequest, Mono<McpSchema.ReadResourceResult>> readHandler) {

        static McpServerFeatures.AsyncResourceSpecification fromSync(McpServerFeatures.SyncResourceSpecification resource, boolean immediateExecution) {
            // FIXME: This is temporary, proper validation should be implemented
            if (resource == null) {
                return null;
            }
            return new McpServerFeatures.AsyncResourceSpecification(resource.resource(), (exchange, req) -> {
                var resourceResult = Mono
                        .fromCallable(() -> resource.readHandler().apply(new MyMcpSyncServerExchange(exchange), req));
                return immediateExecution ? resourceResult : resourceResult.subscribeOn(Schedulers.boundedElastic());
            });
        }
    }

    /**
     * Specification of a prompt template with its asynchronous handler function. Prompts
     * provide structured templates for AI model interactions, supporting:
     * <ul>
     * <li>Consistent message formatting
     * <li>Parameter substitution
     * <li>Context injection
     * <li>Response formatting
     * <li>Instruction templating
     * </ul>
     *
     * <p>
     * Example prompt specification:
     *
     * <pre>{@code
     * new McpServerFeatures.AsyncPromptSpecification(
     * 		new Prompt("analyze", "Code analysis template"),
     * 		(exchange, request) -> {
     * 			String code = request.getArguments().get("code");
     * 			return Mono.just(new GetPromptResult(
     * 					"Analyze this code:\n\n" + code + "\n\nProvide feedback on:"));
     *        })
     * }</pre>
     *
     * @param prompt        The prompt definition including name and description
     * @param promptHandler The function that processes prompt requests and returns
     *                      formatted templates. The function's first argument is an
     *                      {@link McpAsyncServerExchange} upon which the server can interact with the
     *                      connected client. The second arguments is a
     *                      {@link io.modelcontextprotocol.spec.McpSchema.GetPromptRequest}.
     */
    public record AsyncPromptSpecification(McpSchema.Prompt prompt,
                                           BiFunction<McpAsyncServerExchange, McpSchema.GetPromptRequest, Mono<McpSchema.GetPromptResult>> promptHandler) {

        static McpServerFeatures.AsyncPromptSpecification fromSync(McpServerFeatures.SyncPromptSpecification prompt, boolean immediateExecution) {
            // FIXME: This is temporary, proper validation should be implemented
            if (prompt == null) {
                return null;
            }
            return new McpServerFeatures.AsyncPromptSpecification(prompt.prompt(), (exchange, req) -> {
                var promptResult = Mono
                        .fromCallable(() -> prompt.promptHandler().apply(new MyMcpSyncServerExchange(exchange), req));
                return immediateExecution ? promptResult : promptResult.subscribeOn(Schedulers.boundedElastic());
            });
        }
    }

    /**
     * Specification of a completion handler function with asynchronous execution support.
     * Completions generate AI model outputs based on prompt or resource references and
     * user-provided arguments. This abstraction enables:
     * <ul>
     * <li>Customizable response generation logic
     * <li>Parameter-driven template expansion
     * <li>Dynamic interaction with connected clients
     * </ul>
     *
     * @param referenceKey      The unique key representing the completion reference.
     * @param completionHandler The asynchronous function that processes completion
     *                          requests and returns results. The first argument is an
     *                          {@link McpAsyncServerExchange} used to interact with the client. The second
     *                          argument is a {@link io.modelcontextprotocol.spec.McpSchema.CompleteRequest}.
     */
    public record AsyncCompletionSpecification(McpSchema.CompleteReference referenceKey,
                                               BiFunction<McpAsyncServerExchange, McpSchema.CompleteRequest, Mono<McpSchema.CompleteResult>> completionHandler) {

        /**
         * Converts a synchronous {@link McpServerFeatures.SyncCompletionSpecification} into an
         * {@link McpServerFeatures.AsyncCompletionSpecification} by wrapping the handler in a bounded
         * elastic scheduler for safe non-blocking execution.
         *
         * @param completion the synchronous completion specification
         * @return an asynchronous wrapper of the provided sync specification, or
         * {@code null} if input is null
         */
        static McpServerFeatures.AsyncCompletionSpecification fromSync(McpServerFeatures.SyncCompletionSpecification completion,
                                                                       boolean immediateExecution) {
            if (completion == null) {
                return null;
            }
            return new McpServerFeatures.AsyncCompletionSpecification(completion.referenceKey(), (exchange, request) -> {
                var completionResult = Mono.fromCallable(
                        () -> completion.completionHandler().apply(new MyMcpSyncServerExchange(exchange), request));
                return immediateExecution ? completionResult
                        : completionResult.subscribeOn(Schedulers.boundedElastic());
            });
        }
    }
}
