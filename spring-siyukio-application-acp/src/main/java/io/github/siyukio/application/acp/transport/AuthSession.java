package io.github.siyukio.application.acp.transport;

import io.github.siyukio.tools.api.token.Token;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
public class AuthSession {

    private final WebSocketSession webSocketSession;

    private final ReentrantLock lock = new ReentrantLock();

    @Getter
    private final Token token;

    public AuthSession(WebSocketSession webSocketSession, Token token) {
        this.webSocketSession = webSocketSession;
        this.token = token;
    }

    public void close() {
        try {
            this.webSocketSession.close(CloseStatus.GOING_AWAY);
        } catch (IOException ignored) {
        }
    }

    public String getId() {
        return this.webSocketSession.getId();
    }

    public void sendPing() throws IOException {
        this.lock.lock();
        try {
            this.webSocketSession.sendMessage(new PingMessage(ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8))));
            log.debug("SendPingMessage: {}", this.getId());
        } finally {
            this.lock.unlock();
        }
    }

    public void sendTextMessage(String text) {
        this.lock.lock();
        try {
            this.webSocketSession.sendMessage(new TextMessage(text, true));
        } catch (IOException e) {
            log.error("SendTextMessage io error, sessionId: {}, open: {}, message: {}", this.webSocketSession.getId(), this.webSocketSession.isOpen(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("SendTextMessage unexpected error, sessionId: {}, open: {}, message: {}", this.webSocketSession.getId(), this.webSocketSession.isOpen(), e.getMessage());
        } finally {
            this.lock.unlock();
        }
    }
}
