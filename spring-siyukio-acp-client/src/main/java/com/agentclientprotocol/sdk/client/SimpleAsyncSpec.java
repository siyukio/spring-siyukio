package com.agentclientprotocol.sdk.client;

import com.agentclientprotocol.sdk.spec.AcpClientSession;
import com.agentclientprotocol.sdk.spec.AcpClientTransport;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import com.agentclientprotocol.sdk.spec.AcpSession;
import com.agentclientprotocol.sdk.util.Assert;
import io.modelcontextprotocol.json.TypeRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author Bugee
 */
public class SimpleAsyncSpec {

    private static final Logger logger = LoggerFactory.getLogger(SimpleAsyncSpec.class);

    private final AcpClientTransport transport;
    private final Map<String, AcpClientSession.RequestHandler<?>> requestHandlers = new HashMap<>();
    private final Map<String, AcpClientSession.NotificationHandler> notificationHandlers = new HashMap<>();
    private final List<Function<AcpSchema.SessionNotification, Mono<Void>>> sessionUpdateConsumers = new ArrayList<>();
    private Duration requestTimeout = Duration.ofSeconds(30); // Default timeout
    private AcpSchema.ClientCapabilities clientCapabilities;

    public SimpleAsyncSpec(AcpClientTransport transport) {
        Assert.notNull(transport, "Transport must not be null");
        this.transport = transport;
    }

    /**
     * Adds a typed handler for file system read requests from the agent.
     * This is the preferred method as it provides type-safe request handling
     * without manual unmarshalling.
     *
     * <p>Example usage:
     * <pre>{@code
     * .readTextFileHandler(req ->
     *     Mono.fromCallable(() -> Files.readString(Path.of(req.path())))
     *         .map(ReadTextFileResponse::new))
     * }</pre>
     *
     * @param handler The typed handler function that processes read requests
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if handler is null
     */
    public SimpleAsyncSpec readTextFileHandler(
            Function<AcpSchema.ReadTextFileRequest, Mono<AcpSchema.ReadTextFileResponse>> handler) {
        Assert.notNull(handler, "Read text file handler must not be null");
        AcpClientSession.RequestHandler<AcpSchema.ReadTextFileResponse> rawHandler = params -> {
            AcpSchema.ReadTextFileRequest request = transport.unmarshalFrom(params,
                    new TypeRef<AcpSchema.ReadTextFileRequest>() {
                    });
            return handler.apply(request);
        };
        this.requestHandlers.put(AcpSchema.METHOD_FS_READ_TEXT_FILE, rawHandler);
        return this;
    }

    /**
     * Sets the duration to wait for agent responses before timing out requests. This
     * timeout applies to all requests made through the client, including initialize,
     * prompt, and session operations.
     *
     * @param requestTimeout The duration to wait before timing out requests. Must not
     *                       be null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if requestTimeout is null
     */
    public SimpleAsyncSpec requestTimeout(Duration requestTimeout) {
        Assert.notNull(requestTimeout, "Request timeout must not be null");
        this.requestTimeout = requestTimeout;
        return this;
    }

    /**
     * Sets the client capabilities that will be advertised to the agent during
     * initialization. Capabilities define what features the client supports, such as
     * file system operations, terminal access, and authentication methods.
     *
     * @param clientCapabilities The client capabilities configuration. Must not be
     *                           null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if clientCapabilities is null
     */
    public SimpleAsyncSpec clientCapabilities(AcpSchema.ClientCapabilities clientCapabilities) {
        Assert.notNull(clientCapabilities, "Client capabilities must not be null");
        this.clientCapabilities = clientCapabilities;
        return this;
    }

    /**
     * Adds a typed handler for file system write requests from the agent.
     * This is the preferred method as it provides type-safe request handling
     * without manual unmarshalling.
     *
     * <p>Example usage:
     * <pre>{@code
     * .writeTextFileHandler(req ->
     *     Mono.fromRunnable(() -> Files.writeString(Path.of(req.path()), req.content()))
     *         .then(Mono.just(new WriteTextFileResponse())))
     * }</pre>
     *
     * @param handler The typed handler function that processes write requests
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if handler is null
     */
    public SimpleAsyncSpec writeTextFileHandler(
            Function<AcpSchema.WriteTextFileRequest, Mono<AcpSchema.WriteTextFileResponse>> handler) {
        Assert.notNull(handler, "Write text file handler must not be null");
        AcpClientSession.RequestHandler<AcpSchema.WriteTextFileResponse> rawHandler = params -> {
            AcpSchema.WriteTextFileRequest request = transport.unmarshalFrom(params,
                    new TypeRef<AcpSchema.WriteTextFileRequest>() {
                    });
            return handler.apply(request);
        };
        this.requestHandlers.put(AcpSchema.METHOD_FS_WRITE_TEXT_FILE, rawHandler);
        return this;
    }

