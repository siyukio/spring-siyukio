package io.modelcontextprotocol.client.transport;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.util.Utils;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Bugee
 */


@Slf4j
public class WebSocketClientStreamableTransport implements McpClientTransport {

    private final McpJsonMapper jsonMapper;

    private final Map<String, String> headerMap;

    private final URI baseUri;

    private final String endpoint;

    private final AtomicReference<DefaultMcpTransportSession> activeSession = new AtomicReference<>();

    private final AtomicReference<Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>>> handler = new AtomicReference<>();

    private final AtomicReference<Consumer<Throwable>> exceptionHandler = new AtomicReference<>();

    private final WebSocketClient webSocketClient;

    public WebSocketClientStreamableTransport(McpJsonMapper jsonMapper, Map<String, String> headerMap, String baseUri, String endpoint) {
        this.jsonMapper = jsonMapper;
        this.headerMap = headerMap;
        this.baseUri = URI.create(baseUri);
        this.endpoint = endpoint;
        this.webSocketClient = new WebSocketClient();
        this.activeSession.set(createTransportSession());
    }

    private static String sessionIdOrPlaceholder(McpTransportSession<?> transportSession) {
        return transportSession.sessionId().orElse("[missing_session_id]");
    }

    @Override
    public List<String> protocolVersions() {
        return List.of(ProtocolVersions.MCP_2024_11_05, ProtocolVersions.MCP_2025_03_26,
                ProtocolVersions.MCP_2025_06_18);
    }

