package io.modelcontextprotocol.server;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;
import io.modelcontextprotocol.util.Assert;

/**
 *
 * @author Bugee
 */
public class CustomStreamableSyncSpecification extends McpServer.SyncSpecification<McpServer.StreamableSyncSpecification> {

    private final McpStreamableServerTransportProvider transportProvider;

    public CustomStreamableSyncSpecification(McpStreamableServerTransportProvider transportProvider) {
        Assert.notNull(transportProvider, "Transport provider must not be null");
        this.transportProvider = transportProvider;
    }

    @Override
    public McpSyncServer build() {
        McpServerFeatures.Sync syncFeatures = new McpServerFeatures.Sync(this.serverInfo, this.serverCapabilities,
                this.tools, this.resources, this.resourceTemplates, this.prompts, this.completions,
                this.rootsChangeHandlers, this.instructions);
        McpServerFeatures.Async asyncFeatures = CustomMcpServerFeatures.Async.fromSync(syncFeatures,
                this.immediateExecution);
        var jsonSchemaValidator = this.jsonSchemaValidator != null ? this.jsonSchemaValidator
                : JsonSchemaValidator.getDefault();
        var asyncServer = new McpAsyncServer(transportProvider,
                jsonMapper == null ? McpJsonMapper.getDefault() : jsonMapper, asyncFeatures, this.requestTimeout,
                this.uriTemplateManagerFactory, jsonSchemaValidator);
        return new McpSyncServer(asyncServer, this.immediateExecution);
    }
}
