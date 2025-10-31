package io.modelcontextprotocol.client.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.util.Utils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Bugee
 */


public class WebSocketClientStreamableTransport implements McpClientTransport {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientStreamableTransport.class);

    private final Map<String, String> headerMap;

    private final URI baseUri;

    private final String endpoint;

    private final AtomicReference<DefaultMcpTransportSession> activeSession = new AtomicReference<>();

    private final AtomicReference<Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>>> handler = new AtomicReference<>();

    private final AtomicReference<Consumer<Throwable>> exceptionHandler = new AtomicReference<>();

    private final MyWebSocketClient myWebSocketClient;

    public WebSocketClientStreamableTransport(Map<String, String> headerMap, String baseUri, String endpoint) {
        this.headerMap = headerMap;
        this.baseUri = URI.create(baseUri);
        this.endpoint = endpoint;
        this.myWebSocketClient = new MyWebSocketClient();
        this.activeSession.set(createTransportSession());
    }

    private static String sessionIdOrPlaceholder(McpTransportSession<?> transportSession) {
        return transportSession.sessionId().orElse("[missing_session_id]");
    }

    @Override
    public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
        return Mono.deferContextual(ctx -> {
            this.handler.set(handler);
            URI uri = Utils.resolveUri(this.baseUri, this.endpoint);
            return this.myWebSocketClient.connect(this.headerMap, uri);
        });
    }

    private DefaultMcpTransportSession createTransportSession() {
        Function<String, Publisher<Void>> onClose = sessionId -> sessionId == null ? Mono.empty()
                : createDelete(sessionId);
        return new DefaultMcpTransportSession(onClose);
    }

    private Publisher<Void> createDelete(String sessionId) {
        this.myWebSocketClient.close();
        return Mono.empty();
    }

    @Override
    public void setExceptionHandler(Consumer<Throwable> handler) {
        logger.debug("Exception handler registered");
        this.exceptionHandler.set(handler);
    }

    private void handleException(Throwable t) {
        logger.debug("Handling exception for session {}", sessionIdOrPlaceholder(this.activeSession.get()), t);
        if (t instanceof McpTransportSessionNotFoundException) {
            McpTransportSession<?> invalidSession = this.activeSession.getAndSet(createTransportSession());
            logger.warn("Server does not recognize session {}. Invalidating.", invalidSession.sessionId());
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
            logger.debug("Graceful close triggered");
            DefaultMcpTransportSession currentSession = this.activeSession.getAndSet(createTransportSession());
            if (currentSession != null) {
                return currentSession.closeGracefully();
            }
            return Mono.empty();
        });
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage sentMessage) {

        if (sentMessage instanceof McpSchema.JSONRPCResponse jsonrpcResponse) {
            return this.myWebSocketClient.sendResponse(jsonrpcResponse);
        }

        return Mono.create(deliveredSink -> {
            logger.debug("Sending message {}", sentMessage);

            final AtomicReference<Disposable> disposableRef = new AtomicReference<>();
            final McpTransportSession<Disposable> transportSession = this.activeSession.get();

            Disposable connection = Mono.just(this.myWebSocketClient)
                    .flatMapMany(webSocketClient -> Flux.<McpSchema.JSONRPCMessage>create(responseSink -> {
                        Mono.fromFuture(webSocketClient
                                .sendAsync(sentMessage, responseSink)
                                .whenComplete((response, throwable) -> {
                                    if (throwable != null) {
                                        responseSink.error(throwable);
                                    }
                                })).onErrorMap(CompletionException.class, t -> t.getCause()).onErrorComplete().subscribe();
                    }))
                    .flatMap(jsonRpcMessage -> {
                        if (jsonRpcMessage instanceof McpSchema.JSONRPCResponse jsonrpcResponse) {
                            String responseId = String.valueOf(jsonrpcResponse.id());
                            if (responseId.equals("mcp-session-id")) {
                                transportSession.markInitialized(String.valueOf(jsonrpcResponse.result()));
                                deliveredSink.success();
                                return Flux.empty();
                            } else if (responseId.equals("mcp-error")) {
                                McpSchema.JSONRPCResponse.JSONRPCError error = jsonrpcResponse.error();
                                if (error != null) {
                                    deliveredSink.error(new McpError(error));
                                    return Flux.empty();
                                }
                            }
                        }
                        return this.handler.get().apply(Mono.just(jsonRpcMessage));
                    })
                    .onErrorMap(CompletionException.class, t -> t.getCause())
                    .onErrorComplete(t -> {
                        // handle the error first
                        this.handleException(t);
                        // inform the caller of sendMessage
                        deliveredSink.error(t);
                        return true;
                    })
                    .doFinally(s -> {
                        logger.debug("SendMessage finally: {}", s);
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
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
        return JsonUtils.getObjectMapper().convertValue(data, typeRef);
    }
}
