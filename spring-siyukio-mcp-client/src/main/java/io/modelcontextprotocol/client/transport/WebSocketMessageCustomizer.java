package io.modelcontextprotocol.client.transport;

import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import org.json.JSONObject;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

/**
 * @author Bugee
 */
public interface WebSocketMessageCustomizer {

    Noop NOOP = new Noop();

    Publisher<WebSocketMessage> customize(String method, String mcpSessionId,
                                          @Nullable McpSchema.JSONRPCMessage sentMessage, McpTransportContext context);

    class Noop implements WebSocketMessageCustomizer {

        @Override
        public Publisher<WebSocketMessage> customize(String method, String mcpSessionId, McpSchema.JSONRPCMessage sentMessage, McpTransportContext context) {
            String id = IdUtils.getUniqueId();

            JSONObject body = null;
            if (sentMessage != null) {
                body = XDataUtils.copy(sentMessage, JSONObject.class);
            }
            WebSocketMessage webSocketMessage = new WebSocketMessage(id, mcpSessionId, method, body);
            return Mono.just(webSocketMessage);
        }
    }
}
