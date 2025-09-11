/*
 * Copyright 2024-2024 the original author or authors.
 */

package io.modelcontextprotocol.server.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.ApiProfiles;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.server.DefaultMcpTransportContext;
import io.modelcontextprotocol.server.McpTransportContext;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.util.Assert;
import io.modelcontextprotocol.util.KeepAliveScheduler;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.function.ServerResponse.SseBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Server-side implementation of the Model Context Protocol (MCP) transport layer using
 * HTTP with Server-Sent Events (SSE) through Spring WebMVC. This implementation provides
 * a bridge between synchronous WebMVC operations and reactive programming patterns to
 * maintain compatibility with the reactive transport interface.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Implements bidirectional communication using HTTP POST for client-to-server
 * messages and SSE for server-to-client messages</li>
 * <li>Manages client sessions with unique IDs for reliable message delivery</li>
 * <li>Supports graceful shutdown with proper session cleanup</li>
 * <li>Provides JSON-RPC message handling through configured endpoints</li>
 * <li>Includes built-in error handling and logging</li>
 * </ul>
 *
 * <p>
 * The transport operates on two main endpoints:
 * <ul>
 * <li>{@code /sse} - The SSE endpoint where clients establish their event stream
 * connection</li>
 * <li>A configurable message endpoint where clients send their JSON-RPC messages via HTTP
 * POST</li>
 * </ul>
 *
 * <p>
 * This implementation uses {@link ConcurrentHashMap} to safely manage multiple client
 * sessions in a thread-safe manner. Each client session is assigned a unique ID and
 * maintains its own SSE connection.
 *
 * @author Christian Tzolov
 * @author Alexandros Pappas
 * @see McpServerTransportProvider
 * @see RouterFunction
 */
@Slf4j
public class MyWebMvcSseServerTransportProvider implements McpServerTransportProvider {

    /**
     * Event type for JSON-RPC messages sent through the SSE connection.
     */
    public static final String MESSAGE_EVENT_TYPE = "message";
    /**
     * Event type for sending the message endpoint URI to clients.
     */
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";
    /**
     * Default SSE endpoint path as specified by the MCP transport specification.
     */
    public static final String DEFAULT_SSE_ENDPOINT = "/sse";
    private final static HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper;
    private final String messageEndpoint;
    private final String sseEndpoint;
    private final String baseUrl;
    private final RouterFunction<ServerResponse> routerFunction;
    private final TokenProvider tokenProvider;
    /**
     * Map of active client sessions, keyed by session ID.
     */
    private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();
    private final McpTransportContextExtractor<ServerRequest> contextExtractor;
    private McpServerSession.Factory sessionFactory;
    /**
     * Flag indicating if the transport is shutting down.
     */
    private volatile boolean isClosing = false;
    private KeepAliveScheduler keepAliveScheduler;

    /**
     * Constructs a new WebMvcSseServerTransportProvider instance.
     *
     * @param objectMapper      The ObjectMapper to use for JSON serialization/deserialization
     *                          of messages.
     * @param baseUrl           The base URL for the message endpoint, used to construct the full
     *                          endpoint URL for clients.
     * @param messageEndpoint   The endpoint URI where clients should send their JSON-RPC
     *                          messages via HTTP POST. This endpoint will be communicated to clients through the
     *                          SSE connection's initial endpoint event.
     * @param sseEndpoint       The endpoint URI where clients establish their SSE connections.
     * @param keepAliveInterval The interval for sending keep-alive messages to clients.
     * @param contextExtractor  The contextExtractor to fill in a
     *                          {@link McpTransportContext}.
     * @throws IllegalArgumentException if any parameter is null
     */
    private MyWebMvcSseServerTransportProvider(TokenProvider tokenProvider, ObjectMapper objectMapper, String baseUrl, String messageEndpoint,
                                               String sseEndpoint, Duration keepAliveInterval,
                                               McpTransportContextExtractor<ServerRequest> contextExtractor) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(baseUrl, "Message base URL must not be null");
        Assert.notNull(messageEndpoint, "Message endpoint must not be null");
        Assert.notNull(sseEndpoint, "SSE endpoint must not be null");
        Assert.notNull(contextExtractor, "Context extractor must not be null");

        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.messageEndpoint = messageEndpoint;
        this.sseEndpoint = sseEndpoint;
        this.contextExtractor = contextExtractor;
        this.routerFunction = RouterFunctions.route()
                .GET(this.sseEndpoint, this::handleSseConnection)
                .POST(this.messageEndpoint, this::handleMessage)
                .build();