    /**
     * Adds a typed handler for permission requests from the agent.
     * This is the preferred method as it provides type-safe request handling
     * without manual unmarshalling.
     *
     * <p>Example usage:
     * <pre>{@code
     * .requestPermissionHandler(req ->
     *     Mono.just(new RequestPermissionResponse(
     *         new RequestPermissionOutcome("approve", null))))
     * }</pre>
     *
     * @param handler The typed handler function that processes permission requests
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if handler is null
     */
    public SimpleAsyncSpec requestPermissionHandler(
            Function<AcpSchema.RequestPermissionRequest, Mono<AcpSchema.RequestPermissionResponse>> handler) {
        Assert.notNull(handler, "Request permission handler must not be null");
        AcpClientSession.RequestHandler<AcpSchema.RequestPermissionResponse> rawHandler = params -> {
            AcpSchema.RequestPermissionRequest request = transport.unmarshalFrom(params,
                    new TypeRef<AcpSchema.RequestPermissionRequest>() {
                    });
            return handler.apply(request);
        };
        this.requestHandlers.put(AcpSchema.METHOD_SESSION_REQUEST_PERMISSION, rawHandler);
        return this;
    }

    /**
     * Adds a typed handler for terminal creation requests from the agent.
     *
     * <p>Example usage:
     * <pre>{@code
     * .createTerminalHandler(req -> {
     *     String terminalId = UUID.randomUUID().toString();
     *     // Start process with req.command(), req.args(), req.cwd()
     *     return Mono.just(new CreateTerminalResponse(terminalId));
     * })
     * }</pre>
     *
     * @param handler The typed handler function that processes terminal creation requests
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if handler is null
     */
    public SimpleAsyncSpec createTerminalHandler(
            Function<AcpSchema.CreateTerminalRequest, Mono<AcpSchema.CreateTerminalResponse>> handler) {
        Assert.notNull(handler, "Create terminal handler must not be null");
        AcpClientSession.RequestHandler<AcpSchema.CreateTerminalResponse> rawHandler = params -> {
            AcpSchema.CreateTerminalRequest request = transport.unmarshalFrom(params,
                    new TypeRef<AcpSchema.CreateTerminalRequest>() {
                    });
            return handler.apply(request);
        };
        this.requestHandlers.put(AcpSchema.METHOD_TERMINAL_CREATE, rawHandler);
        return this;
    }

    /**
     * Adds a typed handler for terminal output requests from the agent.
     *
     * <p>Example usage:
     * <pre>{@code
     * .terminalOutputHandler(req -> {
     *     String output = getTerminalOutput(req.terminalId());
     *     return Mono.just(new TerminalOutputResponse(output, false, null));
     * })
     * }</pre>
     *
     * @param handler The typed handler function that processes terminal output requests
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if handler is null
     */
    public SimpleAsyncSpec terminalOutputHandler(
            Function<AcpSchema.TerminalOutputRequest, Mono<AcpSchema.TerminalOutputResponse>> handler) {
        Assert.notNull(handler, "Terminal output handler must not be null");
        AcpClientSession.RequestHandler<AcpSchema.TerminalOutputResponse> rawHandler = params -> {
            AcpSchema.TerminalOutputRequest request = transport.unmarshalFrom(params,
                    new TypeRef<AcpSchema.TerminalOutputRequest>() {
                    });
            return handler.apply(request);
        };
        this.requestHandlers.put(AcpSchema.METHOD_TERMINAL_OUTPUT, rawHandler);
        return this;
    }

    /**
     * Adds a typed handler for terminal release requests from the agent.
     *
     * <p>Example usage:
     * <pre>{@code
     * .releaseTerminalHandler(req -> {
     *     releaseTerminal(req.terminalId());
     *     return Mono.just(new ReleaseTerminalResponse());
     * })
     * }</pre>
     *
     * @param handler The typed handler function that processes terminal release requests
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if handler is null
     */
    public SimpleAsyncSpec releaseTerminalHandler(
            Function<AcpSchema.ReleaseTerminalRequest, Mono<AcpSchema.ReleaseTerminalResponse>> handler) {
        Assert.notNull(handler, "Release terminal handler must not be null");
        AcpClientSession.RequestHandler<AcpSchema.ReleaseTerminalResponse> rawHandler = params -> {
            AcpSchema.ReleaseTerminalRequest request = transport.unmarshalFrom(params,
                    new TypeRef<AcpSchema.ReleaseTerminalRequest>() {
                    });
            return handler.apply(request);
        };
        this.requestHandlers.put(AcpSchema.METHOD_TERMINAL_RELEASE, rawHandler);
        return this;
    }

