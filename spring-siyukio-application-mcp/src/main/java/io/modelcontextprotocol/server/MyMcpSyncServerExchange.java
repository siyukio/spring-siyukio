package io.modelcontextprotocol.server;

import io.github.siyukio.tools.api.token.Token;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.MyMcpSchema;
import io.modelcontextprotocol.spec.MyMcpServerSession;

/**
 * @author Bugee
 */
public class MyMcpSyncServerExchange extends McpSyncServerExchange {

    private final McpAsyncServerExchange exchange;

    /**
     * Create a new synchronous exchange with the client using the provided asynchronous
     * implementation as a delegate.
     *
     * @param exchange The asynchronous exchange to delegate to.
     */
    public MyMcpSyncServerExchange(McpAsyncServerExchange exchange) {
        super(exchange);
        this.exchange = exchange;
    }

    public McpAsyncServerExchange getMcpAsyncServerExchange() {
        return this.exchange;
    }

    public Token getToken() {
        if (this.exchange instanceof MyMcpAsyncServerExchange myMcpAsyncServerExchange) {
            McpServerSession mcpServerSession = myMcpAsyncServerExchange.getSession();
            if (mcpServerSession instanceof MyMcpServerSession myMcpServerSession) {
                return myMcpServerSession.getToken();
            }
        }
        return null;
    }

    public void progressNotification(MyMcpSchema.ProgressMessageNotification progressMessageNotification) {
        if (this.exchange instanceof MyMcpAsyncServerExchange myMcpAsyncServerExchange) {
            myMcpAsyncServerExchange.progressNotification(progressMessageNotification).block();
        }

    }

}
