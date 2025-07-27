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
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.util.Assert;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider.*;

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
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final Set<String> invalidHeaderSet = Set.of("connection", "content-length", "expect", "host", "upgrade");
    private McpServerSession.Factory sessionFactory;
    /**
     * Flag indicating if the transport is shutting down.
     */
    private volatile boolean isClosing = false;

    /**
     * Constructs a new WebMvcSseServerTransportProvider instance with the default SSE
     * endpoint.
     *
     * @param objectMapper    The ObjectMapper to use for JSON serialization/deserialization
     *                        of messages.
     * @param messageEndpoint The endpoint URI where clients should send their JSON-RPC
     *                        messages via HTTP POST. This endpoint will be communicated to clients through the
     *                        SSE connection's initial endpoint event.
     * @throws IllegalArgumentException if either objectMapper or messageEndpoint is null
     */
    public MyWebMvcSseServerTransportProvider(TokenProvider tokenProvider, ObjectMapper objectMapper, String messageEndpoint, String sseEndpoint) {
        this(tokenProvider, objectMapper, "", messageEndpoint, sseEndpoint);
    }

    /**
     * Constructs a new WebMvcSseServerTransportProvider instance.
     *
     * @param objectMapper    The ObjectMapper to use for JSON serialization/deserialization
     *                        of messages.
     * @param baseUrl         The base URL for the message endpoint, used to construct the full
     *                        endpoint URL for clients.
     * @param messageEndpoint The endpoint URI where clients should send their JSON-RPC
     *                        messages via HTTP POST. This endpoint will be communicated to clients through the
     *                        SSE connection's initial endpoint event.
     * @param sseEndpoint     The endpoint URI where clients establish their SSE connections.
     * @throws IllegalArgumentException if any parameter is null
     */
    public MyWebMvcSseServerTransportProvider(TokenProvider tokenProvider, ObjectMapper objectMapper, String baseUrl, String messageEndpoint,
                                              String sseEndpoint) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        Assert.notNull(baseUrl, "Message base URL must not be null");
        Assert.notNull(messageEndpoint, "Message endpoint must not be null");
        Assert.notNull(sseEndpoint, "SSE endpoint must not be null");

        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.messageEndpoint = messageEndpoint;
        this.sseEndpoint = sseEndpoint;
        this.routerFunction = RouterFunctions.route()
                .GET(this.sseEndpoint, this::handleSseConnection)
                .POST(this.messageEndpoint, this::handleMessage)
                .build();
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
                })
                .flatMap(McpServerSession::closeGracefully)
                .then()
                .doOnSuccess(v -> log.debug("Graceful shutdown completed"));
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
            return ServerResponse.status(e.error).body(e.message);
        }

        String sessionId = IdUtils.getUniqueId();
        log.debug("Creating new SSE connection for session: {}", sessionId);
        token.sid = sessionId;
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

        if (!request.param("sessionId").isPresent()) {
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
                            if (!invalidHeaderSet.contains(name)) {
                                values.forEach(value -> builder.header(name, value));
                            }
                        }
                );
                this.httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.discarding()).get();
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
            String body = request.body(String.class);
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);
            // Process the message through the session's handle method
            session.handle(message).block(); // Block for WebMVC compatibility

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
     * Implementation of McpServerTransport for WebMVC SSE sessions. This class handles
     * the transport-level communication for a specific client session.
     */
    private class WebMvcMcpSessionTransport implements McpServerTransport {

        private final String sessionId;

        private final SseBuilder sseBuilder;

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
                try {
                    String jsonText = objectMapper.writeValueAsString(message);
                    sseBuilder.id(sessionId).event(MESSAGE_EVENT_TYPE).data(jsonText);
                    log.debug("Message sent to session {}", sessionId);
                } catch (Exception e) {
                    log.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
                    sseBuilder.error(e);
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
                try {
                    sseBuilder.complete();
                    log.debug("Successfully completed SSE builder for session {}", sessionId);
                } catch (Exception e) {
                    log.warn("Failed to complete SSE builder for session {}: {}", sessionId, e.getMessage());
                }
            });
        }

        /**
         * Closes the transport immediately.
         */
        @Override
        public void close() {
            try {
                sseBuilder.complete();
                log.debug("Successfully completed SSE builder for session {}", sessionId);
            } catch (Exception e) {
                log.warn("Failed to complete SSE builder for session {}: {}", sessionId, e.getMessage());
            }
        }

    }

}
