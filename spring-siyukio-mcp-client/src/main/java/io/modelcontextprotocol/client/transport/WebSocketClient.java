package io.modelcontextprotocol.client.transport;

import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Bugee
 */
@Slf4j
public class WebSocketClient {

    private final AtomicReference<WebSocket> webSocketRef = new AtomicReference<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, CompletableFuture<WebSocketMessage>> pendingMap = new ConcurrentHashMap<>();
    private final Sinks.Many<WebSocketMessage> incomingSink = Sinks.many().multicast().onBackpressureBuffer();

    public void close() {
        WebSocket webSocket = this.webSocketRef.get();
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "");
        }
        this.pendingMap.clear();
        this.incomingSink.tryEmitComplete();
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
        WebSocketMessage websocketMessage = XDataUtils.parse(text, WebSocketMessage.class);
        CompletableFuture<WebSocketMessage> future = this.pendingMap.remove(websocketMessage.id());
        if (future == null) {
            log.debug("tryEmitNext webSocketMessage: {}", text);
            Sinks.EmitResult result = this.incomingSink.tryEmitNext(websocketMessage);
            if (result != Sinks.EmitResult.OK) {
                log.error("tryEmitNext webSocketMessage error: {}", text);
            }
        } else {
            future.complete(websocketMessage);
        }
    }

    private boolean sendTextMessage(String text) {
        WebSocket webSocket = this.webSocketRef.get();
        if (webSocket == null) {
            return false;
        }

        lock.lock();
        try {
            webSocket.sendText(text, true).join();
            return true;
        } finally {
            lock.unlock();
        }
    }


    public CompletableFuture<WebSocketMessage> sendAsync(WebSocketMessage requestMessage) {
        return CompletableFuture.supplyAsync(() -> {
            CompletableFuture<WebSocketMessage> future = new CompletableFuture<>();
            this.pendingMap.put(requestMessage.id(), future);

            String text = XDataUtils.toJSONString(requestMessage);
            boolean ok = this.sendTextMessage(text);
            if (!ok) {
                this.pendingMap.remove(requestMessage.id());
                future.completeExceptionally(new RuntimeException("WebSocket send failed"));
            }
            return future.join();
        });
    }


    public Flux<WebSocketMessage> receiveAsync() {
        log.debug("start receiveAsync...");
        return this.incomingSink.asFlux().share().cache(0);
    }
}
