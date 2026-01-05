package io.modelcontextprotocol.client.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.siyukio.tools.util.XDataUtils;
import io.modelcontextprotocol.spec.McpSchema;
import org.json.JSONObject;

/**
 * @author Bugee
 */
public record WebSocketMessage(
        String id,
        String mcpSessionId,
        String method,
        JSONObject body
) {

    public McpSchema.JSONRPCMessage deserializeJsonRpcMessage() {
        ObjectMapper objectMapper = XDataUtils.OBJECT_MAPPER;
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
}