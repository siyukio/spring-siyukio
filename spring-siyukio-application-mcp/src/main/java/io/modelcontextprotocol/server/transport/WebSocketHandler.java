package io.modelcontextprotocol.server.transport;

import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Bugee
 */
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler implements HandshakeHandler {

    private final HandshakeHandler handshakeHandler = new DefaultHandshakeHandler();

    private final ConcurrentHashMap<String, StringBuilder> dataBufferMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, WebSocketServerSession> myWebSocketSessionMap = new ConcurrentHashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    private final WebSocketStreamableContext webSocketStreamableContext;

    private final TokenProvider tokenProvider;

    public WebSocketHandler(WebSocketStreamableContext webSocketStreamableContext, TokenProvider tokenProvider) {
        this.webSocketStreamableContext = webSocketStreamableContext;
        this.tokenProvider = tokenProvider;
        this.webSocketStreamableContext.setWebSocketHandler(this);
    }

    @Override
    public boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response, org.springframework.web.socket.WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {
        String protocol = request.getHeaders().getFirst(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL);
        if (StringUtils.hasText(protocol)) {
            response.getHeaders().add(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL, protocol);
        }

        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(authorization)) {
            authorization = request.getURI().getQuery();
        }
        if (!StringUtils.hasText(authorization)) {
            if (StringUtils.hasText(protocol)) {
                if (!protocol.equalsIgnoreCase("mcp")) {
                    authorization = protocol;
                    request.getHeaders().replace(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL, List.of(""));
                }
            }
        }

        if (!StringUtils.hasText(authorization)) {
            return false;
        }

        Token token = this.tokenProvider.verifyToken(authorization);
        if (token == null || token.refresh() || token.expired()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED);
        }
        attributes.put(HttpHeaders.AUTHORIZATION, token);
        return this.handshakeHandler.doHandshake(request, response, wsHandler, attributes);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object value = session.getAttributes().get(HttpHeaders.AUTHORIZATION);
        if (value instanceof Token token) {
            WebSocketServerSession webSocketServerSession = new WebSocketServerSession(session, token);
            this.myWebSocketSessionMap.put(webSocketServerSession.getId(), webSocketServerSession);
        } else {
            session.close(CloseStatus.PROTOCOL_ERROR);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("mcp WebSocket error:{}, {}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        this.myWebSocketSessionMap.remove(session.getId());

        log.debug("WebSocket closed: {}, {}", status.toString(), session.getId());
        this.dataBufferMap.remove(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        String text = textMessage.getPayload();
        this.lock.lock();
        try {
            if (!textMessage.isLast()) {
                StringBuilder dataBuilder = this.dataBufferMap.computeIfAbsent(session.getId(), k -> new StringBuilder());
                dataBuilder.append(text);
                log.debug("handleTextMessage part:{},{}", textMessage.isLast(), text.length());
                return;
            }

            StringBuilder dataBuilder = this.dataBufferMap.get(session.getId());
            if (dataBuilder != null) {
                dataBuilder.append(text);
                text = dataBuilder.toString();
                dataBuilder.setLength(0);
                this.dataBufferMap.remove(session.getId());
            }
        } finally {
            this.lock.unlock();
        }

        WebSocketServerSession webSocketServerSession = this.myWebSocketSessionMap.get(session.getId());
        if (webSocketServerSession == null) {
            log.error("MyWebSocketServerSession is not found: {}", session.getId());
            return;
        }

        log.debug("handleTextMessage: {}", text);
        WebSocketMessage websocketMessage = XDataUtils.parse(text, WebSocketMessage.class);
        Mono.fromRunnable(() -> {
            WebSocketStreamableServerTransportProvider webSocketStreamableServerTransportProvider = this.webSocketStreamableContext.getWebSocketStreamableServerTransportProvider();
            if (webSocketStreamableServerTransportProvider != null) {
                if (websocketMessage.toDelete()) {
                    webSocketStreamableServerTransportProvider.handleDelete(webSocketServerSession, websocketMessage);
                } else {
                    webSocketStreamableServerTransportProvider.handlePost(webSocketServerSession, websocketMessage);
                }
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        log.debug("handlePongMessage: {}, {}", session.getId(), message.getPayload());
    }

    @Override
    public boolean supportsPartialMessages() {
        return true;
    }

    public void keepAlive() {
        if (this.myWebSocketSessionMap.isEmpty()) {
            return;
        }
        log.debug("websocket keepAliveScheduler execute...");
        Set<String> exceptionIdSet = new HashSet<>();
        for (WebSocketServerSession webSocketSession : this.myWebSocketSessionMap.values()) {
            try {
                webSocketSession.sendPing();
            } catch (Exception ex) {
                exceptionIdSet.add(webSocketSession.getId());
            }
        }
        WebSocketServerSession errorWebSocketServerSession;
        WebSocketStreamableServerTransportProvider webSocketStreamableServerTransportProvider = this.webSocketStreamableContext.getWebSocketStreamableServerTransportProvider();
        for (String exceptionId : exceptionIdSet) {
            errorWebSocketServerSession = this.myWebSocketSessionMap.remove(exceptionId);
            try {
                errorWebSocketServerSession.close();
                webSocketStreamableServerTransportProvider.onCloseWhenKeepAlive(errorWebSocketServerSession);
            } catch (IOException ignored) {
            }
        }
    }

}
