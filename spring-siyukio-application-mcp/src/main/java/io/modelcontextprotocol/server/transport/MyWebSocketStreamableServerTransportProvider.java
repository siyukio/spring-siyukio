/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.server.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.util.AsyncUtils;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.util.Assert;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Bugee
 */
@Slf4j
public class MyWebSocketStreamableServerTransportProvider implements McpStreamableServerTransportProvider {

    private final ObjectMapper objectMapper;

    @Getter
    private final String mcpEndpoint;
    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MyWebSocketStreamableSession> webSocketSessions = new ConcurrentHashMap<>();
    private final McpTransportContextExtractor<MyWebSocketStreamableSession> contextExtractor;
    private final ScheduledFuture<?> keepAliveScheduler;
    private McpStreamableServerSession.Factory sessionFactory;
    private volatile boolean isClosing = false;


    public MyWebSocketStreamableServerTransportProvider(ObjectMapper objectMapper, String mcpEndpoint,
                                                        McpTransportContextExtractor<MyWebSocketStreamableSession> contextExtractor,
                                                        Duration keepAliveInterval) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
        Assert.notNull(contextExtractor, "McpTransportContextExtractor must not be null");

        this.objectMapper = objectMapper;
        this.mcpEndpoint = mcpEndpoint;
        this.contextExtractor = contextExtractor;

