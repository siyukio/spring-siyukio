package io.github.siyukio.client;

import com.agentclientprotocol.sdk.client.AcpAsyncClient;
import com.agentclientprotocol.sdk.client.SimpleAsyncSpec;
import com.agentclientprotocol.sdk.spec.AcpClientSession;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import com.github.benmanes.caffeine.cache.Cache;
import io.github.siyukio.client.transport.WebSocketAcpClientTransport;
import io.github.siyukio.tools.acp.AcpSchemaExt;
import io.github.siyukio.tools.acp.Invoke;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Bugee
 */
@Slf4j
public class SimpleAsyncAcpClient {
    private final static String IN_PROGRESS = "in_progress";
    @Getter
    private final URI uri;
    private final WebSocketAcpClientTransport webSocketAcpClientTransport;
    private final AcpAsyncClient acpAsyncClient;
    private final String callToolSessionId;
    private final Cache<String, String> toolCallUpdateCache;

    public SimpleAsyncAcpClient(
            URI uri,
            WebSocketAcpClientTransport webSocketAcpClientTransport,
            AcpAsyncClient acpAsyncClient,
            String callToolSessionId,
            Cache<String, String> toolCallUpdateCache) {
        this.uri = uri;
        this.webSocketAcpClientTransport = webSocketAcpClientTransport;
        this.acpAsyncClient = acpAsyncClient;
        this.callToolSessionId = callToolSessionId;
        this.toolCallUpdateCache = toolCallUpdateCache;
    }

    public boolean isClosed() {
        return this.webSocketAcpClientTransport.isClosing();
    }

