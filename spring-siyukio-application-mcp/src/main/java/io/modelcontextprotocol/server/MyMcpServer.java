package io.modelcontextprotocol.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.util.Assert;

/**
 * @author Bugee
 */
public interface MyMcpServer {

    /**
     * Starts building a synchronous MCP server that provides blocking operations.
     * Synchronous servers block the current Thread's execution upon each request before
     * giving the control back to the caller, making them simpler to implement but
     * potentially less scalable for concurrent operations.
     *
     * @param transportProvider The transport layer implementation for MCP communication.
     * @return A new instance of {@link McpServer.SyncSpecification} for configuring the server.
     */
    static MySingleSessionSyncSpecification sync(McpServerTransportProvider transportProvider) {
        return new MySingleSessionSyncSpecification(transportProvider);
    }

    class MySingleSessionSyncSpecification extends McpServer.SyncSpecification<MyMcpServer.MySingleSessionSyncSpecification> {

        private final McpServerTransportProvider transportProvider;

        private MySingleSessionSyncSpecification(McpServerTransportProvider transportProvider) {
            Assert.notNull(transportProvider, "Transport provider must not be null");
            this.transportProvider = transportProvider;
        }

        /**
         * Builds a synchronous MCP server that provides blocking operations.
         *
         * @return A new instance of {@link McpSyncServer} configured with this builder's
         * settings.
         */
        @Override
        public McpSyncServer build() {
            McpServerFeatures.Sync syncFeatures = new McpServerFeatures.Sync(this.serverInfo, this.serverCapabilities,
                    this.tools, this.resources, this.resourceTemplates, this.prompts, this.completions,
                    this.rootsChangeHandlers, this.instructions);
            McpServerFeatures.Async asyncFeatures = MyMcpServerFeatures.Async.fromSync(syncFeatures,
                    this.immediateExecution);
            var mapper = this.objectMapper != null ? this.objectMapper : new ObjectMapper();
            var jsonSchemaValidator = this.jsonSchemaValidator != null ? this.jsonSchemaValidator
                    : new DefaultJsonSchemaValidator(mapper);

            var asyncServer = new MyMcpAsyncServer(this.transportProvider, mapper, asyncFeatures, this.requestTimeout,
                    this.uriTemplateManagerFactory, jsonSchemaValidator);

            return new McpSyncServer(asyncServer, this.immediateExecution);
        }

    }
}
