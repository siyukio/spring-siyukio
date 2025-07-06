package io.modelcontextprotocol.spec;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.siyukio.tools.api.token.Token;
import io.modelcontextprotocol.server.McpAsyncServerExchange;
import io.modelcontextprotocol.server.MyMcpAsyncServerExchange;
import io.modelcontextprotocol.server.MyMcpSyncServerExchange;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a Model Control Protocol (MCP) session on the server side. It manages
 * bidirectional JSON-RPC communication with the client.
 */
@Slf4j
public class MyMcpServerSession extends McpServerSession implements McpSession {

    private static final int STATE_UNINITIALIZED = 0;
    private static final int STATE_INITIALIZING = 1;
    private static final int STATE_INITIALIZED = 2;
    private final ConcurrentHashMap<Object, MonoSink<McpSchema.JSONRPCResponse>> pendingResponses = new ConcurrentHashMap<>();

    /**
     * Duration to wait for request responses before timing out
     */
    private final Duration requestTimeout;

    private final AtomicLong requestCounter = new AtomicLong(0);
    private final InitRequestHandler initRequestHandler;
    private final InitNotificationHandler initNotificationHandler;
    private final Map<String, RequestHandler<?>> requestHandlers;
    private final Map<String, NotificationHandler> notificationHandlers;
    private final McpServerTransport transport;
    private final Sinks.One<McpAsyncServerExchange> exchangeSink = Sinks.one();
    private final AtomicReference<McpSchema.ClientCapabilities> clientCapabilities = new AtomicReference<>();
    private final AtomicReference<McpSchema.Implementation> clientInfo = new AtomicReference<>();
    private final AtomicInteger state = new AtomicInteger(STATE_UNINITIALIZED);

    @Getter
    @Setter
    private Token token;

    @Getter
    private long lastActiveTime = System.currentTimeMillis();

    /**
     * Creates a new server session with the given parameters and the transport to use.
     *
     * @param transport               the transport to use
     * @param initHandler             called when a
     *                                {@link McpSchema.InitializeRequest} is received by the
     *                                server
     * @param initNotificationHandler called when a
     *                                {@link McpSchema.METHOD_NOTIFICATION_INITIALIZED} is received.
     * @param requestHandlers         map of request handlers to use
     * @param notificationHandlers    map of notification handlers to use
     */
    public MyMcpServerSession(String id, Duration requestTimeout, McpServerTransport transport, InitRequestHandler initHandler,
                              InitNotificationHandler initNotificationHandler, Map<String, RequestHandler<?>> requestHandlers,
                              Map<String, NotificationHandler> notificationHandlers) {
        super(id, requestTimeout, transport, initHandler, initNotificationHandler, requestHandlers, notificationHandlers);
        this.transport = transport;
        this.initRequestHandler = initHandler;
        this.initNotificationHandler = initNotificationHandler;
        this.requestHandlers = requestHandlers;
        this.notificationHandlers = notificationHandlers;
        this.requestTimeout = requestTimeout;
    }

    /**
     * Called upon successful initialization sequence between the client and the server
     * with the client capabilities and information.
     * <p>
     * <a href=
     * "https://github.com/modelcontextprotocol/specification/blob/main/docs/specification/basic/lifecycle.md#initialization">Initialization
     * Spec</a>
     *
     * @param clientCapabilities the capabilities the connected client provides
     * @param clientInfo         the information about the connected client
     */
    public void init(McpSchema.ClientCapabilities clientCapabilities, McpSchema.Implementation clientInfo) {
        this.clientCapabilities.lazySet(clientCapabilities);
        this.clientInfo.lazySet(clientInfo);
    }

    private String generateRequestId() {
        return this.getId() + "-" + this.requestCounter.getAndIncrement();
    }

    @Override
    public <T> Mono<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
        log.debug("sendRequest:{},{},{},{}", this.token.sid, this.token.uid, method, requestParams);
        this.lastActiveTime = System.currentTimeMillis();
        boolean reply = true;
        if (requestParams instanceof McpSchema.CreateMessageRequest createMessageRequest) {
            Map<String, Object> metadata = createMessageRequest.metadata();
            if (metadata != null) {
                Object value = metadata.getOrDefault(MyMcpSyncServerExchange.REPLY_NAME, true);
                reply = (boolean) value;
                metadata.remove(MyMcpSyncServerExchange.REPLY_NAME);
            }
        }

