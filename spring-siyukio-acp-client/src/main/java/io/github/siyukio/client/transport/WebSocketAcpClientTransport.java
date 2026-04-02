package io.github.siyukio.client.transport;

import com.agentclientprotocol.sdk.spec.AcpClientTransport;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import com.agentclientprotocol.sdk.util.Assert;
import io.github.siyukio.tools.acp.Invoke;
import io.github.siyukio.tools.util.AsyncUtils;
import io.github.siyukio.tools.util.HttpClientUtils;
import io.github.siyukio.tools.util.XDataUtils;
import io.modelcontextprotocol.json.TypeRef;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 * @author Bugee
 */
@Slf4j
public class WebSocketAcpClientTransport implements AcpClientTransport {

    /**
     * Default path for ACP WebSocket endpoints
     */
    public static final String DEFAULT_ACP_PATH = "/acp";

    public static final String DEFAULT_ACP_INVOKE_TAG = "invoke";

    private final URI serverUri;

    private final Map<String, String> headerMap;

    private final Sinks.Many<AcpSchema.JSONRPCMessage> inboundSink;

    private final Sinks.Many<AcpSchema.JSONRPCMessage> outboundSink;

    private final Sinks.One<Void> connectionReady = Sinks.one();
    private final AtomicBoolean isClosing = new AtomicBoolean(false);
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final ReentrantLock lock = new ReentrantLock();
    private WebSocket webSocket;
    private Consumer<Throwable> exceptionHandler = t -> log.error("Transport error", t);
    private Duration connectTimeout = Duration.ofSeconds(30);

    /**
     * Creates a new WebSocketAcpClientTransport with custom HttpClient.
     *
     * @param serverUri The WebSocket URI to connect to
     */
    public WebSocketAcpClientTransport(String serverUri, Map<String, String> headerMap) {
        this.headerMap = headerMap;
        Assert.notNull(serverUri, "The serverUri can not be null");

        if (!serverUri.endsWith(DEFAULT_ACP_PATH)) {
            serverUri = serverUri + DEFAULT_ACP_PATH;
        }

        this.serverUri = URI.create(serverUri);

        this.inboundSink = Sinks.many().unicast().onBackpressureBuffer();
        this.outboundSink = Sinks.many().unicast().onBackpressureBuffer();
    }

    public static String createAcpToolCall(Invoke invoke) {
        return "<" + DEFAULT_ACP_INVOKE_TAG + ">"
                + XDataUtils.toJSONString(invoke)
                + "</" + DEFAULT_ACP_INVOKE_TAG + ">";
    }

    /**
     * Sets the connection timeout for WebSocket establishment.
     *
     * @param timeout The connection timeout
     * @return This transport for chaining
     */
    public WebSocketAcpClientTransport connectTimeout(Duration timeout) {
        this.connectTimeout = timeout;
        return this;
    }

    @Override
    public Mono<Void> connect(Function<Mono<AcpSchema.JSONRPCMessage>, Mono<AcpSchema.JSONRPCMessage>> handler) {
        if (!isConnected.compareAndSet(false, true)) {
            return Mono.error(new IllegalStateException("Already connected"));
        }

        return Mono.fromFuture(() -> {
            log.info("Connecting to WebSocket server at {}", serverUri);

            // Set up inbound message handling
            handleIncomingMessages(handler);

            // Build WebSocket connection with listener
            HttpClient httpClient = HttpClientUtils.getHttpClient();
            WebSocket.Builder builder = httpClient.newWebSocketBuilder();
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }

            return builder.connectTimeout(connectTimeout)
                    .buildAsync(serverUri, new AcpWebSocketListener());
        }).doOnSuccess(ws -> {
            this.webSocket = ws;
            startOutboundProcessing();
            connectionReady.tryEmitValue(null);
            log.debug("Connected to WebSocket server at {}", serverUri);
        }).doOnError(e -> {
            log.error("Failed to connect to WebSocket server at {}", serverUri, e);
            isConnected.set(false);
            exceptionHandler.accept(e);
        }).doOnCancel(() -> {
            log.debug("WebSocket connection cancelled");
            isConnected.set(false);
        }).then();
    }

    private void handleIncomingMessages(Function<Mono<AcpSchema.JSONRPCMessage>, Mono<AcpSchema.JSONRPCMessage>> handler) {
        this.inboundSink.asFlux()
                .flatMap(message -> Mono.just(message).transform(handler))
                .doOnTerminate(() -> {
                    this.outboundSink.tryEmitComplete();
                })
                .subscribe();
    }

    private void startOutboundProcessing() {
        this.outboundSink.asFlux()
                .publishOn(AsyncUtils.VIRTUAL_SCHEDULER)
                .subscribe(message -> {
                    if (message != null && !isClosing.get() && webSocket != null) {
                        try {
                            String jsonMessage = XDataUtils.MCP_JSON_MAPPER.writeValueAsString(message);
                            log.debug("Sending WebSocket message: {}", jsonMessage);
                            webSocket.sendText(jsonMessage, true).join();
                        } catch (Exception e) {
                            if (!isClosing.get()) {
                                log.error("Error sending WebSocket message", e);
                                exceptionHandler.accept(e);
                            }
                        }
                    }
                });
    }

    @Override
    public Mono<Void> sendMessage(AcpSchema.JSONRPCMessage message) {
        return connectionReady.asMono().then(Mono.defer(() -> {
            if (outboundSink.tryEmitNext(message).isSuccess()) {
                return Mono.empty();
            } else {
                return Mono.error(new RuntimeException("Failed to enqueue message"));
            }
        }));
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            log.debug("WebSocket transport closing gracefully");
            isClosing.set(true);
            inboundSink.tryEmitComplete();
            outboundSink.tryEmitComplete();
        }).then(Mono.defer(() -> {
            if (webSocket != null) {
                return Mono.fromFuture(webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client closing")
                        .thenApply(ws -> null));
            }
            return Mono.empty();
        })).then();
    }

    @Override
    public void setExceptionHandler(Consumer<Throwable> handler) {
        this.exceptionHandler = handler;
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
        return XDataUtils.MCP_JSON_MAPPER.convertValue(data, typeRef);
    }

    /**
     * WebSocket.Listener implementation for handling incoming messages.
     */
    private class AcpWebSocketListener implements WebSocket.Listener {

        private final StringBuilder messageBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            log.debug("WebSocket connection opened");
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String fullData = "";
            lock.lock();
            try {
                messageBuffer.append(data);
                if (last) {
                    fullData = messageBuffer.toString();
                    messageBuffer.setLength(0);
                }
            } finally {
                lock.unlock();
            }

            log.debug("Received WebSocket message: {}", fullData);

            try {
                AcpSchema.JSONRPCMessage jsonRpcMessage = AcpSchema.deserializeJsonRpcMessage(XDataUtils.MCP_JSON_MAPPER, fullData);
                if (!inboundSink.tryEmitNext(jsonRpcMessage).isSuccess()) {
                    if (!isClosing.get()) {
                        log.error("Failed to enqueue inbound message");
                    }
                }
            } catch (Exception e) {
                if (!isClosing.get()) {
                    log.error("Error processing inbound message", e);
                    exceptionHandler.accept(e);
                }
            }

            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.info("WebSocket connection closed: {} - {}", statusCode, reason);
            isClosing.set(true);
            inboundSink.tryEmitComplete();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (!isClosing.get()) {
                log.error("WebSocket error", error);
                exceptionHandler.accept(error);
            }
            isClosing.set(true);
            inboundSink.tryEmitComplete();
        }

    }
}
