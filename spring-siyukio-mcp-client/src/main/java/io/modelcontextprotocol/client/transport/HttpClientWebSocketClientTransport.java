package io.modelcontextprotocol.client.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * @author Bugee
 */
@Slf4j
public class HttpClientWebSocketClientTransport implements McpClientTransport {

    /**
     * JSON object mapper for message serialization/deserialization
     */
    protected final ObjectMapper objectMapper;
    private final AtomicReference<MyWebSocket> messageEndpoint = new AtomicReference<>();
    /**
     * Latch for coordinating endpoint discovery
     */
    private final CountDownLatch closeLatch = new CountDownLatch(1);
    /**
     * Base URI for the MCP server
     */
    private final String baseUri;
    /**
     * HTTP client for sending messages to the server. Uses HTTP POST over the message
     * endpoint
     */
    private final Map<String, String> headerMap = new HashMap<>();

    /**
     * Holds the SSE connection future
     */
    private final AtomicReference<CompletableFuture<Void>> connectionFuture = new AtomicReference<>();

    public HttpClientWebSocketClientTransport(String baseUri, Map<String, String> headerMap, ObjectMapper objectMapper) {
        this.baseUri = baseUri;
        this.objectMapper = objectMapper;
        this.headerMap.putAll(headerMap);
    }

    @Override
    public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        connectionFuture.set(future);
        WebSocket.Builder builder = HttpClient.newHttpClient().newWebSocketBuilder();
        for (Map.Entry<String, String> entry : this.headerMap.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        builder.buildAsync(URI.create(this.baseUri + "/mcp/message/ws"), new WebSocket.Listener() {

                    private final StringBuilder dataBuffer = new StringBuilder();

                    private final ReentrantLock lock = new ReentrantLock();

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        WebSocket.Listener.super.onOpen(webSocket);
                        future.complete(null);
                        messageEndpoint.set(new MyWebSocket(webSocket));
                        closeLatch.countDown();
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        CompletionStage<?> completionStage = WebSocket.Listener.super.onText(webSocket, data, last);

                        String fullData = "";
                        this.lock.lock();
                        try {
                            this.dataBuffer.append(data);
                            if (last) {
                                fullData = this.dataBuffer.toString();
                                this.dataBuffer.setLength(0);
                            }
                        } finally {
                            this.lock.unlock();
                        }
                        if (StringUtils.hasText(fullData)) {
                            try {
                                McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, fullData);
                                handler.apply(Mono.just(message)).subscribe();
                            } catch (IOException e) {
                                log.error("Error processing fullData:{}", fullData);
                                log.error("Error processing websocket event", e);
                                future.completeExceptionally(e);
                            }
                        }
                        return completionStage;
                    }

                    public CompletionStage<?> onPing(WebSocket webSocket,
                                                     ByteBuffer message) {
                        log.debug("websocket client onPing:{}", message);
                        return WebSocket.Listener.super.onPing(webSocket, message);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("websocket client connection error", error);
                        future.completeExceptionally(error);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        if (StringUtils.hasText(reason)) {
                            log.error("websocket client connection onClose: {}, {}", statusCode, reason);
                        } else {
                            log.debug("websocket client connection onClose: {}", statusCode);
                        }
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }
                })
                .join();

        return Mono.fromFuture(future);
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            CompletableFuture<Void> future = connectionFuture.get();
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
            MyWebSocket endpoint = messageEndpoint.get();
            if (endpoint != null) {
                endpoint.sendClose(WebSocket.NORMAL_CLOSURE, "");
            }
        });
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {

        try {
            if (!closeLatch.await(10, TimeUnit.SECONDS)) {
                return Mono.error(new McpError("Failed to wait for the message endpoint"));
            }
        } catch (InterruptedException e) {
            return Mono.error(new McpError("Failed to wait for the message endpoint"));
        }

        MyWebSocket endpoint = messageEndpoint.get();
        if (endpoint == null) {
            return Mono.error(new McpError("No message endpoint available"));
        }

        try {
            String jsonText = this.objectMapper.writeValueAsString(message);
            endpoint.sendText(jsonText);
            return Mono.empty();
        } catch (IOException e) {
            log.error("", e);
            return Mono.empty();
        }
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
        return this.objectMapper.convertValue(data, typeRef);
    }
}
