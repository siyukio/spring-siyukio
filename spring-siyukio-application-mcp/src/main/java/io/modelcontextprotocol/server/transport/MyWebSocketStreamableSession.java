package io.modelcontextprotocol.server.transport;

import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
public class MyWebSocketStreamableSession {

    private final WebSocketSession session;

    private final ReentrantLock lock = new ReentrantLock();

    public MyWebSocketStreamableSession(WebSocketSession session) {
        this.session = session;
    }

    public void close() {
        try {
            this.session.close();
        } catch (IOException ignored) {

        }
    }

    public String getId() {
        return this.session.getId();
    }

    public Token getToken() {
        Object value = this.session.getAttributes().get(HttpHeaders.AUTHORIZATION);
        if (value == null) {
            return null;
        }
        return (Token) value;
    }

    public void sendPing() throws IOException {
        this.lock.lock();
        try {
            this.session.sendMessage(new PingMessage(ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8))));
            log.debug("sendPingMessage: {}", this.session.getId());
        } finally {
            this.lock.unlock();
        }
    }

    public void sendTextMessage(String text) throws IOException {
        this.lock.lock();
        try {
            this.session.sendMessage(new TextMessage(text, true));
            log.debug("sendTextMessage: {}", text);
        } finally {
            this.lock.unlock();
        }
    }

    public void sendError(ApiException apiException) {
        McpSchema.JSONRPCResponse.JSONRPCError error = new McpSchema.JSONRPCResponse.JSONRPCError(
                apiException.getCode(), apiException.getMessage(), null);
        McpSchema.JSONRPCResponse response = new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, "mcp-error", "", error);
        String text = JsonUtils.toJSONString(response);
        try {
            this.sendTextMessage(text);
        } catch (IOException ignored) {
        }
    }

    public void sendResponse(McpSchema.JSONRPCResponse response) {
        String text = JsonUtils.toJSONString(response);
        try {
            this.sendTextMessage(text);
        } catch (IOException ignored) {
        }
    }

    public void sendMcpSession() {
        McpSchema.JSONRPCResponse response = new McpSchema.JSONRPCResponse(
                McpSchema.JSONRPC_VERSION, "mcp-session-id", this.session.getId(), null);
        this.sendResponse(response);
    }

}
