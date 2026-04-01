package io.modelcontextprotocol.server;

import io.github.siyukio.tools.util.AsyncUtils;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 *
 * @author Bugee
 */
public class CustomMcpServerFeatures {

    record AsyncToolSpecification() {
        static McpServerFeatures.AsyncToolSpecification fromSync(McpServerFeatures.SyncToolSpecification syncToolSpec, boolean immediate) {

            // FIXME: This is temporary, proper validation should be implemented
            if (syncToolSpec == null) {
                return null;
            }

            BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<McpSchema.CallToolResult>> deprecatedCall = (syncToolSpec
                    .call() != null) ? (exchange, map) -> {
                var toolResult = Mono
                        .fromCallable(() -> syncToolSpec.call().apply(new McpSyncServerExchange(exchange), map));
                return immediate ? toolResult : toolResult.subscribeOn(AsyncUtils.VIRTUAL_SCHEDULER);
            } : null;

            BiFunction<McpAsyncServerExchange, McpSchema.CallToolRequest, Mono<McpSchema.CallToolResult>> callHandler = (
                    exchange, req) -> {
                var toolResult = Mono
                        .fromCallable(() -> syncToolSpec.callHandler().apply(new McpSyncServerExchange(exchange), req));
                return immediate ? toolResult : toolResult.subscribeOn(AsyncUtils.VIRTUAL_SCHEDULER);
            };

            return new McpServerFeatures.AsyncToolSpecification(syncToolSpec.tool(), deprecatedCall, callHandler);
        }
    }

    record AsyncResourceSpecification() {
        static McpServerFeatures.AsyncResourceSpecification fromSync(McpServerFeatures.SyncResourceSpecification resource, boolean immediateExecution) {
            // FIXME: This is temporary, proper validation should be implemented
            if (resource == null) {
                return null;
            }
            return new McpServerFeatures.AsyncResourceSpecification(resource.resource(), (exchange, req) -> {
                var resourceResult = Mono
                        .fromCallable(() -> resource.readHandler().apply(new McpSyncServerExchange(exchange), req));
                return immediateExecution ? resourceResult : resourceResult.subscribeOn(AsyncUtils.VIRTUAL_SCHEDULER);
            });
        }
    }

    record AsyncResourceTemplateSpecification() {
        static McpServerFeatures.AsyncResourceTemplateSpecification fromSync(McpServerFeatures.SyncResourceTemplateSpecification resource,
                                                                             boolean immediateExecution) {
            // FIXME: This is temporary, proper validation should be implemented
            if (resource == null) {
                return null;
            }
            return new McpServerFeatures.AsyncResourceTemplateSpecification(resource.resourceTemplate(), (exchange, req) -> {
                var resourceResult = Mono
                        .fromCallable(() -> resource.readHandler().apply(new McpSyncServerExchange(exchange), req));
                return immediateExecution ? resourceResult : resourceResult.subscribeOn(AsyncUtils.VIRTUAL_SCHEDULER);
            });
        }
    }

    record AsyncPromptSpecification() {
        static McpServerFeatures.AsyncPromptSpecification fromSync(McpServerFeatures.SyncPromptSpecification prompt, boolean immediateExecution) {
            // FIXME: This is temporary, proper validation should be implemented
            if (prompt == null) {
                return null;
            }
            return new McpServerFeatures.AsyncPromptSpecification(prompt.prompt(), (exchange, req) -> {
                var promptResult = Mono
                        .fromCallable(() -> prompt.promptHandler().apply(new McpSyncServerExchange(exchange), req));
                return immediateExecution ? promptResult : promptResult.subscribeOn(AsyncUtils.VIRTUAL_SCHEDULER);
            });
        }
    }

    record AsyncCompletionSpecification() {
        static McpServerFeatures.AsyncCompletionSpecification fromSync(McpServerFeatures.SyncCompletionSpecification completion,
                                                                       boolean immediateExecution) {
            if (completion == null) {
                return null;
            }
            return new McpServerFeatures.AsyncCompletionSpecification(completion.referenceKey(), (exchange, request) -> {
                var completionResult = Mono.fromCallable(
                        () -> completion.completionHandler().apply(new McpSyncServerExchange(exchange), request));
                return immediateExecution ? completionResult
                        : completionResult.subscribeOn(AsyncUtils.VIRTUAL_SCHEDULER);
            });
        }
    }

    record Async() {
        static McpServerFeatures.Async fromSync(McpServerFeatures.Sync syncSpec, boolean immediateExecution) {
            List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();
            for (var tool : syncSpec.tools()) {
                tools.add(AsyncToolSpecification.fromSync(tool, immediateExecution));
            }

            Map<String, McpServerFeatures.AsyncResourceSpecification> resources = new HashMap<>();
            syncSpec.resources().forEach((key, resource) -> {
                resources.put(key, AsyncResourceSpecification.fromSync(resource, immediateExecution));
            });

            Map<String, McpServerFeatures.AsyncResourceTemplateSpecification> resourceTemplates = new HashMap<>();
            syncSpec.resourceTemplates().forEach((key, resource) -> {
                resourceTemplates.put(key, AsyncResourceTemplateSpecification.fromSync(resource, immediateExecution));
            });

            Map<String, McpServerFeatures.AsyncPromptSpecification> prompts = new HashMap<>();
            syncSpec.prompts().forEach((key, prompt) -> {
                prompts.put(key, AsyncPromptSpecification.fromSync(prompt, immediateExecution));
            });

            Map<McpSchema.CompleteReference, McpServerFeatures.AsyncCompletionSpecification> completions = new HashMap<>();
            syncSpec.completions().forEach((key, completion) -> {
                completions.put(key, AsyncCompletionSpecification.fromSync(completion, immediateExecution));
            });

            List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootChangeConsumers = new ArrayList<>();

            for (var rootChangeConsumer : syncSpec.rootsChangeConsumers()) {
                rootChangeConsumers.add((exchange, list) -> Mono
                        .<Void>fromRunnable(() -> rootChangeConsumer.accept(new McpSyncServerExchange(exchange), list))
                        .subscribeOn(AsyncUtils.VIRTUAL_SCHEDULER));
            }

            return new McpServerFeatures.Async(syncSpec.serverInfo(), syncSpec.serverCapabilities(), tools, resources, resourceTemplates,
                    prompts, completions, rootChangeConsumers, syncSpec.instructions());
        }
    }

}