        this.keepAliveScheduler = AsyncUtils.scheduleWithFixedDelay(this::keepAlive, 3, keepAliveInterval.getSeconds(), TimeUnit.SECONDS);
    }

    private void keepAlive() {
        if (this.webSocketSessions.isEmpty()) {
            return;
        }
        log.debug("keepAliveScheduler execute...");
        Set<String> exceptionIdSet = new HashSet<>();
        for (MyWebSocketStreamableSession webSocketSession : this.webSocketSessions.values()) {
            try {
                webSocketSession.sendPing();
            } catch (Exception ex) {
                exceptionIdSet.add(webSocketSession.getId());
            }
        }
        for (String exceptionId : exceptionIdSet) {
            this.webSocketSessions.remove(exceptionId);
        }
    }

    @Override
    public List<String> protocolVersions() {
        return List.of(ProtocolVersions.MCP_2024_11_05, ProtocolVersions.MCP_2025_03_26);
    }

    @Override
    public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
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
        if (this.sessions.isEmpty()) {
            log.debug("No active websocket sessions to broadcast message to");
            return Mono.empty();
        }

        log.debug("Attempting to broadcast message to {} active websocket sessions", this.sessions.size());

        return Mono.fromRunnable(() -> {
            this.sessions.values().parallelStream().forEach(session -> {
                try {
                    session.sendNotification(method, params).block();
                } catch (Exception e) {
                    log.error("Failed to send message to websocket session {}: {}", session.getId(), e.getMessage());
                }
            });
        });
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            this.isClosing = true;
            log.debug("Initiating graceful shutdown with {} active websocket sessions", this.sessions.size());

            this.sessions.values().parallelStream().forEach(session -> {
                try {
                    session.closeGracefully().block();
                } catch (Exception e) {
                    log.error("Failed to close websocket session {}: {}", session.getId(), e.getMessage());
                }
            });

            this.sessions.clear();
            log.debug("Websocket graceful shutdown completed");
        }).then().doOnSuccess(v -> {
            if (this.keepAliveScheduler != null) {
                this.keepAliveScheduler.cancel(true);
            }
        });
    }

    void handlePost(MyWebSocketStreamableSession myWebSocketStreamableSession, String body) {
        if (this.isClosing) {
            myWebSocketStreamableSession.sendError(ApiException.getApiException(HttpStatus.SERVICE_UNAVAILABLE, "Server is shutting down"));
        }

        this.webSocketSessions.put(myWebSocketStreamableSession.getId(), myWebSocketStreamableSession);

        McpTransportContext transportContext = this.contextExtractor.extract(myWebSocketStreamableSession);

        try {
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);

            // Handle initialization request
            if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest
                    && jsonrpcRequest.method().equals(McpSchema.METHOD_INITIALIZE)) {
                McpSchema.InitializeRequest initializeRequest = objectMapper.convertValue(jsonrpcRequest.params(),
                        new TypeReference<McpSchema.InitializeRequest>() {
                        });
                McpStreamableServerSession.McpStreamableServerSessionInit init = this.sessionFactory
                        .startSession(initializeRequest);
                this.sessions.put(myWebSocketStreamableSession.getId(), init.session());

                try {
                    McpSchema.InitializeResult initResult = init.initResult().block();
                    myWebSocketStreamableSession.sendResponse(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, jsonrpcRequest.id(), initResult,
                            null));
                } catch (Exception e) {
                    log.error("Failed to initialize websocket session: {}", e.getMessage());
                    myWebSocketStreamableSession.sendError(ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                }
                return;
            }

            // Handle other messages that require a session

            String sessionId = myWebSocketStreamableSession.getId();
            McpStreamableServerSession session = this.sessions.get(sessionId);

            if (session == null) {
                myWebSocketStreamableSession.sendError(ApiException.getApiException(HttpStatus.NOT_FOUND, "Session not found: " + sessionId));
                return;
            }

            if (message instanceof McpSchema.JSONRPCResponse jsonrpcResponse) {
                session.accept(jsonrpcResponse)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                log.debug("received McpSchema.JSONRPCResponse:{}", jsonrpcResponse);
            } else if (message instanceof McpSchema.JSONRPCNotification jsonrpcNotification) {
                session.accept(jsonrpcNotification)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                log.debug("received McpSchema.JSONRPCNotification:{}", jsonrpcNotification);
                if (jsonrpcNotification.method().equals(McpSchema.METHOD_NOTIFICATION_INITIALIZED)) {
                    myWebSocketStreamableSession.sendMcpSession();
                }
            } else if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {

                WebSocketStreamableMcpSessionTransport sessionTransport = new WebSocketStreamableMcpSessionTransport(
                        myWebSocketStreamableSession);

                try {
                    session.responseStream(jsonrpcRequest, sessionTransport)
                            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                            .block();
                } catch (Exception e) {
                    log.error("Failed to handle websocket request stream: {}", e.getMessage());
                    myWebSocketStreamableSession.sendError(ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                }
            } else {
                myWebSocketStreamableSession.sendError(ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown message type"));
            }
        } catch (IllegalArgumentException | IOException e) {
            log.error("Failed to deserialize websocket message: {}", e.getMessage());
            myWebSocketStreamableSession.sendError(ApiException.getApiException(HttpStatus.BAD_REQUEST, "Invalid message format"));
        } catch (Exception e) {
            log.error("Error handling websocket message: {}", e.getMessage());
            myWebSocketStreamableSession.sendError(ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    public void handleDelete(MyWebSocketStreamableSession myWebSocketStreamableSession) {
        this.webSocketSessions.remove(myWebSocketStreamableSession.getId());

        if (this.isClosing) {
            myWebSocketStreamableSession.sendError(ApiException.getApiException(HttpStatus.SERVICE_UNAVAILABLE, "Server is shutting down"));
        }

        McpTransportContext transportContext = this.contextExtractor.extract(myWebSocketStreamableSession);

        String sessionId = myWebSocketStreamableSession.getId();
        McpStreamableServerSession session = this.sessions.get(sessionId);

        if (session == null) {
            return;
        }

        try {
            session.delete().contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)).block();
            this.sessions.remove(sessionId);
        } catch (Exception e) {
            log.error("Failed to delete websocket session {}: {}", sessionId, e.getMessage());
        }
    }

    public class WebSocketStreamableMcpSessionTransport implements McpStreamableServerTransport {

        private final MyWebSocketStreamableSession session;

        private final ReentrantLock lock = new ReentrantLock();

        private volatile boolean closed = false;

        WebSocketStreamableMcpSessionTransport(MyWebSocketStreamableSession session) {
            this.session = session;
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return sendMessage(message, null);
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId) {
            return Mono.fromRunnable(() -> {
                if (this.closed) {
                    log.debug("Attempted to send message to closed websocket session: {}", this.session.getId());
                    return;
                }
                this.lock.lock();
                try {
                    if (this.closed) {
                        log.debug("Websocket session {} was closed during message send attempt", this.session.getId());
                        return;
                    }

                    String jsonText = objectMapper.writeValueAsString(message);
                    this.session.sendTextMessage(jsonText);
                    log.debug("Message sent to websocket session {} with ID {}", this.session.getId(), messageId);
                } catch (Exception e) {
                    //todo
                    log.error("Failed to send message to websocket session {}: {}", this.session.getId(), e.getMessage());
                } finally {
                    this.lock.unlock();
                }
            });
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return objectMapper.convertValue(data, typeRef);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(WebSocketStreamableMcpSessionTransport.this::close);
        }

        @Override
        public void close() {
            try {
                if (this.closed) {
                    log.debug("Websocket session transport {} already closed", this.session.getId());
                    return;
                }

                this.closed = true;
                log.debug("Successfully completed for websocket session {}", this.session.getId());
            } catch (Exception e) {
                log.warn("Failed to complete for websocket  session {}: {}", this.session.getId(), e.getMessage());
            }
        }
    }

}
