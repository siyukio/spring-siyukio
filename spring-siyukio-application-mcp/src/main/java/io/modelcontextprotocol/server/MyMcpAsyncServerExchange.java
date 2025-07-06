package io.modelcontextprotocol.server;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.MyMcpSchema;
import lombok.Getter;
import reactor.core.publisher.Mono;

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

    public Mono<Void> progressNotification(MyMcpSchema.ProgressMessageNotification progressMessageNotification) {

        if (progressMessageNotification == null) {
            return Mono.error(new McpError("progress message must not be null"));
        }

        return Mono.defer(() -> {
            return this.session.sendNotification(MyMcpSchema.METHOD_NOTIFICATION_PROGRESS, progressMessageNotification);
        });
    }

}
