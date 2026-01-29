package io.modelcontextprotocol.server.transport;

import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author Bugee
 */
@Slf4j
public class WebSocketServerSession {

    private final WebSocketSession webSocketSession;

    private final ReentrantLock lock = new ReentrantLock();

    @Getter
    private final Token token;

    public WebSocketServerSession(WebSocketSession webSocketSession, Token token) {
        this.webSocketSession = webSocketSession;
        this.token = token;
    }

    public void close() throws IOException {
        this.webSocketSession.close(CloseStatus.PROTOCOL_ERROR);
    }

    public String getId() {
        return this.webSocketSession.getId();
    }

    public void sendPing() throws IOException {
        this.lock.lock();
        try {
            this.webSocketSession.sendMessage(new PingMessage(ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8))));
            log.debug("sendPingMessage: {}", this.getId());
        } finally {
            this.lock.unlock();
        }
    }

    private void sendTextMessage(String text) {
        this.lock.lock();
        try {
            this.webSocketSession.sendMessage(new TextMessage(text, true));
            log.debug("sendTextMessage: {}", text);
        } catch (IOException e) {
            log.error("sendTextMessage io error, open: {}, message: {}", this.webSocketSession.isOpen(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("sendTextMessage unexpected error, open: {}, message: {}", this.webSocketSession.isOpen(), e.getMessage());
            if (!this.webSocketSession.isOpen()) {
                throw new WebSocketUnexpectedClosedException(this.getId(), e);
            }
        } finally {
            this.lock.unlock();
        }
    }

    public void sendError(String id, ApiException apiException) {
        McpSchema.JSONRPCResponse.JSONRPCError error = new McpSchema.JSONRPCResponse.JSONRPCError(
                apiException.getCode(), apiException.getMessage(), null);
        McpSchema.JSONRPCResponse response = new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, "", "", error);
        JSONObject body = XDataUtils.copy(response, JSONObject.class);
        WebSocketMessage webSocketMessage = new WebSocketMessage(id, body);
        String text = XDataUtils.toJSONString(webSocketMessage);
        this.sendTextMessage(text);
    }

    public void sendResponse(String id, McpSchema.JSONRPCResponse response) {
        this.sendResponse(id, null, response);
    }

    public void sendResponse(String id, String mcpSessionId, McpSchema.JSONRPCResponse response) {
        JSONObject body = XDataUtils.copy(response, JSONObject.class);
        WebSocketMessage webSocketMessage = new WebSocketMessage(id, mcpSessionId, body);
        String text = XDataUtils.toJSONString(webSocketMessage);
        this.sendTextMessage(text);
    }

    public void sendRequestOrNotificationMessage(McpSchema.JSONRPCMessage message) {
        String id = IdUtils.getUniqueId();
        JSONObject body = XDataUtils.copy(message, JSONObject.class);
        WebSocketMessage webSocketMessage = new WebSocketMessage(id, body);
        String text = XDataUtils.toJSONString(webSocketMessage);
        this.sendTextMessage(text);
    }

    public void sendAccepted(String id) {
        WebSocketMessage webSocketMessage = new WebSocketMessage(id);
        String text = XDataUtils.toJSONString(webSocketMessage);
        this.sendTextMessage(text);
    }
}
