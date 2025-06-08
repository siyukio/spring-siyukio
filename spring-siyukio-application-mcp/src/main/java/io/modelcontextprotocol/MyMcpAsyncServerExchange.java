package io.modelcontextprotocol;

import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import lombok.Getter;

/**
 * @author Bugee
 */

@Getter
public class MyMcpAsyncServerExchange extends McpAsyncServerExchange {

    private final McpServerSession session;

    /**
     * Create a new asynchronous exchange with the client.
     *
     * @param session            The server session representing a 1-1 interaction.
     * @param clientCapabilities The client capabilities that define the supported
     *                           features and functionality.
     * @param clientInfo         The client implementation information.
     */
    public MyMcpAsyncServerExchange(McpServerSession session, McpSchema.ClientCapabilities clientCapabilities, McpSchema.Implementation clientInfo) {
        super(session, clientCapabilities, clientInfo);
        this.session = session;
    }

}
