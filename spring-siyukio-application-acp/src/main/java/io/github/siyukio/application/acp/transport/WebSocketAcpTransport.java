/*
 * Copyright 2025-2025 the original author or authors.
 */

package io.github.siyukio.application.acp.transport;

import com.agentclientprotocol.sdk.agent.AcpAgent;
import com.agentclientprotocol.sdk.agent.PromptContext;
import com.agentclientprotocol.sdk.error.AcpProtocolException;
import com.agentclientprotocol.sdk.spec.AcpAgentTransport;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import com.agentclientprotocol.sdk.spec.AcpSchema.JSONRPCMessage;
import com.agentclientprotocol.sdk.util.Assert;
import io.github.siyukio.tools.acp.Invoke;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.ApiHandler;
import io.github.siyukio.tools.api.ApiProfiles;
import io.github.siyukio.tools.api.definition.ApiDefinition;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.AsyncUtils;
import io.github.siyukio.tools.util.XDataUtils;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class WebSocketAcpTransport implements AcpAgentTransport {

    /**
     * Default path for ACP WebSocket endpoints
     */
    public static final String DEFAULT_ACP_PATH = "/acp";

    public static final String DEFAULT_ACP_INVOKE_TAG = "invoke";

    @Getter
    private final String path;
    private final Sinks.Many<JSONRPCMessage> inboundSink;
    private final Sinks.Many<JSONRPCMessage> outboundSink;
    private final Sinks.One<Void> terminationSink = Sinks.one();
    private final AtomicBoolean isClosing = new AtomicBoolean(false);
    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final TokenProvider tokenProvider;
    private final ConcurrentHashMap<String, WebSocketAcpSession> webSocketAcpSessionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketAcpSession> requestAcpSessionMap = new ConcurrentHashMap<>();
    private Consumer<Throwable> exceptionHandler = t -> log.error("WebSocket Acp Transport error", t);

    /**
     * Creates a new WebSocketAcpTransport on the specified port with default path.
     *
     */
    public WebSocketAcpTransport(TokenProvider tokenProvider) {
        this(DEFAULT_ACP_PATH, tokenProvider);
    }

    /**
     * Creates a new WebSocketAcpTransport on the specified port and path.
     *
     * @param path The WebSocket endpoint path (e.g., "/acp")
     */
    public WebSocketAcpTransport(String path, TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
        Assert.hasText(path, "Path must not be empty");
        this.path = path;

        this.inboundSink = Sinks.many().unicast().onBackpressureBuffer();
        this.outboundSink = Sinks.many().unicast().onBackpressureBuffer();
    }

    @Override
    public Mono<Void> start(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
        if (!isStarted.compareAndSet(false, true)) {
            return Mono.error(new IllegalStateException("Already started"));
        }

        return Mono.fromCallable(() -> {
            log.info("Starting WebSocket agent server on port {} at path {}", ApiProfiles.PORT, path);

            // Set up inbound message handling
            handleIncomingMessages(handler);

            startOutboundProcessing();

            log.info("WebSocket agent server started on port {} at path {}", ApiProfiles.PORT, path);
            return null;
        }).then();
    }

    private void handleIncomingMessages(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
        this.inboundSink.asFlux()
                .flatMap(message -> Mono.just(message).transform(handler))
                .doOnNext(response -> {
                    if (response != null) {
                        this.outboundSink.tryEmitNext(response);
                    }
                })
                .doOnTerminate(() -> {
                    this.outboundSink.tryEmitComplete();
                })
                .subscribe();
    }

    private void startOutboundProcessing() {
        this.outboundSink.asFlux()
                .publishOn(AsyncUtils.VIRTUAL_SCHEDULER)
                .subscribe(message -> {
                    if (message != null && !isClosing.get()) {
                        WebSocketAcpSession webSocketAcpSession = null;
                        if (message instanceof AcpSchema.JSONRPCResponse jsonrpcResponse) {
                            webSocketAcpSession = requestAcpSessionMap.remove(jsonrpcResponse.id().toString());
                        } else if (message instanceof AcpSchema.JSONRPCNotification jsonrpcNotification) {
                            if (jsonrpcNotification.params() instanceof AcpSchema.SessionNotification params) {
                                String sessionId = params.sessionId();
                                webSocketAcpSession = webSocketAcpSessionMap.get(sessionId);
                            }
                        } else {
                            log.error("Unsupported message type: {}", message.getClass().getSimpleName());
                        }
                        if (webSocketAcpSession != null) {
                            try {
                                McpJsonMapper jsonMapper = XDataUtils.MCP_JSON_MAPPER;
                                String jsonMessage = jsonMapper.writeValueAsString(message);
                                log.debug("Sending WebSocket message: {}", jsonMessage);
                                webSocketAcpSession.sendTextMessage(jsonMessage);
                            } catch (Exception e) {
                                if (!isClosing.get()) {
                                    log.error("Error sending WebSocket message", e);
                                    exceptionHandler.accept(e);
                                }
                            }
                        }

                    }
                });
    }

    @Override
    public Mono<Void> sendMessage(JSONRPCMessage message) {
        if (outboundSink.tryEmitNext(message).isSuccess()) {
            return Mono.empty();
        } else {
            return Mono.error(new RuntimeException("Failed to enqueue message"));
        }
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            log.debug("WebSocket agent transport closing gracefully");
            isClosing.set(true);
            inboundSink.tryEmitComplete();
            outboundSink.tryEmitComplete();
        }).then(Mono.fromCallable(() -> {
            webSocketAcpSessionMap.values().forEach((session) -> {
                try {
                    session.close();
                } catch (IOException ignored) {
                }
            });
            return null;
        })).then();
    }

    @Override
    public void setExceptionHandler(Consumer<Throwable> handler) {
        this.exceptionHandler = handler;
    }

    @Override
    public Mono<Void> awaitTermination() {
        return terminationSink.asMono();
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
        return XDataUtils.MCP_JSON_MAPPER.convertValue(data, typeRef);
    }

    public class WebSocketAcpHandler extends TextWebSocketHandler implements HandshakeHandler {

        private final HandshakeHandler handshakeHandler = new DefaultHandshakeHandler();

        private final ConcurrentHashMap<String, StringBuilder> dataBufferMap = new ConcurrentHashMap<>();

        private final ReentrantLock lock = new ReentrantLock();

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
                    if (!protocol.equalsIgnoreCase("acp")) {
                        authorization = protocol;
                        request.getHeaders().replace(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL, List.of(""));
                    }
                }
            }

            if (!StringUtils.hasText(authorization)) {
                return false;
            }

            Token token = tokenProvider.verifyToken(authorization);
            if (token == null || token.refresh()) {
                throw new ApiException(HttpStatus.UNAUTHORIZED);
            }
            attributes.put(HttpHeaders.AUTHORIZATION, token);
            return this.handshakeHandler.doHandshake(request, response, wsHandler, attributes);
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            Object value = session.getAttributes().get(HttpHeaders.AUTHORIZATION);
            if (value instanceof Token token) {
                WebSocketAcpSession webSocketAcpSession = new WebSocketAcpSession(session, token);
                webSocketAcpSessionMap.put(webSocketAcpSession.getId(), webSocketAcpSession);
            } else {
                session.close(CloseStatus.PROTOCOL_ERROR);
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("WebSocket Acp error:{}, {}", session.getId(), exception.getMessage());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            webSocketAcpSessionMap.remove(session.getId());

            log.debug("WebSocket Acp closed: {}, {}", status.toString(), session.getId());
            this.dataBufferMap.remove(session.getId());
        }

        private String getMessage(WebSocketSession session, TextMessage textMessage) {
            String message = textMessage.getPayload();
            this.lock.lock();
            try {
                if (!textMessage.isLast()) {
                    StringBuilder dataBuilder = this.dataBufferMap.computeIfAbsent(session.getId(), k -> new StringBuilder());
                    dataBuilder.append(message);
                    log.debug("Received WebSocket Acp message part:{},{}", textMessage.isLast(), message.length());
                    return "";
                }

                StringBuilder dataBuilder = this.dataBufferMap.get(session.getId());
                if (dataBuilder != null) {
                    dataBuilder.append(message);
                    message = dataBuilder.toString();
                    dataBuilder.setLength(0);
                    this.dataBufferMap.remove(session.getId());
                }
            } finally {
                this.lock.unlock();
            }
            return message;
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
            String message = this.getMessage(session, textMessage);
            log.debug("Received WebSocket Acp message: {}, {}", session.getId(), message);

            WebSocketAcpSession webSocketAcpSession = webSocketAcpSessionMap.get(session.getId());
            if (webSocketAcpSession == null) {
                log.error("WebSocket Acp session is not found: {}", session.getId());
                return;
            }

            Mono.fromRunnable(() -> {
                try {
                    JSONRPCMessage jsonRpcMessage = AcpSchema.deserializeJsonRpcMessage(XDataUtils.MCP_JSON_MAPPER, message);
                    if (jsonRpcMessage instanceof AcpSchema.JSONRPCRequest jsonrpcRequest) {
                        // new session
                        if (jsonrpcRequest.method().equals(AcpSchema.METHOD_SESSION_NEW)) {
                            JSONObject requestJson = XDataUtils.parseObject(message);
                            JSONObject params = requestJson.optJSONObject("params");
                            if (params != null) {
                                JSONObject meta = params.optJSONObject("_meta");
                                if (meta == null) {
                                    meta = new JSONObject();
                                    params.put("_meta", meta);
                                }
                                meta.put("sessionId", session.getId());
                                log.debug("Received method:{}, auto set sessionId: {}", AcpSchema.METHOD_SESSION_NEW, session.getId());
                                jsonRpcMessage = XDataUtils.copy(requestJson, AcpSchema.JSONRPCRequest.class);
                            }
                        }

                        requestAcpSessionMap.put(jsonrpcRequest.id().toString(), webSocketAcpSession);
                    }
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
            }).subscribeOn(AsyncUtils.VIRTUAL_SCHEDULER).subscribe();
        }

        @Override
        protected void handlePongMessage(WebSocketSession session, PongMessage message) {
            log.debug("Received WebSocket Acp pong message: {}, {}", session.getId(), message.getPayload());
        }

        @Override
        public boolean supportsPartialMessages() {
            return true;
        }

    }

    public class AcpPromptHandler implements AcpAgent.PromptHandler {

        private final Map<String, ApiHandler> toolHandlerMap = new HashMap<>();

        public AcpPromptHandler(Map<String, ApiHandler> apiHandlerMap) {
            apiHandlerMap.forEach((k, v) -> {
                if (v.apiDefinition().acpAvailable()) {
                    if (k.startsWith("/")) {
                        k = k.substring(1);
                    }

                    k = k.replaceAll("/", ".");
                    toolHandlerMap.put(k, v);
                }
            });
        }

        private Invoke parseInvoke(String text) {
            String startTag = "<" + DEFAULT_ACP_INVOKE_TAG + ">";
            int startIndex = text.indexOf(startTag);
            if (startIndex != -1) {
                String endTag = "</" + DEFAULT_ACP_INVOKE_TAG + ">";
                int endIndex = text.indexOf(endTag);
                if (endIndex != -1) {
                    String content = text.substring(startIndex + startTag.length(), endIndex);
                    return XDataUtils.parse(content, Invoke.class);
                }
            }
            return null;
        }

        @Override
        public Mono<AcpSchema.PromptResponse> handle(AcpSchema.PromptRequest request, PromptContext context) {
            String sessionId = request.sessionId();
            WebSocketAcpSession webSocketAcpSession = webSocketAcpSessionMap.get(sessionId);
            if (webSocketAcpSession == null) {
                return Mono.error(new AcpProtocolException(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase()));
            }

            Invoke invoke = null;
            for (AcpSchema.ContentBlock contentBlock : request.prompt()) {
                if (contentBlock instanceof AcpSchema.TextContent textContent) {
                    invoke = parseInvoke(textContent.text());
                    if (invoke != null) {
                        break;
                    }
                }
            }

            if (invoke == null) {
                return Mono.error(new AcpProtocolException(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase()));
            }

            ApiHandler apiHandler = toolHandlerMap.get(invoke.tool());
            if (apiHandler == null) {
                return Mono.error(new AcpProtocolException(HttpStatus.NOT_FOUND.value(), "Tool not found"));
            }

            Token token = webSocketAcpSession.getToken();

            ApiDefinition apiDefinition = apiHandler.apiDefinition();
            if (apiDefinition.authorization()) {
                //validate authorization
                if (!apiDefinition.roles().isEmpty()) {
                    // validate role
                    Set<String> roleSet = new HashSet<>(apiDefinition.roles());
                    if (!CollectionUtils.isEmpty(token.roles())) {
                        roleSet.retainAll(token.roles());
                    }
                    if (roleSet.isEmpty()) {
                        return Mono.error(new AcpProtocolException(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase()));
                    }
                }
            }

            JSONObject requestJson;
            try {
                requestJson = apiHandler.requestValidator().validate(invoke.params());
            } catch (ApiException ex) {
                return Mono.error(new AcpProtocolException(ex.getCode(), ex.getMessage()));
            }

            Object[] params = new Object[]{context, token, invoke};

            Object resultValue;
            try {
                resultValue = apiHandler.apiInvoker().invoke(requestJson, params);
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                log.error("CallTool error:{}, {}", invoke.tool(), ex.getMessage());
                ApiException exception = ApiException.getUnknownApiException(ex);
                return Mono.error(new AcpProtocolException(exception.getCode(), exception.getMessage()));
            } catch (InvocationTargetException ex) {
                Throwable throwable = ex.getTargetException();
                log.error("CallTool error: {}, {}", invoke.tool(), throwable.getMessage());
                ApiException exception = ApiException.getUnknownApiException(throwable);
                return Mono.error(new AcpProtocolException(exception.getCode(), exception.getMessage()));
            }


            Class<?> returnType = apiDefinition.realReturnType();
            String result;
            if (returnType == void.class || returnType == Void.class) {
                result = "{}";
            } else {
                result = XDataUtils.toJSONString(resultValue);
            }

            AcpSchema.TextResourceContents textResourceContents = new AcpSchema.TextResourceContents(result, "", MimeTypeUtils.APPLICATION_JSON_VALUE);
            AcpSchema.Resource resource = new AcpSchema.Resource("resource", textResourceContents, null, null);
            context.sendUpdate(context.getSessionId(), new AcpSchema.ToolCallUpdateNotification(
                    "tool_call_update",
                    invoke.toolCallId(),
                    apiDefinition.description(),
                    AcpSchema.ToolKind.EXECUTE,
                    AcpSchema.ToolCallStatus.COMPLETED,
                    List.of(new AcpSchema.ToolCallContentBlock("content", resource)),
                    List.of(),
                    "",
                    "",
                    Map.of()));

            return Mono.just(new AcpSchema.PromptResponse(AcpSchema.StopReason.END_TURN));
        }
    }

    public class AcpNewSessionHandler implements AcpAgent.NewSessionHandler {

        @Override
        public Mono<AcpSchema.NewSessionResponse> handle(AcpSchema.NewSessionRequest request) {
            Object sessionIdValue = request.meta().get("sessionId");
            if (sessionIdValue == null) {
                return Mono.error(new AcpProtocolException(HttpStatus.NOT_EXTENDED.value(), "Session id is null"));
            }
            String sessionId = sessionIdValue.toString();
            AcpSchema.SessionModelState sessionModelState = new AcpSchema.SessionModelState("default", List.of());
            AcpSchema.SessionModeState sessionModeState = new AcpSchema.SessionModeState("default", List.of());
            return Mono.just(new AcpSchema.NewSessionResponse(sessionId, sessionModeState, sessionModelState));
        }
    }

}
