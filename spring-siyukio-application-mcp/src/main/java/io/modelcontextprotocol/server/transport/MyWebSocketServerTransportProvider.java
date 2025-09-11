/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.server.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.AsyncUtils;
import io.modelcontextprotocol.server.DefaultMcpTransportContext;
import io.modelcontextprotocol.server.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.util.Assert;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Bugee
 */
@Slf4j
public class MyWebSocketServerTransportProvider implements McpServerTransportProvider {

    private final ObjectMapper objectMapper;

    @Getter
    private final String messageEndpoint;
    private final ConcurrentHashMap<String, MyMcpServerSession> sessions = new ConcurrentHashMap<>();
    private final McpTransportContextExtractor<MyMcpServerSession> contextExtractor;
    private McpServerSession.Factory sessionFactory;
    private Consumer<Token> onConnectConsumer = null;

    private Consumer<Token> onCloseConsumer = null;

    public MyWebSocketServerTransportProvider(ObjectMapper objectMapper, String messageEndpoint, McpTransportContextExtractor<MyMcpServerSession> contextExtractor) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(messageEndpoint, "message endpoint must not be null");

        this.objectMapper = objectMapper;
        this.messageEndpoint = messageEndpoint;
        this.contextExtractor = contextExtractor;
    }

    public void onConnect(Consumer<Token> consumer) {
        this.onConnectConsumer = consumer;
    }

    public void onClose(Consumer<Token> consumer) {
        this.onCloseConsumer = consumer;
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Broadcasts a notification to all connected clients through their SSE connections.
     * The message is serialized to JSON and sent as an SSE event with type "message". If
     * any errors occur during sending to a particular client, they are logged but don't
     * prevent sending to other clients.
     *
     * @param method The method name for the notification
     * @param params The parameters for the notification
     * @return A Mono that completes when the broadcast attempt is finished
     */
    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (sessions.isEmpty()) {
            log.debug("No active sessions to broadcast message to");
            return Mono.empty();
        }

        log.debug("Attempting to broadcast message to {} active sessions", sessions.size());

        return Flux.fromIterable(sessions.values())
                .flatMap(session -> session.sendNotification(method, params)
                        .doOnError(
                                e -> log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage()))
                        .onErrorComplete())
                .then();
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Flux.fromIterable(sessions.values()).doFirst(() -> {
                    log.debug("Initiating graceful shutdown with {} active sessions", sessions.size());
                })
                .flatMap(McpServerSession::closeGracefully)
                .then()
                .doOnSuccess(v -> log.debug("Graceful shutdown completed"));
    }

    void handleConnection(MyWebSocketSession myWebSocketSession) {

        WebSocketMcpSessionTransport sessionTransport = new WebSocketMcpSessionTransport(myWebSocketSession);
        McpServerSession session = sessionFactory.create(sessionTransport);

        if (session instanceof MyMcpServerSession myMcpServerSession) {
            Token token = myWebSocketSession.getToken();
            myMcpServerSession.setToken(token);
            this.sessions.put(myWebSocketSession.getId(), myMcpServerSession);
            if (this.onConnectConsumer != null) {
                this.onConnectConsumer.accept(token);
            }
        }
    }

    void handleMessage(String sessionId, String text) {

        MyMcpServerSession session = sessions.get(sessionId);
        if (session == null) {
            log.warn("handleMessage MyMcpServerSession not found:{}", sessionId);
            return;
        }

        try {
            final McpTransportContext transportContext = this.contextExtractor.extract(session,
                    new DefaultMcpTransportContext());

            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, text);

            // Process the message through the session's handle method
            session.handle(message).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)).block(); // Block for WebMVC compatibility

        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
        }
    }

    void handleClose(String sessionId) {
        MyMcpServerSession session = this.sessions.remove(sessionId);
        if (session != null && this.onCloseConsumer != null) {
            this.onCloseConsumer.accept(session.getToken());
        }
    }

    public void close(Token token) {
        MyMcpServerSession session = this.sessions.get(token.sid);
        if (session != null) {
            session.close();
        }
    }

    public void closeIdle(Duration unusedTime) {
        long maxTime = unusedTime.toMillis();
        AsyncUtils.scheduleWithFixedDelay(() -> {
            long currentTime = System.currentTimeMillis();
            Map<String, MyMcpServerSession> unusedMap = new HashMap<>();
            for (Map.Entry<String, MyMcpServerSession> entry : this.sessions.entrySet()) {
                if (currentTime - entry.getValue().getLastActiveTime() > maxTime) {
                    unusedMap.put(entry.getKey(), entry.getValue());
                }
            }
            for (Map.Entry<String, MyMcpServerSession> entry : unusedMap.entrySet()) {
                entry.getValue().close();
                log.debug("remove unused myMcpServerSession:{},{}", entry.getKey(), entry.getValue().getToken().uid);
            }
        }, 30, 60, TimeUnit.SECONDS);
    }

    public class WebSocketMcpSessionTransport implements McpServerTransport {

        private final MyWebSocketSession session;

        WebSocketMcpSessionTransport(MyWebSocketSession session) {
            this.session = session;
        }

        public String getSessionId() {
            return this.session.getId();
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            try {
                String jsonText = objectMapper.writeValueAsString(message);
                this.session.sendTextMessage(jsonText);
            } catch (Exception e) {
                log.error("myWebSocketMcpSessionTransport sendMessage error:", e);
            }
            return Mono.empty();
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return objectMapper.convertValue(data, typeRef);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(() -> {
                log.debug("Closing session transport: {}", this.session.getId());
                this.session.close();
            });
        }

        @Override
        public void close() {
            try {
                this.session.close();
                log.debug("Successfully close for session {}", this.session.getId());
            } catch (Exception e) {
                log.warn("Failed to close for session {}: {}", this.session.getId(), e.getMessage());
            }
        }

    }

}
