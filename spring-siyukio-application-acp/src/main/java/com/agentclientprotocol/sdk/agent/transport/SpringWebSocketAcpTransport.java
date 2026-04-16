/*
 * Copyright 2025-2025 the original author or authors.
 */

package com.agentclientprotocol.sdk.agent.transport;

import com.agentclientprotocol.sdk.agent.AcpAgent;
import com.agentclientprotocol.sdk.agent.PromptContext;
import com.agentclientprotocol.sdk.error.AcpProtocolException;
import com.agentclientprotocol.sdk.spec.AcpAgentTransport;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import com.agentclientprotocol.sdk.spec.AcpSchema.JSONRPCMessage;
import com.agentclientprotocol.sdk.util.Assert;
import io.github.siyukio.application.acp.AcpSessionHandler;
import io.github.siyukio.tools.acp.AcpSchemaExt;
import io.github.siyukio.tools.acp.AcpSessionContext;
import io.github.siyukio.tools.acp.Invoke;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.ApiHandler;
import io.github.siyukio.tools.api.ApiProfiles;
import io.github.siyukio.tools.api.definition.ApiDefinition;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.AsyncUtils;
import io.github.siyukio.tools.util.OpenApiUtils;
import io.github.siyukio.tools.util.XDataUtils;
import io.modelcontextprotocol.json.TypeRef;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class SpringWebSocketAcpTransport implements AcpAgentTransport {

    /**
     * Default path for ACP WebSocket endpoints
     */
    public static final String DEFAULT_ACP_PATH = "/acp";

    public static final String DEFAULT_ACP_INVOKE_TAG = "invoke";

    @Getter
    private final String path;
    private final Sinks.Many<AcpSchemaExt.TransportMessage> inboundSink;
    private final Sinks.Many<AcpSchemaExt.TransportMessage> outboundSink;
    private final Sinks.One<Void> terminationSink = Sinks.one();
    private final AtomicBoolean isClosing = new AtomicBoolean(false);
    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final TokenProvider tokenProvider;
    private final AcpSessionHandler acpSessionHandler;
    private final ConcurrentHashMap<String, AuthSession> authSessionMap = new ConcurrentHashMap<>();
    private ScheduledFuture<?> keepAliveScheduler;
    private Consumer<Throwable> exceptionHandler = t -> log.error("WebSocket Acp Transport error", t);

    /**
     * Creates a new WebSocketAcpTransport on the specified port with default path.
     *
     */
    public SpringWebSocketAcpTransport(TokenProvider tokenProvider, AcpSessionHandler acpSessionHandler) {
        this(DEFAULT_ACP_PATH, tokenProvider, acpSessionHandler);
    }

    /**
     * Creates a new WebSocketAcpTransport on the specified port and path.
     *
     * @param path The WebSocket endpoint path (e.g., "/acp")
     */
    public SpringWebSocketAcpTransport(String path, TokenProvider tokenProvider, AcpSessionHandler acpSessionHandler) {
        this.tokenProvider = tokenProvider;
        this.acpSessionHandler = acpSessionHandler;
        Assert.hasText(path, "Path must not be empty");
        this.path = path;

        this.inboundSink = Sinks.many().unicast().onBackpressureBuffer();
        this.outboundSink = Sinks.many().unicast().onBackpressureBuffer();
    }

    private void keepAlive() {
        if (this.authSessionMap.isEmpty()) {
            return;
        }
        Set<String> exceptionIdSet = new HashSet<>();
        int total = this.authSessionMap.size();
        for (AuthSession authSession : this.authSessionMap.values()) {
            try {
                authSession.sendPing();
            } catch (Exception ex) {
                exceptionIdSet.add(authSession.getId());
            }
        }
        exceptionIdSet.forEach(this.authSessionMap::remove);
        int active = this.authSessionMap.size();
        log.debug("WebSocket Acp keepAlive:{}/{}", active, total);
    }

    @Override
    public Mono<Void> start(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
        if (!isStarted.compareAndSet(false, true)) {
            return Mono.error(new IllegalStateException("Already started"));
        }

        // Start keepAlive
        log.info("Starting WebSocket Acp keepAlive");
        this.keepAliveScheduler = AsyncUtils.scheduleWithFixedDelay(this::keepAlive, 3, 10, TimeUnit.SECONDS);

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
                // Process each message on a separate virtual thread to avoid blocking
                .flatMap(message ->
                        Mono.just(message.jsonRpcMessage())
                                // 1. Use Mono.deferContextual to get the Context
                                .transform(handler)
                                // 2. Get the Context before doOnNext
                                .flatMap(response ->
                                        Mono.deferContextual(ctx -> {
                                            String transportId = ctx.get(AcpSchemaExt.TRANSPORT_ID);
                                            return Mono.just(Tuples.of(response, transportId));
                                        })
                                )
                                // 3. Use response + transportId
                                .doOnNext(tuple -> {
                                    JSONRPCMessage response = tuple.getT1();
                                    String transportId = tuple.getT2();
                                    AcpSchemaExt.TransportMessage responseMessage = new AcpSchemaExt.TransportMessage(transportId, response);
                                    Sinks.EmitResult emitResult = this.outboundSink.tryEmitNext(responseMessage);
                                    if (!emitResult.isSuccess()) {
                                        if (!isClosing.get()) {
                                            log.error("Failed to enqueue outbound message");
                                        }
                                    }
                                })
                                // 4. Restore the original data
                                .map(Tuple2::getT1)
                                // 5. Write transportId to Context
                                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, message.transportId()))
                                // Run on virtual thread scheduler for parallel processing
                                .subscribeOn(AsyncUtils.VIRTUAL_SCHEDULER)
                )
                .doOnTerminate(() -> this.outboundSink.tryEmitComplete())
                .subscribe();
    }

    private void sendMessage(AuthSession authSession, JSONRPCMessage message) {
        try {
            String jsonMessage = XDataUtils.MCP_JSON_MAPPER.writeValueAsString(message);
            log.debug("Sending Acp message: {}", jsonMessage);
            authSession.sendTextMessage(jsonMessage);
        } catch (IOException ignored) {
        }
    }

    private void startOutboundProcessing() {
        this.outboundSink.asFlux()
                .publishOn(AsyncUtils.VIRTUAL_SCHEDULER)
                .subscribe(message -> {
                    try {
                        if (message != null && !isClosing.get()) {
                            AuthSession authSession = authSessionMap.get(message.transportId());
                            if (authSession != null) {
                                sendMessage(authSession, message.jsonRpcMessage());
                            } else {
                                log.warn("AuthSession not found for outbound message: {}", message.jsonRpcMessage());
                            }
                        }
                    } catch (Exception e) {
                        if (!isClosing.get()) {
                            log.error("Error processing outbound message", e);
                            exceptionHandler.accept(e);
                        }
                    }
                });
    }

    @Override
    public Mono<Void> sendMessage(JSONRPCMessage message) {
        return Mono.deferContextual(ctx -> {
            String transportId = ctx.get(AcpSchemaExt.TRANSPORT_ID);
            AcpSchemaExt.TransportMessage transportMessage = new AcpSchemaExt.TransportMessage(transportId, message);
            if (outboundSink.tryEmitNext(transportMessage).isSuccess()) {
                return Mono.empty();
            } else {
                return Mono.error(new RuntimeException(
                        "Failed to enqueue outbound message"
                ));
            }
        });
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            log.debug("WebSocket Acp transport closing gracefully");
            isClosing.set(true);
            inboundSink.tryEmitComplete();
            outboundSink.tryEmitComplete();
            this.keepAliveScheduler.cancel(false);
        }).then(Mono.fromCallable(() -> {
            authSessionMap.values().forEach(AuthSession::close);
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
        public boolean doHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws HandshakeFailureException {
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
                log.error("WebSocket Acp authorization failed: {}", authorization);
                return false;
            }
            attributes.put(HttpHeaders.AUTHORIZATION, token);
            return this.handshakeHandler.doHandshake(request, response, wsHandler, attributes);
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            Object value = session.getAttributes().get(HttpHeaders.AUTHORIZATION);
            if (value instanceof Token token) {
                AuthSession authSession = new AuthSession(session, token);
                authSessionMap.put(authSession.getId(), authSession);
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
            authSessionMap.remove(session.getId());
            this.dataBufferMap.remove(session.getId());
            log.debug("WebSocket Acp closed: {}, {}", status.toString(), session.getId());
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

            AuthSession authSession = authSessionMap.get(session.getId());
            if (authSession == null) {
                log.error("Auth session is not found: {}", session.getId());
                return;
            }

            try {
                JSONRPCMessage jsonRpcMessage = AcpSchema.deserializeJsonRpcMessage(XDataUtils.MCP_JSON_MAPPER, message);
                log.debug("Received Acp message: {}", jsonRpcMessage);
                AcpSchemaExt.TransportMessage transportMessage = new AcpSchemaExt.TransportMessage(session.getId(), jsonRpcMessage);
                Sinks.EmitResult emitResult = inboundSink.tryEmitNext(transportMessage);
                if (!emitResult.isSuccess()) {
                    if (!isClosing.get()) {
                        log.error("Failed to enqueue inbound message: {}", emitResult);
                    }
                }
            } catch (Exception e) {
                if (!isClosing.get()) {
                    log.error("Error processing inbound message", e);
                    exceptionHandler.accept(e);
                }
            }
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

    public class AcpInitializeHandler implements AcpAgent.InitializeHandler {

        @Override
        public Mono<AcpSchema.InitializeResponse> handle(AcpSchema.InitializeRequest request) {
            return AcpSessionHandler.withContext(authSessionMap, authSession -> {
                AcpSchema.InitializeResponse response = acpSessionHandler.handleInit(authSession.getToken(), request);
                List<AcpSchema.AuthMethod> authMethods = response.authMethods();
                if (CollectionUtils.isEmpty(authMethods)) {
                    authMethods = new ArrayList<>();
                } else {
                    authMethods = new ArrayList<>(authMethods);
                }
                Token token = authSession.getToken();
                authMethods.add(new AcpSchema.AuthMethod(token.id(), AcpSchemaExt.DEFAULT_AUTH_METHOD_NAME, token.name()));
                response = new AcpSchema.InitializeResponse(
                        response.protocolVersion(),
                        response.agentCapabilities(),
                        authMethods);
                return Mono.just(response);
            });
        }
    }

    public class AcpNewSessionHandler implements AcpAgent.NewSessionHandler {

        @Override
        public Mono<AcpSchema.NewSessionResponse> handle(AcpSchema.NewSessionRequest request) {
            return AcpSessionHandler.withContext(authSessionMap, authSession -> {
                AcpSchema.NewSessionResponse response = acpSessionHandler.handleNewSession(authSession.getToken(), request);
                return Mono.just(response);
            });
        }
    }

    public class AcpLoadSessionHandler implements AcpAgent.LoadSessionHandler {

        @Override
        public Mono<AcpSchema.LoadSessionResponse> handle(AcpSchema.LoadSessionRequest request) {
            return AcpSessionHandler.withContext(authSessionMap, authSession -> {
                AcpSchema.LoadSessionResponse response = acpSessionHandler.handleLoadSession(authSession.getToken(), request);
                return Mono.just(response);
            });
        }
    }

    public class AcpCancelHandler implements AcpAgent.CancelHandler {

        @Override
        public Mono<Void> handle(AcpSchema.CancelNotification request) {
            return AcpSessionHandler.withContext(authSessionMap, authSession -> {
                try {
                    acpSessionHandler.handleCancel(authSession.getToken(), request);
                } catch (Exception e) {
                    log.error("Error handling cancel request", e);
                }
                return Mono.empty();
            });
        }
    }

    public class AcpSetSessionModeHandler implements AcpAgent.SetSessionModeHandler {

        @Override
        public Mono<AcpSchema.SetSessionModeResponse> handle(AcpSchema.SetSessionModeRequest request) {
            return AcpSessionHandler.withContext(authSessionMap, authSession -> {
                AcpSchema.SetSessionModeResponse response = acpSessionHandler.handleSetSessionMode(authSession.getToken(), request);
                return Mono.just(response);
            });
        }
    }

    public class AcpSetSessionModelHandler implements AcpAgent.SetSessionModelHandler {

        @Override
        public Mono<AcpSchema.SetSessionModelResponse> handle(AcpSchema.SetSessionModelRequest request) {
            return AcpSessionHandler.withContext(authSessionMap, authSession -> {
                AcpSchema.SetSessionModelResponse response = acpSessionHandler.handleSetSessionModel(authSession.getToken(), request);
                return Mono.just(response);
            });
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

        private Mono<AcpSchema.PromptResponse> listTools(AcpSessionContext acpSessionContext) {
            List<AcpSchemaExt.Tool> tools = new ArrayList<>();
            this.toolHandlerMap.forEach((key, value) -> {
                ApiDefinition apiDefinition = value.apiDefinition();
                String title = apiDefinition.summary();
                String description = apiDefinition.description();
                if (!StringUtils.hasText(description)) {
                    description = title;
                }

                JSONObject inputSchema = XDataUtils.copy(apiDefinition.requestBodyParameter().schema(), JSONObject.class);

                OpenApiUtils.simplifySchema(inputSchema);

                JSONObject outputSchema = XDataUtils.copy(apiDefinition.responseBodyParameter().schema(), JSONObject.class);

                AcpSchemaExt.Tool tool = new AcpSchemaExt.Tool(
                        key,
                        title,
                        description,
                        inputSchema,
                        outputSchema
                );
                tools.add(tool);
            });

            AcpSchemaExt.ListToolsResult listToolsResult = new AcpSchemaExt.ListToolsResult(tools);
            String result = XDataUtils.toJSONString(listToolsResult);
            return acpSessionContext.sendToolCallCompletedAsync(result)
                    .then(Mono.just(new AcpSchema.PromptResponse(AcpSchema.StopReason.END_TURN)));
        }

        private Mono<AcpSchema.PromptResponse> callTool(ApiHandler apiHandler, AcpSessionContext acpSessionContext) {
            Token token = acpSessionContext.getToken();
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
            Invoke invoke = acpSessionContext.getInvoke();
            try {
                requestJson = apiHandler.requestValidator().validate(invoke.params());
            } catch (ApiException ex) {
                return Mono.error(new AcpProtocolException(ex.getCode(), ex.getMessage()));
            }

            Object[] params = new Object[]{acpSessionContext, token};

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

            return acpSessionContext.sendToolCallCompletedAsync(result)
                    .then(Mono.just(new AcpSchema.PromptResponse(AcpSchema.StopReason.END_TURN)));
        }

        @Override
        public Mono<AcpSchema.PromptResponse> handle(AcpSchema.PromptRequest request, PromptContext promptContext) {
            return AcpSessionHandler.withContext(authSessionMap, authSession -> {
                Invoke invoke = null;
                for (AcpSchema.ContentBlock contentBlock : request.prompt()) {
                    if (contentBlock instanceof AcpSchema.TextContent textContent) {
                        invoke = parseInvoke(textContent.text());
                        if (invoke != null) {
                            break;
                        }
                    }
                }
                Token token = authSession.getToken();
                AcpSessionContext acpSessionContext = new AcpSessionContext(promptContext, invoke, token, authSession.getId());

                if (invoke == null) {
                    AcpSchema.PromptResponse response = acpSessionHandler.handlePrompt(authSession.getToken(), request, acpSessionContext);
                    return Mono.just(response);
                }

                if (invoke.tool().equals(AcpSchemaExt.LIST_TOOLS)) {
                    return this.listTools(acpSessionContext);
                } else {
                    ApiHandler apiHandler = toolHandlerMap.get(invoke.tool());
                    if (apiHandler == null) {
                        return Mono.error(new AcpProtocolException(HttpStatus.NOT_FOUND.value(), "Tool not found"));
                    }
                    return this.callTool(apiHandler, acpSessionContext);
                }
            });
        }
    }

}
