package io.modelcontextprotocol.server;

import io.github.siyukio.tools.api.token.Token;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.MyMcpSchema;
import io.modelcontextprotocol.spec.MyMcpServerSession;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bugee
 */
public class MyMcpSyncServerExchange extends McpSyncServerExchange {

    public final static String REPLY_NAME = "_replay";

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

    @Override
    public McpSchema.CreateMessageResult createMessage(McpSchema.CreateMessageRequest createMessageRequest) {
        return this.exchange.createMessage(createMessageRequest).block();
    }

    public void createMessageNoReply(McpSchema.CreateMessageRequest createMessageRequest) {
        Map<String, Object> metadata = createMessageRequest.metadata();
        if (metadata == null) {
            metadata = new HashMap<>();
            createMessageRequest = McpSchema.CreateMessageRequest.builder()
                    .messages(createMessageRequest.messages())
                    .modelPreferences(createMessageRequest.modelPreferences())
                    .systemPrompt(createMessageRequest.systemPrompt())
                    .includeContext(createMessageRequest.includeContext())
                    .temperature(createMessageRequest.temperature())
                    .maxTokens(createMessageRequest.maxTokens())
                    .stopSequences(createMessageRequest.stopSequences())
                    .metadata(metadata)
                    .build();
        }
        metadata.put(REPLY_NAME, false);
        this.exchange.createMessage(createMessageRequest);
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
