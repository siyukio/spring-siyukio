/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.server.transport;

import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.util.AsyncUtils;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
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
public class WebSocketStreamableServerTransportProvider implements McpStreamableServerTransportProvider {

    private final McpJsonMapper mcpJsonMapper;

    private final WebSocketStreamableContext webSocketStreamableContext;

    @Getter
    private final String mcpEndpoint;
    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> webSocketAndSessionMap = new ConcurrentHashMap<>();
    private final McpTransportContextExtractor<WebSocketServerSession> contextExtractor;
    private final ScheduledFuture<?> keepAliveScheduler;
    private McpStreamableServerSession.Factory sessionFactory;
    private volatile boolean isClosing = false;


    public WebSocketStreamableServerTransportProvider(McpJsonMapper mcpJsonMapper, WebSocketStreamableContext webSocketStreamableContext, String mcpEndpoint,
                                                      McpTransportContextExtractor<WebSocketServerSession> contextExtractor,
                                                      Duration keepAliveInterval) {
        Assert.notNull(mcpJsonMapper, "McpJsonMapper must not be null");
        Assert.notNull(mcpEndpoint, "MCP endpoint must not be null");
        Assert.notNull(contextExtractor, "McpTransportContextExtractor must not be null");

        this.mcpJsonMapper = mcpJsonMapper;
        this.mcpEndpoint = mcpEndpoint;
        this.contextExtractor = contextExtractor;

        this.webSocketStreamableContext = webSocketStreamableContext;
        webSocketStreamableContext.setWebSocketStreamableServerTransportProvider(this);

        this.keepAliveScheduler = AsyncUtils.scheduleWithFixedDelay(this::keepAlive, 3, keepAliveInterval.getSeconds(), TimeUnit.SECONDS);
    }

    private void keepAlive() {
        WebSocketHandler webSocketHandler = this.webSocketStreamableContext.getWebSocketHandler();
        if (webSocketHandler != null) {
            webSocketHandler.keepAlive();
        }
    }

