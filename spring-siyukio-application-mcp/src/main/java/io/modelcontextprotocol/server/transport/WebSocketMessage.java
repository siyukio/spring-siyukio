package io.modelcontextprotocol.server.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.siyukio.tools.util.XDataUtils;
import io.modelcontextprotocol.spec.McpSchema;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

/**
 * @author Bugee
 */
public record WebSocketMessage(
        String id,
        String mcpSessionId,
        String method,
        JSONObject body
) {

    public WebSocketMessage(String id, JSONObject body) {
        this(id, null, null, body);
    }

    public WebSocketMessage(String id, String mcpSessionId, JSONObject body) {
        this(id, mcpSessionId, null, body);
    }

    public WebSocketMessage(String id) {
        this(id, null, null, null);
    }

    public McpSchema.JSONRPCMessage deserializeJsonRpcMessage() {
        ObjectMapper objectMapper = XDataUtils.getObjectMapper();
        // Determine message type based on specific JSON structure
        if (this.body.has("method") && this.body.has("id")) {
            return objectMapper.convertValue(this.body, McpSchema.JSONRPCRequest.class);
        } else if (this.body.has("method") && !this.body.has("id")) {
            return objectMapper.convertValue(this.body, McpSchema.JSONRPCNotification.class);
        } else if (this.body.has("result") || this.body.has("error")) {
            return objectMapper.convertValue(this.body, McpSchema.JSONRPCResponse.class);
        }

        throw new IllegalArgumentException("Cannot deserialize JSONRPCMessage: " + XDataUtils.toPrettyJSONString(this.body));
    }

    public boolean toDelete() {
        return StringUtils.hasText(this.method) && this.method.equals("delete");
    }
}