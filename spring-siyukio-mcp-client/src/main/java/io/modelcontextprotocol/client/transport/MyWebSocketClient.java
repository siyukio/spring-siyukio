package io.modelcontextprotocol.client.transport;

import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpTransportException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Bugee
 */
@Slf4j
public class MyWebSocketClient {

    private final AtomicReference<WebSocket> webSocketRef = new AtomicReference<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Sinks.Many<McpSchema.JSONRPCMessage> incomingSink = Sinks.many().multicast().onBackpressureBuffer();

    public void close() {
        WebSocket webSocket = this.webSocketRef.get();
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "");
        }
    }

    public Mono<Void> connect(Map<String, String> headerMap, URI uri) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        HttpClient.Builder clientBuilder = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1);
        HttpClient httpClient = clientBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        WebSocket.Builder builder = httpClient.newWebSocketBuilder();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        builder.buildAsync(uri, new WebSocket.Listener() {

                    private final StringBuilder dataBuffer = new StringBuilder();

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        WebSocket.Listener.super.onOpen(webSocket);
                        webSocketRef.set(webSocket);
                        future.complete(null);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        CompletionStage<?> completionStage = WebSocket.Listener.super.onText(webSocket, data, last);

                        String fullData = "";
                        lock.lock();
                        try {
                            this.dataBuffer.append(data);
                            if (last) {
                                fullData = this.dataBuffer.toString();
                                this.dataBuffer.setLength(0);
                            }
                        } finally {
                            lock.unlock();
                        }
                        if (StringUtils.hasText(fullData)) {
                            handleText(fullData);
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
                        future.complete(null);
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }
                })
                .join();
        return Mono.fromFuture(future);
    }

    private void handleText(String text) {
        log.debug("handleText: {}", text);
        try {
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(JsonUtils.getObjectMapper(), text);
            this.incomingSink.tryEmitNext(message);
        } catch (Exception e) {
            log.error("websocket handleText error ", e);
        }
    }

    public CompletableFuture<Boolean> sendAsync(McpSchema.JSONRPCMessage sendMessage, FluxSink<McpSchema.JSONRPCMessage> sink) {
        return CompletableFuture.supplyAsync(() -> {
            this.sendRequest(sendMessage)
                    .onErrorResume(error -> {
                        sink.error(error);
                        return Flux.empty();
                    })
                    .subscribe(receiveMessage -> {
                        if (receiveMessage instanceof McpSchema.JSONRPCResponse jsonrpcResponse) {
                            String id = String.valueOf(jsonrpcResponse.id());
                            if (id.equals("mcp-error")) {
                                sink.next(receiveMessage);
                                sink.error(new McpError(jsonrpcResponse.error()));
                            } else {
                                sink.next(receiveMessage);
                                sink.complete();
                            }
                        } else {
                            sink.next(receiveMessage);
                        }
                    });
            return true;
        });
    }

    public Mono<Void> sendResponse(McpSchema.JSONRPCResponse message) {
        WebSocket webSocket = this.webSocketRef.get();

        if (webSocket == null) {
            return Mono.error(new McpTransportException("websocket is null"));
        }

        return Mono.defer(() -> {
            lock.lock();
            try {
                String json = JsonUtils.toJSONString(message);
                log.debug("sendResponseText: {}", json);
                webSocket.sendText(json, true).join();
                return Mono.empty();
            } catch (Exception e) {
                return Mono.error(e);
            } finally {
                lock.unlock();
            }
        });
    }

    public Flux<McpSchema.JSONRPCMessage> sendRequest(McpSchema.JSONRPCMessage sendMessage) {

        WebSocket webSocket = this.webSocketRef.get();

        if (webSocket == null) {
            return Flux.error(new McpTransportException("websocket is null"));
        }

        Mono<Void> sendMono = Mono.defer(() -> {
            lock.lock();
            try {
                String json = JsonUtils.toJSONString(sendMessage);
                log.debug("sendRequestText: {}", json);
                webSocket.sendText(json, true).join();
                return Mono.empty();
            } catch (Exception e) {
                return Mono.error(e);
            } finally {
                lock.unlock();
            }
        });

        Flux<McpSchema.JSONRPCMessage> responseFlux = this.incomingSink.asFlux().share().cache(0);
        return sendMono.thenMany(responseFlux);
    }
}