    @Override
    public List<String> protocolVersions() {
        return List.of(ProtocolVersions.MCP_2024_11_05, ProtocolVersions.MCP_2025_03_26,
                ProtocolVersions.MCP_2025_06_18);
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

    void handlePost(WebSocketServerSession webSocketServerSession, WebSocketMessage sentMessage) {
        if (this.isClosing) {
            webSocketServerSession.sendError(sentMessage.id(), ApiException.getApiException(HttpStatus.SERVICE_UNAVAILABLE, "Server is shutting down"));
        }

        McpTransportContext transportContext = this.contextExtractor.extract(webSocketServerSession);

        try {
            McpSchema.JSONRPCMessage message = sentMessage.deserializeJsonRpcMessage();

            // Handle initialization request
            if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest
                    && jsonrpcRequest.method().equals(McpSchema.METHOD_INITIALIZE)) {
                McpSchema.InitializeRequest initializeRequest = this.mcpJsonMapper.convertValue(jsonrpcRequest.params(),
                        new TypeRef<McpSchema.InitializeRequest>() {
                        });
                McpStreamableServerSession.McpStreamableServerSessionInit init = this.sessionFactory
                        .startSession(initializeRequest);
                this.sessions.put(init.session().getId(), init.session());
                this.webSocketAndSessionMap.put(webSocketServerSession.getId(), init.session().getId());
                try {
                    McpSchema.InitializeResult initResult = init.initResult().block();

                    webSocketServerSession.sendResponse(sentMessage.id(), init.session().getId(),
                            new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, jsonrpcRequest.id(), initResult,
                                    null));
                } catch (Exception e) {
                    log.error("Failed to initialize websocket session: {}", e.getMessage());
                    webSocketServerSession.sendError(sentMessage.id(), ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                }
                return;
            }

            // Handle other messages that require a session
            if (!StringUtils.hasText(sentMessage.mcpSessionId())) {
                webSocketServerSession.sendError(sentMessage.id(), ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Session ID missing"));
            }

            String sessionId = sentMessage.mcpSessionId();
            McpStreamableServerSession session = this.sessions.get(sessionId);

            if (session == null) {
                webSocketServerSession.sendError(sentMessage.id(), ApiException.getApiException(HttpStatus.NOT_FOUND, "Session not found: " + sessionId));
                return;
            }

            if (message instanceof McpSchema.JSONRPCResponse jsonrpcResponse) {
                session.accept(jsonrpcResponse)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                log.debug("received McpSchema.JSONRPCResponse:{}", jsonrpcResponse);
                webSocketServerSession.sendAccepted(sentMessage.id());
            } else if (message instanceof McpSchema.JSONRPCNotification jsonrpcNotification) {
                session.accept(jsonrpcNotification)
                        .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                        .block();
                log.debug("received McpSchema.JSONRPCNotification:{}", jsonrpcNotification);
                webSocketServerSession.sendAccepted(sentMessage.id());
            } else if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {

                WebSocketStreamableMcpSessionTransport sessionTransport = new WebSocketStreamableMcpSessionTransport(
                        webSocketServerSession, sentMessage.id(), sentMessage.mcpSessionId());

                try {
                    session.responseStream(jsonrpcRequest, sessionTransport)
                            .contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext))
                            .block();
                } catch (Exception e) {
                    log.error("Failed to handle websocket request stream: {}", e.getMessage());
                    webSocketServerSession.sendError(sentMessage.id(), ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
                }
            } else {
                webSocketServerSession.sendError(sentMessage.id(), ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown message type"));
            }
        } catch (IllegalArgumentException e) {
            log.error("Failed to deserialize websocket message: {}", e.getMessage());
            webSocketServerSession.sendError(sentMessage.id(), ApiException.getApiException(HttpStatus.BAD_REQUEST, "Invalid message format"));
        } catch (Exception e) {
            log.error("Error handling websocket message: {}", e.getMessage());
            webSocketServerSession.sendError(sentMessage.id(), ApiException.getApiException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    public void handleDelete(WebSocketServerSession webSocketServerSession, WebSocketMessage sendMessage) {

        if (this.isClosing) {
            webSocketServerSession.sendError(sendMessage.id(), ApiException.getApiException(HttpStatus.SERVICE_UNAVAILABLE, "Server is shutting down"));
        }

        McpTransportContext transportContext = this.contextExtractor.extract(webSocketServerSession);

        String sessionId = sendMessage.mcpSessionId();
        McpStreamableServerSession session = this.sessions.remove(sessionId);

        if (session == null) {
            return;
        }

        this.webSocketAndSessionMap.remove(webSocketServerSession.getId());
        try {
            session.delete().contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)).block();
            webSocketServerSession.sendAccepted(sendMessage.id());
        } catch (Exception e) {
            log.error("Failed to delete websocket session {}: {}", sessionId, e.getMessage());
        }
    }

    public void onCloseWhenKeepAlive(WebSocketServerSession webSocketServerSession) {
        String sessionId = this.webSocketAndSessionMap.remove(webSocketServerSession.getId());
        if (StringUtils.hasText(sessionId)) {
            log.error("onCloseWhenKeepAlive websocket session {}: {}", webSocketServerSession.getId(), sessionId);
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

        private final WebSocketServerSession webSocketServerSession;

        private final String requestId;

        private final String mcpSessionId;

        private final ReentrantLock lock = new ReentrantLock();

        private volatile boolean closed = false;

        WebSocketStreamableMcpSessionTransport(WebSocketServerSession webSocketServerSession, String requestId, String mcpSessionId) {
            this.webSocketServerSession = webSocketServerSession;
            this.requestId = requestId;
            this.mcpSessionId = mcpSessionId;
        }

        private void sendWebSocketMessage(McpSchema.JSONRPCMessage message) {
            if (message instanceof McpSchema.JSONRPCResponse response) {
                this.webSocketServerSession.sendResponse(this.requestId, response);
            } else {
                this.webSocketServerSession.sendRequestOrNotificationMessage(message);
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
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return mcpJsonMapper.convertValue(data, typeRef);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(WebSocketStreamableMcpSessionTransport.this::close);
        }

        @Override
        public void close() {
            this.lock.lock();
            try {
                if (this.closed) {
                    log.debug("Mcp websocket session transport {} already closed", this.mcpSessionId);
                    return;
                }

                this.closed = true;
                log.debug("Successfully completed for mcp websocket session {}", this.mcpSessionId);
            } catch (Exception e) {
                log.warn("Failed to complete for mcp websocket  session {}: {}", this.mcpSessionId, e.getMessage());
            } finally {
                this.lock.unlock();
            }
        }
    }

}
