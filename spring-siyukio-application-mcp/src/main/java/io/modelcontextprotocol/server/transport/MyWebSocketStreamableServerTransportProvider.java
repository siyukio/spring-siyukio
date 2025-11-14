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
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
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

    private final MyWebsocketStreamableContext myWebsocketStreamableContext;

    @Getter
    private final String mcpEndpoint;
    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> webSocketAndSessionMap = new ConcurrentHashMap<>();
    private final McpTransportContextExtractor<MyWebSocketServerSession> contextExtractor;
    private final ScheduledFuture<?> keepAliveScheduler;
    private McpStreamableServerSession.Factory sessionFactory;
    private volatile boolean isClosing = false;


    public MyWebSocketStreamableServerTransportProvider(ObjectMapper objectMapper, MyWebsocketStreamableContext myWebsocketStreamableContext, String mcpEndpoint,
                                                        McpTransportContextExtractor<MyWebSocketServerSession> contextExtractor,
                                                        Duration keepAliveInterval) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
        Assert.notNull(contextExtractor, "McpTransportContextExtractor must not be null");

        this.objectMapper = objectMapper;
        this.mcpEndpoint = mcpEndpoint;
        this.contextExtractor = contextExtractor;

        this.myWebsocketStreamableContext = myWebsocketStreamableContext;
        myWebsocketStreamableContext.setMyWebSocketStreamableServerTransportProvider(this);

        this.keepAliveScheduler = AsyncUtils.scheduleWithFixedDelay(this::keepAlive, 3, keepAliveInterval.getSeconds(), TimeUnit.SECONDS);
    }

    private void keepAlive() {
        MyWebSocketHandler myWebSocketHandler = this.myWebsocketStreamableContext.getMyWebSocketHandler();
        if (myWebSocketHandler != null) {
            myWebSocketHandler.keepAlive();
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

    void handlePost(MyWebSocketServerSession myWebSocketServerSession, MyWebSocketMessage inputMessage) {
        if (this.isClosing) {
            myWebSocketServerSession.sendError(inputMessage.id(), ApiException.getApiException(HttpStatus.SERVICE_UNAVAILABLE, "Server is shutting down"));
        }

        McpTransportContext transportContext = this.contextExtractor.extract(myWebSocketServerSession);

        try {
            McpSchema.JSONRPCMessage message = inputMessage.deserializeJsonRpcMessage();

            // Handle initialization request
            if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest
                    && jsonrpcRequest.method().equals(McpSchema.METHOD_INITIALIZE)) {
                McpSchema.InitializeRequest initializeRequest = objectMapper.convertValue(jsonrpcRequest.params(),
                        new TypeReference<McpSchema.InitializeRequest>() {
                        });
                McpStreamableServerSession.McpStreamableServerSessionInit init = this.sessionFactory
                        .startSession(initializeRequest);
                this.sessions.put(init.session().getId(), init.session());
                this.webSocketAndSessionMap.put(myWebSocketServerSession.getId(), init.session().getId());
                try {
                    McpSchema.InitializeResult initResult = init.initResult().block();

                    myWebSocketServerSession.sendResponse(inputMessage.id(), init.session().getId(),
                            new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, jsonrpcRequest.id(), initResult,
                                    null));
                } catch (Exception e) {
                    log.error("Failed to initialize websocket session: {}", e.getMessage());
                    myWebSocketServerSession.sendError(inputMessage.id(), ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                }
                return;
            }

            // Handle other messages that require a session
            if (!StringUtils.hasText(inputMessage.mcpSessionId())) {
                myWebSocketServerSession.sendError(inputMessage.id(), ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Session ID missing"));
            }

            String sessionId = inputMessage.mcpSessionId();
            McpStreamableServerSession session = this.sessions.get(sessionId);

            if (session == null) {
                myWebSocketServerSession.sendError(inputMessage.id(), ApiException.getApiException(HttpStatus.NOT_FOUND, "Session not found: " + sessionId));
                return;
            }

            if (message instanceof McpSchema.JSONRPCResponse jsonrpcResponse) {
                session.accept(jsonrpcResponse)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                log.debug("received McpSchema.JSONRPCResponse:{}", jsonrpcResponse);
                myWebSocketServerSession.sendAccepted(inputMessage.id());
            } else if (message instanceof McpSchema.JSONRPCNotification jsonrpcNotification) {
                session.accept(jsonrpcNotification)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                log.debug("received McpSchema.JSONRPCNotification:{}", jsonrpcNotification);
                myWebSocketServerSession.sendAccepted(inputMessage.id());
            } else if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {

                WebSocketStreamableMcpSessionTransport sessionTransport = new WebSocketStreamableMcpSessionTransport(
                        myWebSocketServerSession, inputMessage.id(), inputMessage.mcpSessionId());

                try {
                    session.responseStream(jsonrpcRequest, sessionTransport)
                            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                            .block();
                } catch (Exception e) {
                    log.error("Failed to handle websocket request stream: {}", e.getMessage());
                    myWebSocketServerSession.sendError(inputMessage.id(), ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                }
            } else {
                myWebSocketServerSession.sendError(inputMessage.id(), ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown message type"));
            }
        } catch (IllegalArgumentException e) {
            log.error("Failed to deserialize websocket message: {}", e.getMessage());
            myWebSocketServerSession.sendError(inputMessage.id(), ApiException.getApiException(HttpStatus.BAD_REQUEST, "Invalid message format"));
        } catch (Exception e) {
            log.error("Error handling websocket message: {}", e.getMessage());
            myWebSocketServerSession.sendError(inputMessage.id(), ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    public void handleDelete(MyWebSocketServerSession myWebSocketServerSession, MyWebSocketMessage inputMessage) {

        if (this.isClosing) {
            myWebSocketServerSession.sendError(inputMessage.id(), ApiException.getApiException(HttpStatus.SERVICE_UNAVAILABLE, "Server is shutting down"));
        }

        McpTransportContext transportContext = this.contextExtractor.extract(myWebSocketServerSession);

        String sessionId = inputMessage.mcpSessionId();
        McpStreamableServerSession session = this.sessions.remove(sessionId);

        if (session == null) {
            return;
        }

        this.webSocketAndSessionMap.remove(myWebSocketServerSession.getId());
        try {
            session.delete().contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)).block();
            myWebSocketServerSession.sendAccepted(inputMessage.id());
        } catch (Exception e) {
            log.error("Failed to delete websocket session {}: {}", sessionId, e.getMessage());
        }
    }

    public void onCloseWhenKeepAlive(MyWebSocketServerSession myWebSocketServerSession) {
        String sessionId = this.webSocketAndSessionMap.remove(myWebSocketServerSession.getId());
        if (StringUtils.hasText(sessionId)) {
            log.error("onCloseWhenKeepAlive websocket session {}: {}", myWebSocketServerSession.getId(), sessionId);
            McpStreamableServerSession session = this.sessions.remove(sessionId);
            if (session == null) {
                return;
            }
            try {
                session.delete().block();
            } catch (Exception ignored) {
            }
        }
    }

    public class WebSocketStreamableMcpSessionTransport implements McpStreamableServerTransport {

        private final MyWebSocketServerSession myWebSocketServerSession;

        private final String requestId;

        private final String mcpSessionId;

        private final ReentrantLock lock = new ReentrantLock();

        private volatile boolean closed = false;

        WebSocketStreamableMcpSessionTransport(MyWebSocketServerSession myWebSocketServerSession, String requestId, String mcpSessionId) {
            this.myWebSocketServerSession = myWebSocketServerSession;
            this.requestId = requestId;
            this.mcpSessionId = mcpSessionId;
        }

        private void sendWebSocketMessage(McpSchema.JSONRPCMessage message) {
            if (message instanceof McpSchema.JSONRPCResponse response) {
                this.myWebSocketServerSession.sendResponse(this.requestId, response);
            } else {
                this.myWebSocketServerSession.sendRequestOrNotificationMessage(message);
            }
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return sendMessage(message, null);
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId) {
            return Mono.fromRunnable(() -> {
                if (this.closed) {
                    log.debug("Attempted to send message to closed mcp websocket session: {}", this.mcpSessionId);
                    return;
                }
                this.lock.lock();
                try {
                    if (this.closed) {
                        log.debug("Mcp websocket session {} was closed during message send attempt", this.mcpSessionId);
                        return;
                    }

                    this.sendWebSocketMessage(message);
                    log.debug("Message sent to mcp websocket session {} with ID {}", this.mcpSessionId, messageId);
                } catch (Exception e) {
                    //todo
                    log.error("Failed to send message to mcp websocket session {}: {}", this.mcpSessionId, e.getMessage());
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
                    log.debug("Mcp websocket session transport {} already closed", this.mcpSessionId);
                    return;
                }

                this.closed = true;
                log.debug("Successfully completed for mcp websocket session {}", this.mcpSessionId);
            } catch (Exception e) {
                log.warn("Failed to complete for mcp websocket  session {}: {}", this.mcpSessionId, e.getMessage());
            }
        }
    }

}