        if (!reply) {
            McpSchema.JSONRPCRequest jsonrpcRequest = new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION, method,
                    "", requestParams);
            this.transport.sendMessage(jsonrpcRequest).subscribe(v -> {
            }, error -> {
            });
            return Mono.empty();
        }

        String requestId = this.generateRequestId();

        return Mono.<McpSchema.JSONRPCResponse>create(sink -> {
            this.pendingResponses.put(requestId, sink);
            McpSchema.JSONRPCRequest jsonrpcRequest = new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION, method,
                    requestId, requestParams);
            this.transport.sendMessage(jsonrpcRequest).subscribe(v -> {
            }, error -> {
                this.pendingResponses.remove(requestId);
                sink.error(error);
            });
        }).timeout(this.requestTimeout).handle((jsonRpcResponse, sink) -> {
            if (jsonRpcResponse.error() != null) {
                sink.error(new McpError(jsonRpcResponse.error()));
            } else {
                if (typeRef.getType().equals(Void.class)) {
                    sink.complete();
                } else {
                    sink.next(this.transport.unmarshalFrom(jsonRpcResponse.result(), typeRef));
                }
            }
        });
    }

    @Override
    public Mono<Void> sendNotification(String method, Object params) {
        log.debug("sendNotification:{},{},{},{}", this.token.sid, this.token.uid, method, params);
        this.lastActiveTime = System.currentTimeMillis();
        McpSchema.JSONRPCNotification jsonrpcNotification = new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION,
                method, params);
        return this.transport.sendMessage(jsonrpcNotification);
    }

    /**
     * Called by the {@link McpServerTransportProvider} once the session is determined.
     * The purpose of this method is to dispatch the message to an appropriate handler as
     * specified by the MCP server implementation
     * ({@link io.modelcontextprotocol.server.McpAsyncServer} or
     * {@link io.modelcontextprotocol.server.McpSyncServer}) via
     * {@link Factory} that the server creates.
     *
     * @param message the incoming JSON-RPC message
     * @return a Mono that completes when the message is processed
     */
    public Mono<Void> handle(McpSchema.JSONRPCMessage message) {
        this.lastActiveTime = System.currentTimeMillis();
        return Mono.defer(() -> {
            // TODO handle errors for communication to without initialization happening
            // first
            if (message instanceof McpSchema.JSONRPCResponse response) {
                log.debug("Received Response: {},{},{}", this.token.sid, this.token.uid, response);
                var sink = pendingResponses.remove(response.id());
                if (sink == null) {
                    log.warn("Unexpected response for unknown id {}", response.id());
                } else {
                    sink.success(response);
                }
                return Mono.empty();
            } else if (message instanceof McpSchema.JSONRPCRequest request) {
                log.debug("Received request: {},{},{}", this.token.sid, this.token.uid, request);
                return handleIncomingRequest(request).onErrorResume(error -> {
                    var errorResponse = new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), null,
                            new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR,
                                    error.getMessage(), null));
                    // TODO: Should the error go to SSE or back as POST return?
                    return this.transport.sendMessage(errorResponse).then(Mono.empty());
                }).flatMap(this.transport::sendMessage);
            } else if (message instanceof McpSchema.JSONRPCNotification notification) {
                // TODO handle errors for communication to without initialization
                // happening first
                log.debug("Received notification: {},{},{}", this.token.sid, this.token.uid, notification);
                // TODO: in case of error, should the POST request be signalled?
                return handleIncomingNotification(notification)
                        .doOnError(error -> log.error("Error handling notification: {}", error.getMessage()));
            } else {
                log.warn("Received unknown message type: {},{},{}", this.token.sid, this.token.uid, message);
                return Mono.empty();
            }
        });
    }

    /**
     * Handles an incoming JSON-RPC request by routing it to the appropriate handler.
     *
     * @param request The incoming JSON-RPC request
     * @return A Mono containing the JSON-RPC response
     */
    private Mono<McpSchema.JSONRPCResponse> handleIncomingRequest(McpSchema.JSONRPCRequest request) {
        return Mono.defer(() -> {
            Mono<?> resultMono;
            if (McpSchema.METHOD_INITIALIZE.equals(request.method())) {
                // TODO handle situation where already initialized!
                McpSchema.InitializeRequest initializeRequest = transport.unmarshalFrom(request.params(),
                        new TypeReference<McpSchema.InitializeRequest>() {
                        });

                this.state.lazySet(STATE_INITIALIZING);
                this.init(initializeRequest.capabilities(), initializeRequest.clientInfo());
                resultMono = this.initRequestHandler.handle(initializeRequest);
            } else {
                // TODO handle errors for communication to this session without
                // initialization happening first
                var handler = this.requestHandlers.get(request.method());
                if (handler == null) {
                    MethodNotFoundError error = getMethodNotFoundError(request.method());
                    return Mono.just(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), null,
                            new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND,
                                    error.message(), error.data())));
                }

                resultMono = this.exchangeSink.asMono().flatMap(exchange -> handler.handle(exchange, request.params()));
            }
            return resultMono
                    .map(result -> new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), result, null))
                    .onErrorResume(error -> Mono.just(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(),
                            null, new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR,
                            error.getMessage(), null)))); // TODO: add error message
            // through the data field
        });
    }

    /**
     * Handles an incoming JSON-RPC notification by routing it to the appropriate handler.
     *
     * @param notification The incoming JSON-RPC notification
     * @return A Mono that completes when the notification is processed
     */
    private Mono<Void> handleIncomingNotification(McpSchema.JSONRPCNotification notification) {
        return Mono.defer(() -> {
            if (McpSchema.METHOD_NOTIFICATION_INITIALIZED.equals(notification.method())) {
                this.state.lazySet(STATE_INITIALIZED);
                exchangeSink.tryEmitValue(new MyMcpAsyncServerExchange(this, clientCapabilities.get(), clientInfo.get()));
                return this.initNotificationHandler.handle();
            }

            var handler = notificationHandlers.get(notification.method());
            if (handler == null) {
                log.error("No handler registered for notification method: {}", notification.method());
                return Mono.empty();
            }
            return this.exchangeSink.asMono().flatMap(exchange -> handler.handle(exchange, notification.params()));
        });
    }

    private MethodNotFoundError getMethodNotFoundError(String method) {
        return new MethodNotFoundError(method, "Method not found: " + method, null);
    }

    @Override
    public Mono<Void> closeGracefully() {
        return this.transport.closeGracefully();
    }

    @Override
    public void close() {
        this.transport.close();
    }

}