    public <T> T callTool(String tool, Object params, Class<T> typeClass) {

        if (!StringUtils.hasText(this.callToolSessionId)) {
            throw new ApiException("Unsupported callTool");
        }
        Invoke invoke = Invoke.create(tool, params);
        String toolCallText = WebSocketAcpClientTransport.createAcpToolCall(invoke);
        List<AcpSchema.ContentBlock> prompts = new ArrayList<>();
        prompts.add(new AcpSchema.TextContent(toolCallText));

        this.toolCallUpdateCache.put(invoke.toolCallId(), IN_PROGRESS);
        try {
            AcpSchema.PromptRequest promptRequest = new AcpSchema.PromptRequest(this.callToolSessionId, prompts);
            AcpSchema.PromptResponse promptResponse = this.acpAsyncClient.prompt(promptRequest).block();
            log.debug("{}", XDataUtils.toPrettyJSONString(promptResponse));
            String cacheValue = this.toolCallUpdateCache.getIfPresent(invoke.toolCallId());
            if (cacheValue != null && !cacheValue.equals(IN_PROGRESS)) {
                if (typeClass.equals(Void.class)) {
                    return null;
                } else {
                    return XDataUtils.parse(cacheValue, typeClass);
                }
            }
        } catch (AcpClientSession.AcpError ex) {
            throw new ApiException(ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            Throwable t = ex.getCause();
            if (t instanceof TimeoutException) {
                throw new ApiException("CallTool timeout: " + invoke.tool() + "," + invoke.toolCallId());
            } else {
                throw new ApiException("CallTool error: " + invoke.tool() + "," + invoke.toolCallId() + ex.getMessage());
            }
        } finally {
            this.toolCallUpdateCache.invalidate(invoke.toolCallId());
        }

        throw new ApiException("CallTool no response:" + invoke.tool() + "," + invoke.toolCallId());
    }

    public AcpSchemaExt.ListToolsResult listTools() {
        return this.callTool(AcpSchemaExt.LIST_TOOLS, new JSONObject(), AcpSchemaExt.ListToolsResult.class);
    }

    public AcpSchema.NewSessionResponse newSession() {
        String cwd = "/" + IdUtils.getUniqueId();
        AcpSchema.NewSessionRequest newSessionRequest = new AcpSchema.NewSessionRequest(cwd, List.of(), Map.of());
        return this.acpAsyncClient.newSession(newSessionRequest).block();
    }

    public AcpSchema.LoadSessionResponse loadSession(String sessionId) {
        String cwd = "/" + IdUtils.getUniqueId();
        AcpSchema.LoadSessionRequest loadSessionRequest = new AcpSchema.LoadSessionRequest(sessionId, cwd, List.of(), Map.of());
        return this.acpAsyncClient.loadSession(loadSessionRequest).block();
    }

    public void cancel(String sessionId) {
        AcpSchema.CancelNotification cancelNotification = new AcpSchema.CancelNotification(sessionId);
        this.acpAsyncClient.cancel(cancelNotification).block();
    }

    public AcpSchema.SetSessionModeResponse setSessionMode(String sessionId, String modeId) {
        AcpSchema.SetSessionModeRequest setModeRequest = new AcpSchema.SetSessionModeRequest(sessionId, modeId);
        return this.acpAsyncClient.setSessionMode(setModeRequest).block();
    }

    public AcpSchema.SetSessionModelResponse setSessionModel(String sessionId, String modelId) {
        AcpSchema.SetSessionModelRequest setModelRequest = new AcpSchema.SetSessionModelRequest(sessionId, modelId);
        return this.acpAsyncClient.setSessionModel(setModelRequest).block();
    }

    public AcpSchema.PromptResponse prompt(String sessionId, String prompt) {
        List<AcpSchema.ContentBlock> prompts = new ArrayList<>();
        prompts.add(new AcpSchema.TextContent(prompt));
        AcpSchema.PromptRequest promptRequest = new AcpSchema.PromptRequest(sessionId, prompts);
        return this.acpAsyncClient.prompt(promptRequest).block();
    }

    public void close() {
        this.acpAsyncClient.close();
    }

    @FunctionalInterface
    public interface ProgressNotificationHandler {

        void handle(AcpSchemaExt.ProgressNotification progressNotification);

    }

    @FunctionalInterface
    public interface SessionNotificationHandler {

        void handle(AcpSchema.SessionNotification sessionNotification);

    }

    @FunctionalInterface
    public interface RequestPermissionHandler {

        AcpSchema.RequestPermissionResponse handle(AcpSchema.RequestPermissionRequest request);

    }

    @FunctionalInterface
    public interface CreateTerminalHandler {

        AcpSchema.CreateTerminalResponse handle(AcpSchema.CreateTerminalRequest request);

    }

    @FunctionalInterface
    public interface WaitForTerminalExitHandler {

        AcpSchema.WaitForTerminalExitResponse handle(AcpSchema.WaitForTerminalExitRequest request);

    }

    @FunctionalInterface
    public interface TerminalOutputHandler {

        AcpSchema.TerminalOutputResponse handle(AcpSchema.TerminalOutputRequest request);

    }

    @FunctionalInterface
    public interface ReleaseTerminalHandler {

        AcpSchema.ReleaseTerminalResponse handle(AcpSchema.ReleaseTerminalRequest request);

    }

    @FunctionalInterface
    public interface ReadTextFileHandler {

        AcpSchema.ReadTextFileResponse handle(AcpSchema.ReadTextFileRequest request);

    }

    @FunctionalInterface
    public interface WriteTextFileHandler {

        AcpSchema.WriteTextFileResponse handle(AcpSchema.WriteTextFileRequest request);

    }

    public interface TerminalHandler {

        CreateTerminalHandler createTerminalHandler();

        WaitForTerminalExitHandler waitForTerminalExitHandler();

        TerminalOutputHandler terminalOutputHandler();

        ReleaseTerminalHandler releaseTerminalHandler();
    }

    public static class SessionUpdateNotificationHandler implements AcpClientSession.NotificationHandler {

        private final List<ProgressNotificationHandler> progressNotificationHandlers;
        private final List<SessionNotificationHandler> sessionNotificationHandlers;

        private final Cache<String, String> toolCallUpdateCache;
        
        public SessionUpdateNotificationHandler(
                List<ProgressNotificationHandler> progressNotificationHandlers,
                List<SessionNotificationHandler> sessionNotificationHandlers,
                Cache<String, String> toolCallUpdateCache) {
            this.progressNotificationHandlers = progressNotificationHandlers;
            this.sessionNotificationHandlers = sessionNotificationHandlers;
            this.toolCallUpdateCache = toolCallUpdateCache;
        }

        private String readJsonText(AcpSchema.ToolCallUpdateNotification toolCallUpdateNotification) {
            if (CollectionUtils.isEmpty(toolCallUpdateNotification.content())) {
                return null;
            }
            AcpSchema.ToolCallContent toolCallContent = toolCallUpdateNotification.content().getFirst();
            if (toolCallContent instanceof AcpSchema.ToolCallContentBlock block) {
                AcpSchema.ContentBlock contentBlock = block.content();
                if (contentBlock instanceof AcpSchema.Resource resource) {
                    AcpSchema.EmbeddedResourceResource embeddedResourceResource = resource.resource();
                    if (embeddedResourceResource instanceof AcpSchema.TextResourceContents textResourceContents) {
                        if (textResourceContents.mimeType().equals(MimeTypeUtils.APPLICATION_JSON_VALUE)) {
                            return textResourceContents.text();
                        }
                    }
                }
            }
            return null;
        }

        private void handleSessionNotification(AcpSchema.SessionNotification sessionNotification) {
            this.sessionNotificationHandlers.forEach(handler -> {
                try {
                    handler.handle(sessionNotification);
                } catch (Exception ignored) {
                }
            });
        }

        @Override
        public Mono<Void> handle(Object notification) {
            log.debug("{}: {}", AcpSchema.METHOD_SESSION_UPDATE, XDataUtils.toPrettyJSONString(notification));
            AcpSchema.SessionNotification sessionNotification = XDataUtils.copy(notification, AcpSchema.SessionNotification.class);
            AcpSchema.SessionUpdate sessionUpdate = sessionNotification.update();
            if (sessionUpdate instanceof AcpSchema.ToolCallUpdateNotification toolCallUpdateNotification) {
                String toolCallId = toolCallUpdateNotification.toolCallId();
                String cacheValue = this.toolCallUpdateCache.getIfPresent(toolCallId);
                if (cacheValue != null && cacheValue.equals(IN_PROGRESS)) {
                    if (toolCallUpdateNotification.status().equals(AcpSchema.ToolCallStatus.COMPLETED)) {
                        String jsonText = this.readJsonText(toolCallUpdateNotification);
                        if (StringUtils.hasText(jsonText)) {
                            this.toolCallUpdateCache.put(toolCallId, jsonText);
                        }
                    } else if (toolCallUpdateNotification.status().equals(AcpSchema.ToolCallStatus.IN_PROGRESS)) {
                        String jsonText = this.readJsonText(toolCallUpdateNotification);
                        if (StringUtils.hasText(jsonText)) {
                            AcpSchemaExt.ProgressNotification progressNotification = XDataUtils.parse(jsonText, AcpSchemaExt.ProgressNotification.class);
                            AcpSchemaExt.ProgressNotification completeProgressNotification = progressNotification.withToolCallId(toolCallId);
                            this.progressNotificationHandlers.forEach(handler -> {
                                try {
                                    handler.handle(completeProgressNotification);
                                } catch (Exception ignored) {
                                }
                            });
                        }
                    }
                } else {
                    this.handleSessionNotification(sessionNotification);
                }
            } else {
                this.handleSessionNotification(sessionNotification);
            }
            return Mono.empty();
        }
    }

    public record Builder(
            Cache<String, String> toolCallUpdateCache,
            List<ProgressNotificationHandler> progressNotificationHandlers,
            List<SessionNotificationHandler> sessionNotificationHandlers,
            RequestPermissionHandler requestPermissionHandler,
            TerminalHandler terminalHandler,
            ReadTextFileHandler readTextFileHandler,
            WriteTextFileHandler writeTextFileHandler,
            Duration requestTimeout,
            Duration connectTimeout,
            String authorization
    ) {

        public SimpleAsyncAcpClient build(URI uri) {
            WebSocketAcpClientTransport webSocketAcpClientTransport = new WebSocketAcpClientTransport(uri.toString(), Map.of("authorization", this.authorization))
                    .connectTimeout(this.connectTimeout);

            SessionUpdateNotificationHandler sessionUpdateNotificationHandler = new SessionUpdateNotificationHandler(
                    this.progressNotificationHandlers, this.sessionNotificationHandlers, this.toolCallUpdateCache);
            SimpleAsyncSpec simpleAsyncSpec = new SimpleAsyncSpec(webSocketAcpClientTransport)
                    .requestTimeout(this.requestTimeout)
                    .notificationHandler(AcpSchema.METHOD_SESSION_UPDATE, sessionUpdateNotificationHandler);
            if (this.requestPermissionHandler != null) {
                simpleAsyncSpec.requestPermissionHandler(request -> Mono.just(this.requestPermissionHandler.handle(request)));
            }
            boolean terminal = false;
            if (this.terminalHandler != null) {
                terminal = true;
                simpleAsyncSpec.createTerminalHandler(request ->
                                Mono.just(this.terminalHandler.createTerminalHandler().handle(request)))
                        .waitForTerminalExitHandler(request ->
                                Mono.just(this.terminalHandler.waitForTerminalExitHandler().handle(request)))
                        .terminalOutputHandler(request -> Mono.just(this.terminalHandler.terminalOutputHandler().handle(request)))
                        .releaseTerminalHandler(releaseTerminalRequest -> Mono.just(this.terminalHandler.releaseTerminalHandler().handle(releaseTerminalRequest)));
            }

            boolean readTextFile = false;
            if (this.readTextFileHandler != null) {
                readTextFile = true;
                simpleAsyncSpec.readTextFileHandler(request -> Mono.just(this.readTextFileHandler.handle(request)));
            }
            boolean writeTextFile = false;
            if (this.writeTextFileHandler != null) {
                writeTextFile = true;
                simpleAsyncSpec.writeTextFileHandler(request -> Mono.just(this.writeTextFileHandler.handle(request)));
            }

            AcpAsyncClient acpAsyncClient = simpleAsyncSpec.build();

            AcpSchema.FileSystemCapability fileSystemCapability = new AcpSchema.FileSystemCapability(readTextFile, writeTextFile);
            AcpSchema.ClientCapabilities clientCapabilities = new AcpSchema.ClientCapabilities(fileSystemCapability, terminal);
            AcpSchema.InitializeRequest initializeRequest = new AcpSchema.InitializeRequest(1, clientCapabilities);

            AcpSchema.InitializeResponse initializeResponse = acpAsyncClient.initialize(initializeRequest).block();
            log.debug("Init async acp client: {}, {}", uri, XDataUtils.toJSONString(initializeResponse));
            String callToolSessionId = "";
            assert initializeResponse != null;
            if (!CollectionUtils.isEmpty(initializeResponse.authMethods())) {
                for (AcpSchema.AuthMethod authMethod : initializeResponse.authMethods()) {
                    if (authMethod.name().equals(AcpSchemaExt.DEFAULT_AUTH_METHOD_NAME)) {
                        callToolSessionId = authMethod.id();
                        break;
                    }
                }
            }
            return new SimpleAsyncAcpClient(
                    uri,
                    webSocketAcpClientTransport,
                    acpAsyncClient,
                    callToolSessionId, toolCallUpdateCache);
        }
    }
}