    @Override
    public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
        return Mono.deferContextual(ctx -> {
            this.handler.set(handler);
            URI uri = Utils.resolveUri(this.baseUri, this.endpoint);
            return this.webSocketClient.connect(this.headerMap, uri);
        });
    }

    private DefaultMcpTransportSession createTransportSession() {
        Function<String, Publisher<Void>> onClose = sessionId -> sessionId == null ? Mono.empty()
                : createDelete(sessionId);
        return new DefaultMcpTransportSession(onClose);
    }

    private Publisher<Void> createDelete(String sessionId) {
        return Mono.from(WebSocketMessageCustomizer.NOOP.customize("delete", sessionId, null, null))
                .doOnNext(webSocketMessage -> {
                    this.webSocketClient.sendAsync(webSocketMessage).whenComplete((response, throwable) -> {
                        this.webSocketClient.close();
                    });
                }).then();
    }

    @Override
    public void setExceptionHandler(Consumer<Throwable> handler) {
        log.debug("Exception handler registered");
        this.exceptionHandler.set(handler);
    }

    private void handleException(Throwable t) {
        log.debug("Handling exception for session {}", sessionIdOrPlaceholder(this.activeSession.get()), t);
        if (t instanceof McpTransportSessionNotFoundException) {
            McpTransportSession<?> invalidSession = this.activeSession.getAndSet(createTransportSession());
            log.warn("Server does not recognize session {}. Invalidating.", invalidSession.sessionId());
            invalidSession.close();
        }
        Consumer<Throwable> handler = this.exceptionHandler.get();
        if (handler != null) {
            handler.accept(t);
        }
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.defer(() -> {
            log.debug("Graceful close triggered");
            DefaultMcpTransportSession currentSession = this.activeSession.getAndSet(createTransportSession());
            if (currentSession != null) {
                return currentSession.closeGracefully();
            }
            return Mono.empty();
        });
    }

    private Mono<Disposable> reconnect() {

        return Mono.deferContextual(ctx -> {
            final AtomicReference<Disposable> disposableRef = new AtomicReference<>();
            final McpTransportSession<Disposable> transportSession = this.activeSession.get();

            Disposable connection = this.webSocketClient.receiveAsync()
                    .flatMap(responseMessage -> {
                        McpSchema.JSONRPCMessage jsonrpcMessage = responseMessage.deserializeJsonRpcMessage();
                        if (jsonrpcMessage instanceof McpSchema.JSONRPCRequest ||
                                jsonrpcMessage instanceof McpSchema.JSONRPCNotification
                        ) {
                            return Mono.just(jsonrpcMessage);
                        } else {
                            return Flux.<McpSchema.JSONRPCMessage>error(
                                    new IllegalArgumentException("Unknown received message type"));
                        }
                    })
                    .flatMap(jsonRpcMessage -> this.handler.get().apply(Mono.just(jsonRpcMessage)))
                    .onErrorMap(CompletionException.class, t -> t.getCause())
                    .onErrorComplete(t -> {
                        this.handleException(t);
                        return true;
                    })
                    .doFinally(s -> {
                        Disposable ref = disposableRef.getAndSet(null);
                        if (ref != null) {
                            transportSession.removeConnection(ref);
                        }
                    })
                    .subscribe();

            disposableRef.set(connection);
            transportSession.addConnection(connection);
            return Mono.just(connection);
        });

    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage sentMessage) {

        return Mono.create(deliveredSink -> {
            log.debug("Sending message {}", sentMessage);

            final AtomicReference<Disposable> disposableRef = new AtomicReference<>();
            final McpTransportSession<Disposable> transportSession = this.activeSession.get();

            Disposable connection = Mono.deferContextual(connectionCtx -> {

                        String mcpSessionId = null;
                        if (transportSession != null && transportSession.sessionId().isPresent()) {
                            mcpSessionId = transportSession.sessionId().get();
                        }

                        var transportContext = connectionCtx.getOrDefault(McpTransportContext.KEY, McpTransportContext.EMPTY);
                        return Mono.from(WebSocketMessageCustomizer.NOOP.customize(null, mcpSessionId, sentMessage, transportContext));
                    })
                    .flatMapMany(requestMessage -> Flux.<WebSocketMessage>create(responseSink -> {
                        Mono.fromFuture(this.webSocketClient
                                .sendAsync(requestMessage)
                                .whenComplete((response, throwable) -> {
                                    if (throwable != null) {
                                        responseSink.error(throwable);
                                    } else {
                                        responseSink.next(response);
                                    }
                                    responseSink.complete();
                                })).onErrorMap(CompletionException.class, t -> t.getCause()).onErrorComplete().subscribe();
                    }))
                    .flatMap(responseMessage -> {
                        if (transportSession.sessionId().isEmpty() && StringUtils.hasText(responseMessage.mcpSessionId())) {
                            transportSession.markInitialized(responseMessage.mcpSessionId());
                            // Once we have a session, we try to open an async stream for
                            // the server to send notifications and requests out-of-band.
                            this.reconnect().contextWrite(deliveredSink.contextView()).subscribe();
                        }

                        String sessionRepresentation = sessionIdOrPlaceholder(transportSession);
                        if (responseMessage.body() == null || responseMessage.body().isEmpty()) {
                            log.debug("No content type returned for websocket request in session {}", sessionRepresentation);
                            // No content type means no response body, so we can just
                            // return
                            // an empty stream
                            deliveredSink.success();
                            return Flux.empty();
                        } else {
                            McpSchema.JSONRPCMessage jsonrpcMessage = responseMessage.deserializeJsonRpcMessage();
                            if (jsonrpcMessage instanceof McpSchema.JSONRPCResponse jsonrpcResponse) {
                                deliveredSink.success();
                                if (jsonrpcResponse.error() != null) {
                                    return Flux.<McpSchema.JSONRPCMessage>error(new McpError(jsonrpcResponse.error()));
                                } else {
                                    return Mono.just(jsonrpcMessage);
                                }
                            } else {
                                return Flux.<McpSchema.JSONRPCMessage>error(
                                        new IllegalArgumentException("Unknown returned message type"));
                            }
                        }
                    })
                    .flatMap(jsonRpcMessage -> this.handler.get().apply(Mono.just(jsonRpcMessage)))
                    .onErrorMap(CompletionException.class, t -> t.getCause())
                    .onErrorComplete(t -> {
                        // handle the error first
                        this.handleException(t);
                        // inform the caller of sendMessage
                        deliveredSink.error(t);
                        return true;
                    })
                    .doFinally(s -> {
                        log.debug("SendMessage finally: {}", s);
                        Disposable ref = disposableRef.getAndSet(null);
                        if (ref != null) {
                            transportSession.removeConnection(ref);
                        }
                    })
                    .contextWrite(deliveredSink.contextView())
                    .subscribe();

            disposableRef.set(connection);
            transportSession.addConnection(connection);
        });
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
        return this.jsonMapper.convertValue(data, typeRef);
    }

}
