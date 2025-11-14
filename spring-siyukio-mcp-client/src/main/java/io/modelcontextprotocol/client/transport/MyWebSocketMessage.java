package io.modelcontextprotocol.client.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.spec.McpSchema;
import org.json.JSONObject;

/**
 * @author Bugee
 */
public record MyWebSocketMessage(
        String id,
        String mcpSessionId,
        String method,
        JSONObject body
) {

    public McpSchema.JSONRPCMessage deserializeJsonRpcMessage() {
        ObjectMapper objectMapper = JsonUtils.getObjectMapper();
        // Determine message type based on specific JSON structure
        if (this.body.has("method") && this.body.has("id")) {
            return objectMapper.convertValue(this.body, McpSchema.JSONRPCRequest.class);
        } else if (this.body.has("method") && !this.body.has("id")) {
            return objectMapper.convertValue(this.body, McpSchema.JSONRPCNotification.class);
        } else if (this.body.has("result") || this.body.has("error")) {
            return objectMapper.convertValue(this.body, McpSchema.JSONRPCResponse.class);
        }

        throw new IllegalArgumentException("Cannot deserialize JSONRPCMessage: " + JsonUtils.toPrettyJSONString(this.body));
    }
}