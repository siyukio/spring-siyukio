package io.modelcontextprotocol.server.transport;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.AsyncUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * @author Bugee
 */
@Slf4j
public class MyWebSocketSession {

    private final WebSocketSession session;

    private final CountDownLatch closeLatch = new CountDownLatch(1);

    private final BlockingQueue<WebSocketMessage<?>> messageQueue = new LinkedBlockingQueue<>();

    public MyWebSocketSession(WebSocketSession session) {
        this.session = session;
    }

    public void start() {
        AsyncUtils.submit(this::processQueue);
        AsyncUtils.submit(() -> {
            while (this.closeLatch.getCount() > 0 && this.session.isOpen()) {
                try {
                    boolean result = this.closeLatch.await(20, TimeUnit.SECONDS);
                    if (result) {
                        log.debug("closeLatch stop, {} isOpen: {}", this.session.getId(), this.session.isOpen());
                    } else {
                        log.debug("closeLatch timeout, to sendPing: {} isOpen: {}", this.session.getId(), this.session.isOpen());
                        this.messageQueue.offer(new PingMessage(ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8))));
                    }
                } catch (InterruptedException ignored) {
                }
            }
            log.debug("heartbeat stoped: {}", this.session.getId());
        });
    }

    public void sendTextMessage(String message) {
        this.messageQueue.offer(new TextMessage(message, true));
    }

    private void processQueue() {
        try {
            while (this.session.isOpen()) {
                WebSocketMessage<?> msg = messageQueue.take();
                try {
                    this.session.sendMessage(msg);
                } catch (Exception e) {
                    log.error("webSocket sendMessage error:{}", e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void close() {
        this.closeLatch.countDown();
        try {
            this.session.close();
        } catch (Exception e) {
            log.error("webSocket close error:{}", e.getMessage());
        }
    }

    public String getId() {
        return this.session.getId();
    }

    public Token getToken() {
        Object value = this.session.getAttributes().get(ApiConstants.AUTHORIZATION);
        if (value == null) {
            return null;
        }
        return (Token) value;
    }

}