    /**
     * Adds a typed handler for wait-for-terminal-exit requests from the agent.
     *
     * <p>Example usage:
     * <pre>{@code
     * .waitForTerminalExitHandler(req -> {
     *     int exitCode = waitForExit(req.terminalId());
     *     return Mono.just(new WaitForTerminalExitResponse(exitCode, null));
     * })
     * }</pre>
     *
     * @param handler The typed handler function that processes wait-for-exit requests
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if handler is null
     */
    public SimpleAsyncSpec waitForTerminalExitHandler(
            Function<AcpSchema.WaitForTerminalExitRequest, Mono<AcpSchema.WaitForTerminalExitResponse>> handler) {
        Assert.notNull(handler, "Wait for terminal exit handler must not be null");
        AcpClientSession.RequestHandler<AcpSchema.WaitForTerminalExitResponse> rawHandler = params -> {
            AcpSchema.WaitForTerminalExitRequest request = transport.unmarshalFrom(params,
                    new TypeRef<AcpSchema.WaitForTerminalExitRequest>() {
                    });
            return handler.apply(request);
        };
        this.requestHandlers.put(AcpSchema.METHOD_TERMINAL_WAIT_FOR_EXIT, rawHandler);
        return this;
    }

    /**
     * Adds a typed handler for terminal kill requests from the agent.
     *
     * <p>Example usage:
     * <pre>{@code
     * .killTerminalHandler(req -> {
     *     killProcess(req.terminalId());
     *     return Mono.just(new KillTerminalCommandResponse());
     * })
     * }</pre>
     *
     * @param handler The typed handler function that processes terminal kill requests
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if handler is null
     */
    public SimpleAsyncSpec killTerminalHandler(
            Function<AcpSchema.KillTerminalCommandRequest, Mono<AcpSchema.KillTerminalCommandResponse>> handler) {
        Assert.notNull(handler, "Kill terminal handler must not be null");
        AcpClientSession.RequestHandler<AcpSchema.KillTerminalCommandResponse> rawHandler = params -> {
            AcpSchema.KillTerminalCommandRequest request = transport.unmarshalFrom(params,
                    new TypeRef<AcpSchema.KillTerminalCommandRequest>() {
                    });
            return handler.apply(request);
        };
        this.requestHandlers.put(AcpSchema.METHOD_TERMINAL_KILL, rawHandler);
        return this;
    }

    /**
     * Adds a consumer to be notified when session update notifications are received
     * from the agent. Session updates include agent thoughts, message chunks, and
     * other streaming content during prompt processing.
     *
     * @param sessionUpdateConsumer A consumer that receives session update
     *                              notifications. Must not be null.
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if sessionUpdateConsumer is null
     */
    public SimpleAsyncSpec sessionUpdateConsumer(
            Function<AcpSchema.SessionNotification, Mono<Void>> sessionUpdateConsumer) {
        Assert.notNull(sessionUpdateConsumer, "Session update consumer must not be null");
        this.sessionUpdateConsumers.add(sessionUpdateConsumer);
        return this;
    }

    /**
     * Adds a custom request handler for a specific method. This allows handling
     * additional agent requests beyond the standard file system and permission
     * operations.
     *
     * @param method  The method name (e.g., "custom/operation")
     * @param handler The handler function for this method
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if method or handler is null
     */
    public SimpleAsyncSpec requestHandler(String method, AcpClientSession.RequestHandler<?> handler) {
        Assert.notNull(method, "Method must not be null");
        Assert.notNull(handler, "Handler must not be null");
        this.requestHandlers.put(method, handler);
        return this;
    }

    /**
     * Adds a custom notification handler for a specific method. This allows handling
     * additional agent notifications beyond session updates.
     *
     * @param method  The method name (e.g., "custom/notification")
     * @param handler The handler function for this method
     * @return This builder instance for method chaining
     * @throws IllegalArgumentException if method or handler is null
     */
    public SimpleAsyncSpec notificationHandler(String method, AcpClientSession.NotificationHandler handler) {
        Assert.notNull(method, "Method must not be null");
        Assert.notNull(handler, "Handler must not be null");
        this.notificationHandlers.put(method, handler);
        return this;
    }

    /**
     * Creates an instance of {@link AcpAsyncClient} with the provided configurations
     * or sensible defaults.
     *
     * @return a new instance of {@link AcpAsyncClient}
     */
    public AcpAsyncClient build() {
        // Set up session update notification handler
        if (!sessionUpdateConsumers.isEmpty()) {
            notificationHandlers.put(AcpSchema.METHOD_SESSION_UPDATE, params -> {
                AcpSchema.SessionNotification notification = transport.unmarshalFrom(params,
                        new io.modelcontextprotocol.json.TypeRef<AcpSchema.SessionNotification>() {
                        });
                logger.debug("Received session update for session: {}", notification.sessionId());

                // Call all registered consumers
                return Mono
                        .when(sessionUpdateConsumers.stream().map(consumer -> consumer.apply(notification)).toList());
            });
        }

        // Create session with request and notification handlers
        AcpSession session = new SimpleAcpClientSession(requestTimeout, transport, requestHandlers, notificationHandlers,
                Function.identity());

        return new AcpAsyncClient(session, transport, clientCapabilities);
    }

}
