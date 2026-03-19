package io.modelcontextprotocol.server;

import io.modelcontextprotocol.spec.McpStreamableServerTransportProvider;

/**
 *
 * @author Bugee
 */
public interface CustomMcpServer {

    static CustomStreamableSyncSpecification sync(McpStreamableServerTransportProvider transportProvider) {
        return new CustomStreamableSyncSpecification(transportProvider);
    }
}