        if (keepAliveInterval != null) {

            this.keepAliveScheduler = KeepAliveScheduler
                    .builder(() -> (isClosing) ? Flux.empty() : Flux.fromIterable(sessions.values()))
                    .initialDelay(keepAliveInterval)
                    .interval(keepAliveInterval)
                    .build();

            this.keepAliveScheduler.start();
        }
    }

    /**
     * Creates a new Builder instance for configuring and creating instances of
     * WebMvcSseServerTransportProvider.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public List<String> protocolVersions() {
        return List.of(ProtocolVersions.MCP_2024_11_05);
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Broadcasts a notification to all connected clients through their SSE connections.
     * The message is serialized to JSON and sent as an SSE event with type "message". If
     * any errors occur during sending to a particular client, they are logged but don't
     * prevent sending to other clients.
     *
     * @param method The method name for the notification
     * @param params The parameters for the notification
     * @return A Mono that completes when the broadcast attempt is finished
     */
    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (sessions.isEmpty()) {
            log.debug("No active sessions to broadcast message to");
            return Mono.empty();
        }

        log.debug("Attempting to broadcast message to {} active sessions", sessions.size());

        return Flux.fromIterable(sessions.values())
                .flatMap(session -> session.sendNotification(method, params)
                        .doOnError(
                                e -> log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage()))
                        .onErrorComplete())
                .then();
    }

    /**
     * Initiates a graceful shutdown of the transport. This method:
     * <ul>
     * <li>Sets the closing flag to prevent new connections</li>
     * <li>Closes all active SSE connections</li>
     * <li>Removes all session records</li>
     * </ul>
     *
     * @return A Mono that completes when all cleanup operations are finished
     */
    @Override
    public Mono<Void> closeGracefully() {
        return Flux.fromIterable(sessions.values()).doFirst(() -> {
            this.isClosing = true;
            log.debug("Initiating graceful shutdown with {} active sessions", sessions.size());
        }).flatMap(McpServerSession::closeGracefully).then().doOnSuccess(v -> {
            log.debug("Graceful shutdown completed");
            sessions.clear();
            if (this.keepAliveScheduler != null) {
                this.keepAliveScheduler.shutdown();
            }
        });
    }

    /**
     * Returns the RouterFunction that defines the HTTP endpoints for this transport. The
     * router function handles two endpoints:
     * <ul>
     * <li>GET /sse - For establishing SSE connections</li>
     * <li>POST [messageEndpoint] - For receiving JSON-RPC messages from clients</li>
     * </ul>
     *
     * @return The configured RouterFunction for handling HTTP requests
     */
    public RouterFunction<ServerResponse> getRouterFunction() {
        return this.routerFunction;
    }

    private Token validateToken(ServerRequest request) {

        String authorization = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED);
        }

        Token token = this.tokenProvider.verifyToken(authorization);
        if (token == null || token.refresh || token.expired) {
            throw new ApiException(HttpStatus.UNAUTHORIZED);
        }
        return token;
    }

    private String encodeSessionId(String sessionId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sessionId", sessionId);
        jsonObject.put("originIp", ApiProfiles.IP4);
        jsonObject.put("originPort", ApiProfiles.PORT);
        String json = JsonUtils.toJSONString(jsonObject);
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private JSONObject decodeSessionId(String sessionId) {
        byte[] decode = Base64.getDecoder().decode(sessionId.getBytes(StandardCharsets.UTF_8));
        String json = new String(decode, StandardCharsets.UTF_8);
        return JsonUtils.parseObject(json);
    }

    /**
     * Handles new SSE connection requests from clients by creating a new session and
     * establishing an SSE connection. This method:
     * <ul>
     * <li>Generates a unique session ID</li>
     * <li>Creates a new session with a WebMvcMcpSessionTransport</li>
     * <li>Sends an initial endpoint event to inform the client where to send
     * messages</li>
     * <li>Maintains the session in the sessions map</li>
     * </ul>
     *
     * @param request The incoming server request
     * @return A ServerResponse configured for SSE communication, or an error response if
     * the server is shutting down or the connection fails
     */
    private ServerResponse handleSseConnection(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        }

        //from internal
        String from = request.headers().firstHeader(HttpHeaders.FROM);
        if (StringUtils.hasText(from) && from.equalsIgnoreCase("internal")) {
            return ServerResponse.status(HttpStatus.TEMPORARY_REDIRECT)
                    .location(URI.create("http://" + ApiProfiles.IP4 + ":" + ApiProfiles.PORT + DEFAULT_SSE_ENDPOINT)).build();
        }

        final Token token;
        try {
            token = this.validateToken(request);
        } catch (ApiException e) {
            return ServerResponse.status(e.error).body(e.errorReason);
        }


        String sessionId = IdUtils.getUniqueId();
        log.debug("Creating new SSE connection for session: {}", sessionId);

        // Send initial endpoint event
        try {
            return ServerResponse.sse(sseBuilder -> {
                sseBuilder.onComplete(() -> {
                    log.debug("SSE connection completed for session: {}", sessionId);
                    sessions.remove(sessionId);
                });
                sseBuilder.onTimeout(() -> {
                    log.debug("SSE connection timed out for session: {}", sessionId);
                    sessions.remove(sessionId);
                });

                WebMvcMcpSessionTransport sessionTransport = new WebMvcMcpSessionTransport(sessionId, sseBuilder);
                McpServerSession session = sessionFactory.create(sessionTransport);
                this.sessions.put(sessionId, session);

                if (session instanceof MyMcpServerSession myMcpServerSession) {
                    myMcpServerSession.setToken(token);
                }

                String encodeSessionId = this.encodeSessionId(sessionId);
                try {
                    sseBuilder.id(sessionId)
                            .event(ENDPOINT_EVENT_TYPE)
                            .data(this.baseUrl + this.messageEndpoint + "?sessionId=" + encodeSessionId);
                } catch (Exception e) {
                    log.error("Failed to send initial endpoint event: {}", e.getMessage());
                    sseBuilder.error(e);
                }
            }, Duration.ZERO);
        } catch (Exception e) {
            log.error("Failed to send initial endpoint event to session {}: {}", sessionId, e.getMessage());
            sessions.remove(sessionId);
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Handles incoming JSON-RPC messages from clients. This method:
     * <ul>
     * <li>Deserializes the request body into a JSON-RPC message</li>
     * <li>Processes the message through the session's handle method</li>
     * <li>Returns appropriate HTTP responses based on the processing result</li>
     * </ul>
     *
     * @param request The incoming server request containing the JSON-RPC message
     * @return A ServerResponse indicating success (200 OK) or appropriate error status
     * with error details in case of failures
     */
    private ServerResponse handleMessage(ServerRequest request) {
        if (this.isClosing) {
            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
        }

        if (request.param("sessionId").isEmpty()) {
            return ServerResponse.badRequest().body(new McpError("Session ID missing in message endpoint"));
        }

        String sessionId = request.param("sessionId").get();
        JSONObject decodeSession = this.decodeSessionId(sessionId);

        String originIp = decodeSession.optString("originIp", "");
        int originPort = decodeSession.optInt("originPort", 80);
        if (!originIp.equals(ApiProfiles.IP4) || originPort != ApiProfiles.PORT) {
            String newBaseUri = "http://" + originIp + ":" + originPort + this.messageEndpoint + "?sessionId=" + sessionId;
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(newBaseUri));
            try {
                String body = request.body(String.class);
                builder.method(request.method().name(), HttpRequest.BodyPublishers.ofString(body));
                request.headers().asHttpHeaders().forEach((name, values) ->
                        {
                            Set<String> invalidHeaderSet = Set.of("connection", "content-length", "expect", "host", "upgrade");
                            if (!invalidHeaderSet.contains(name)) {
                                values.forEach(value -> builder.header(name, value));
                            }
                        }
                );

                HTTP_CLIENT.sendAsync(builder.build(), HttpResponse.BodyHandlers.discarding()).get();
            } catch (Exception e) {
                log.error("Error proxy message: {}", e.getMessage());
                return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new McpError(e.getMessage()));
            }
            return ServerResponse.ok().build();
        }

        sessionId = decodeSession.optString("sessionId", "");
        McpServerSession session = sessions.get(sessionId);

        if (session == null) {
            return ServerResponse.status(HttpStatus.NOT_FOUND).body(new McpError("Session not found: " + sessionId));
        }

        try {
            final McpTransportContext transportContext = this.contextExtractor.extract(request,
                    new DefaultMcpTransportContext());

            String body = request.body(String.class);
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);

            // Process the message through the session's handle method
            session.handle(message).contextWrite(ctx -> ctx.put(McpTransportContext.KEY, transportContext)).block(); // Block
            // for
            // WebMVC
            // compatibility

            return ServerResponse.ok().build();
        } catch (IllegalArgumentException | IOException e) {
            log.error("Failed to deserialize message: {}", e.getMessage());
            return ServerResponse.badRequest().body(new McpError("Invalid message format"));
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new McpError(e.getMessage()));
        }
    }

    /**
     * Builder for creating instances of WebMvcSseServerTransportProvider.
     * <p>
     * This builder provides a fluent API for configuring and creating instances of
     * WebMvcSseServerTransportProvider with custom settings.
     */
    public static class Builder {

        private ObjectMapper objectMapper = new ObjectMapper();

        private String baseUrl = "";

        private String messageEndpoint;

        private String sseEndpoint = DEFAULT_SSE_ENDPOINT;

        private Duration keepAliveInterval;

        private TokenProvider tokenProvider;

        private McpTransportContextExtractor<ServerRequest> contextExtractor = (serverRequest, context) -> context;

        /**
         * Sets the JSON object mapper to use for message serialization/deserialization.
         *
         * @param objectMapper The object mapper to use
         * @return This builder instance for method chaining
         */
        public Builder objectMapper(ObjectMapper objectMapper) {
            Assert.notNull(objectMapper, "ObjectMapper must not be null");
            this.objectMapper = objectMapper;
            return this;
        }

        /**
         * Sets the base URL for the server transport.
         *
         * @param baseUrl The base URL to use
         * @return This builder instance for method chaining
         */
        public Builder baseUrl(String baseUrl) {
            Assert.notNull(baseUrl, "Base URL must not be null");
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * Sets the endpoint path where clients will send their messages.
         *
         * @param messageEndpoint The message endpoint path
         * @return This builder instance for method chaining
         */
        public Builder messageEndpoint(String messageEndpoint) {
            Assert.hasText(messageEndpoint, "Message endpoint must not be empty");
            this.messageEndpoint = messageEndpoint;
            return this;
        }

        /**
         * Sets the endpoint path where clients will establish SSE connections.
         * <p>
         * If not specified, the default value of {@link #DEFAULT_SSE_ENDPOINT} will be
         * used.
         *
         * @param sseEndpoint The SSE endpoint path
         * @return This builder instance for method chaining
         */
        public Builder sseEndpoint(String sseEndpoint) {
            Assert.hasText(sseEndpoint, "SSE endpoint must not be empty");
            this.sseEndpoint = sseEndpoint;
            return this;
        }

        /**
         * Sets the interval for keep-alive pings.
         * <p>
         * If not specified, keep-alive pings will be disabled.
         *
         * @param keepAliveInterval The interval duration for keep-alive pings
         * @return This builder instance for method chaining
         */
        public Builder keepAliveInterval(Duration keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        /**
         * Sets the context extractor that allows providing the MCP feature
         * implementations to inspect HTTP transport level metadata that was present at
         * HTTP request processing time. This allows to extract custom headers and other
         * useful data for use during execution later on in the process.
         *
         * @param contextExtractor The contextExtractor to fill in a
         *                         {@link McpTransportContext}.
         * @return this builder instance
         * @throws IllegalArgumentException if contextExtractor is null
         */
        public Builder contextExtractor(McpTransportContextExtractor<ServerRequest> contextExtractor) {
            Assert.notNull(contextExtractor, "contextExtractor must not be null");
            this.contextExtractor = contextExtractor;
            return this;
        }

        public Builder tokenProvider(TokenProvider tokenProvider) {
            this.tokenProvider = tokenProvider;
            return this;
        }

        /**
         * Builds a new instance of WebMvcSseServerTransportProvider with the configured
         * settings.
         *
         * @return A new WebMvcSseServerTransportProvider instance
         * @throws IllegalStateException if objectMapper or messageEndpoint is not set
         */
        public MyWebMvcSseServerTransportProvider build() {
            if (messageEndpoint == null) {
                throw new IllegalStateException("MessageEndpoint must be set");
            }
            return new MyWebMvcSseServerTransportProvider(this.tokenProvider, objectMapper, baseUrl, messageEndpoint, sseEndpoint,
                    keepAliveInterval, contextExtractor);
        }

    }

    /**
     * Implementation of McpServerTransport for WebMVC SSE sessions. This class handles
     * the transport-level communication for a specific client session.
     */
    private class WebMvcMcpSessionTransport implements McpServerTransport {

        private final String sessionId;

        private final SseBuilder sseBuilder;

        /**
         * Lock to ensure thread-safe access to the SSE builder when sending messages.
         * This prevents concurrent modifications that could lead to corrupted SSE events.
         */
        private final ReentrantLock sseBuilderLock = new ReentrantLock();

        /**
         * Creates a new session transport with the specified ID and SSE builder.
         *
         * @param sessionId  The unique identifier for this session
         * @param sseBuilder The SSE builder for sending server events to the client
         */
        WebMvcMcpSessionTransport(String sessionId, SseBuilder sseBuilder) {
            this.sessionId = sessionId;
            this.sseBuilder = sseBuilder;
            log.debug("Session transport {} initialized with SSE builder", sessionId);
        }

        /**
         * Sends a JSON-RPC message to the client through the SSE connection.
         *
         * @param message The JSON-RPC message to send
         * @return A Mono that completes when the message has been sent
         */
        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return Mono.fromRunnable(() -> {
                sseBuilderLock.lock();
                try {
                    String jsonText = objectMapper.writeValueAsString(message);
                    sseBuilder.id(sessionId).event(MESSAGE_EVENT_TYPE).data(jsonText);
                    log.debug("Message sent to session {}", sessionId);
                } catch (Exception e) {
                    log.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
                    sseBuilder.error(e);
                } finally {
                    sseBuilderLock.unlock();
                }
            });
        }

        /**
         * Converts data from one type to another using the configured ObjectMapper.
         *
         * @param data    The source data object to convert
         * @param typeRef The target type reference
         * @param <T>     The target type
         * @return The converted object of type T
         */
        @Override
        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return objectMapper.convertValue(data, typeRef);
        }

        /**
         * Initiates a graceful shutdown of the transport.
         *
         * @return A Mono that completes when the shutdown is complete
         */
        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(() -> {
                log.debug("Closing session transport: {}", sessionId);
                sseBuilderLock.lock();
                try {
                    sseBuilder.complete();
                    log.debug("Successfully completed SSE builder for session {}", sessionId);
                } catch (Exception e) {
                    log.warn("Failed to complete SSE builder for session {}: {}", sessionId, e.getMessage());
                } finally {
                    sseBuilderLock.unlock();
                }
            });
        }

        /**
         * Closes the transport immediately.
         */
        @Override
        public void close() {
            sseBuilderLock.lock();
            try {
                sseBuilder.complete();
                log.debug("Successfully completed SSE builder for session {}", sessionId);
            } catch (Exception e) {
                log.warn("Failed to complete SSE builder for session {}: {}", sessionId, e.getMessage());
            } finally {
                sseBuilderLock.unlock();
            }
        }

    }

}
