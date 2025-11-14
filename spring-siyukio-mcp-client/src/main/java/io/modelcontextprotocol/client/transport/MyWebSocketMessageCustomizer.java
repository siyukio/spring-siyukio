package io.modelcontextprotocol.client.transport;

import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import org.json.JSONObject;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

/**
 * @author Bugee
 */
public interface MyWebSocketMessageCustomizer {

    Noop NOOP = new Noop();

    Publisher<MyWebSocketMessage> customize(String method, String mcpSessionId,
                                            @Nullable McpSchema.JSONRPCMessage sentMessage, McpTransportContext context);

    class Noop implements MyWebSocketMessageCustomizer {

        @Override
        public Publisher<MyWebSocketMessage> customize(String method, String mcpSessionId, McpSchema.JSONRPCMessage sentMessage, McpTransportContext context) {
            String id = IdUtils.getUniqueId();

            JSONObject body = null;
            if (sentMessage != null) {
                body = JsonUtils.copy(sentMessage, JSONObject.class);
            }
            MyWebSocketMessage myWebSocketMessage = new MyWebSocketMessage(id, mcpSessionId, method, body);
            return Mono.just(myWebSocketMessage);
        }
    }
}
