package io.modelcontextprotocol.server.transport;

import com.siyukio.tools.api.ApiException;
import com.siyukio.tools.api.constants.ApiConstants;
import com.siyukio.tools.api.token.Token;
import com.siyukio.tools.api.token.TokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Bugee
 */
@Slf4j
public class MyWebSocketHandler extends TextWebSocketHandler implements HandshakeHandler {

    private final HandshakeHandler handshakeHandler = new DefaultHandshakeHandler();

    private final ConcurrentHashMap<String, StringBuilder> dataBufferMap = new ConcurrentHashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    private final MyWebSocketServerTransportProvider myWebSocketServerTransportProvider;

    private final TokenProvider tokenProvider;

    public MyWebSocketHandler(MyWebSocketServerTransportProvider myWebSocketServerTransportProvider, TokenProvider tokenProvider) {
        this.myWebSocketServerTransportProvider = myWebSocketServerTransportProvider;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {
        String protocol = request.getHeaders().getFirst(ApiConstants.SEC_WEBSOCKET_PROTOCOL);
        if (StringUtils.hasText(protocol)) {
            response.getHeaders().add(ApiConstants.SEC_WEBSOCKET_PROTOCOL, protocol);
        }

        String authorization = request.getHeaders().getFirst(ApiConstants.AUTHORIZATION);

        if (!StringUtils.hasText(authorization)) {
            authorization = request.getURI().getQuery();
        }
        if (!StringUtils.hasText(authorization)) {
            if (StringUtils.hasText(protocol)) {
                if (!protocol.toLowerCase(Locale.ROOT).equals("mcp")) {
                    authorization = protocol;
                    request.getHeaders().replace(ApiConstants.SEC_WEBSOCKET_PROTOCOL, List.of(""));
                }
            }
        }

        if (!StringUtils.hasText(authorization)) {
            return false;
        }

        Token token = this.tokenProvider.verifyToken(authorization);
        if (token == null || token.refresh || token.expired) {
            throw new ApiException(HttpStatus.UNAUTHORIZED);
        }
        attributes.put(ApiConstants.AUTHORIZATION, token);
        return this.handshakeHandler.doHandshake(request, response, wsHandler, attributes);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Token token = (Token) session.getAttributes().get(ApiConstants.AUTHORIZATION);
        if (token == null) {
            session.close(CloseStatus.PROTOCOL_ERROR);
            return;
        }
        token.sid = session.getId();
        MyWebSocketSession myWebSocketSession = new MyWebSocketSession(session);
        this.myWebSocketServerTransportProvider.handleConnection(myWebSocketSession);
        myWebSocketSession.start();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // 处理连接异常
        log.error("mcp WebSocket error:{}, {}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 连接关闭后触发
        log.info("WebSocket closed: {}, {}", status.toString(), session.getId());
        this.dataBufferMap.remove(session.getId());
        this.myWebSocketServerTransportProvider.handleClose(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
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

        log.debug("handleTextMessage: {}", text.length());
        this.myWebSocketServerTransportProvider.handleMessage(session.getId(), text);
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        log.debug("handlePongMessage: {}, {}", session.getId(), message.getPayload().toString());
    }

    @Override
    public boolean supportsPartialMessages() {
        return true;
    }

}
