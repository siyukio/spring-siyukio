package io.github.siyukio.client;

import com.agentclientprotocol.sdk.client.AcpAsyncClientExt;
import com.agentclientprotocol.sdk.client.SimpleAsyncSpec;
import com.agentclientprotocol.sdk.spec.AcpClientSession;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.client.transport.WebSocketAcpClientTransport;
import io.github.siyukio.tools.acp.sdk.spec.AcpSchemaExt;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
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

    @Getter
    private final URI uri;
    @Getter
    private final String agentName;
    private final WebSocketAcpClientTransport webSocketAcpClientTransport;
    private final AcpAsyncClientExt acpAsyncClientExt;

    public SimpleAsyncAcpClient(
            URI uri,
            String agentName,
            WebSocketAcpClientTransport webSocketAcpClientTransport,
            AcpAsyncClientExt acpAsyncClientExt) {
        this.uri = uri;
        this.agentName = agentName;
        this.webSocketAcpClientTransport = webSocketAcpClientTransport;
        this.acpAsyncClientExt = acpAsyncClientExt;
    }

    public boolean isClosed() {
        return this.webSocketAcpClientTransport.isClosing();
    }

    public <T> T callTool(String tool, Object params, Class<T> typeClass, ProgressNotificationHandler progressNotificationHandler) {
        String toolCallId = IdUtils.getUniqueId();
        JSONObject paramsJson = XDataUtils.copy(params, JSONObject.class);
        AcpSchemaExt.CallToolRequest request = new AcpSchemaExt.CallToolRequest(tool, toolCallId, paramsJson);
        AcpClientSession.NotificationHandler toolUpdateNotificationHandler = null;
        if (progressNotificationHandler != null) {
            toolUpdateNotificationHandler = new ToolUpdateNotificationHandler(progressNotificationHandler);
        }
        try {
            JSONObject response = this.acpAsyncClientExt.callTool(request, toolUpdateNotificationHandler).block();
            if (typeClass.equals(Void.class)) {
                return null;
            } else {
                return XDataUtils.copy(response, typeClass);
            }
        } catch (AcpClientSession.AcpError ex) {
            throw new ApiException(ex.getCode(), ex.getMessage());
        } catch (Exception ex) {
            Throwable t = ex.getCause();
            if (t instanceof TimeoutException) {
                throw new ApiException("CallTool timeout: " + tool + "," + toolCallId);
            } else {
                throw new ApiException("CallTool error: " + tool + "," + toolCallId + ex.getMessage());
            }
        }
    }

    public AcpSchemaExt.ListToolsResult listTools() {
        return this.acpAsyncClientExt.listTools(new AcpSchemaExt.ListToolsRequest()).block();
    }

    public AcpSchema.NewSessionResponse newSession() {
        String cwd = "/" + IdUtils.getUniqueId();
        AcpSchema.NewSessionRequest newSessionRequest = new AcpSchema.NewSessionRequest(cwd, List.of(), Map.of(AcpSchemaExt.AGENT_NAME, this.agentName));
        return this.acpAsyncClientExt.newSession(newSessionRequest).block();
    }

    public AcpSchema.LoadSessionResponse loadSession(String sessionId) {
        String cwd = "/" + IdUtils.getUniqueId();
        AcpSchema.LoadSessionRequest loadSessionRequest = new AcpSchema.LoadSessionRequest(sessionId, cwd, List.of(), Map.of(AcpSchemaExt.AGENT_NAME, this.agentName));
        return this.acpAsyncClientExt.loadSession(loadSessionRequest).block();
    }

    public void cancel(String sessionId) {
        AcpSchema.CancelNotification cancelNotification = new AcpSchema.CancelNotification(sessionId);
        this.acpAsyncClientExt.cancel(cancelNotification).block();
    }

    public AcpSchema.SetSessionModeResponse setSessionMode(String sessionId, String modeId) {
        AcpSchema.SetSessionModeRequest setModeRequest = new AcpSchema.SetSessionModeRequest(sessionId, modeId);
        return this.acpAsyncClientExt.setSessionMode(setModeRequest).block();
    }

    public AcpSchema.SetSessionModelResponse setSessionModel(String sessionId, String modelId) {
        AcpSchema.SetSessionModelRequest setModelRequest = new AcpSchema.SetSessionModelRequest(sessionId, modelId);
        return this.acpAsyncClientExt.setSessionModel(setModelRequest).block();
    }

    public AcpSchema.PromptResponse prompt(String sessionId, String prompt) {
        List<AcpSchema.ContentBlock> prompts = new ArrayList<>();
        prompts.add(new AcpSchema.TextContent(prompt));
        AcpSchema.PromptRequest promptRequest = new AcpSchema.PromptRequest(sessionId, prompts, Map.of(AcpSchemaExt.AGENT_NAME, this.agentName));
        return this.acpAsyncClientExt.prompt(promptRequest).block();
    }

    public void close() {
        this.acpAsyncClientExt.close();
    }

    @FunctionalInterface
    public interface SessionNotificationHandler {

        void handle(AcpSchema.SessionNotification sessionNotification);

    }

    @FunctionalInterface
    public interface ProgressNotificationHandler {

        void handle(AcpSchemaExt.ProgressNotification progressNotification);

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

    public static class ToolUpdateNotificationHandler implements AcpClientSession.NotificationHandler {

        private final ProgressNotificationHandler progressNotificationHandler;

        public ToolUpdateNotificationHandler(ProgressNotificationHandler progressNotificationHandler) {
            this.progressNotificationHandler = progressNotificationHandler;
        }

        @Override
        public Mono<Void> handle(Object notification) {
            if (log.isDebugEnabled()) {
                log.debug("{}", XDataUtils.toPrettyJSONString(notification));
            }

            AcpSchemaExt.ProgressNotification progressNotification = XDataUtils.copy(notification, AcpSchemaExt.ProgressNotification.class);
            this.progressNotificationHandler.handle(progressNotification);
            return Mono.empty();
        }
    }

    public static class SessionUpdateNotificationHandler implements AcpClientSession.NotificationHandler {

        private final List<SessionNotificationHandler> sessionNotificationHandlers;

        public SessionUpdateNotificationHandler(List<SessionNotificationHandler> sessionNotificationHandlers) {
            this.sessionNotificationHandlers = sessionNotificationHandlers;
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
            if (log.isDebugEnabled()) {
                log.debug("{}: {}", AcpSchema.METHOD_SESSION_UPDATE, XDataUtils.toPrettyJSONString(notification));
            }
            AcpSchema.SessionNotification sessionNotification = XDataUtils.copy(notification, AcpSchema.SessionNotification.class);
            this.handleSessionNotification(sessionNotification);
            return Mono.empty();
        }
    }

    public record Builder(
            List<SessionNotificationHandler> sessionNotificationHandlers,
            RequestPermissionHandler requestPermissionHandler,
            TerminalHandler terminalHandler,
            ReadTextFileHandler readTextFileHandler,
            WriteTextFileHandler writeTextFileHandler,
            Duration requestTimeout,
            Duration connectTimeout,
            String authorization,
            String agentName
    ) {

        public SimpleAsyncAcpClient build(URI uri) {
            WebSocketAcpClientTransport webSocketAcpClientTransport = new WebSocketAcpClientTransport(uri, Map.of("authorization", this.authorization))
                    .connectTimeout(this.connectTimeout);

            SessionUpdateNotificationHandler sessionUpdateNotificationHandler = new SessionUpdateNotificationHandler(
                    this.sessionNotificationHandlers);
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

            AcpAsyncClientExt acpAsyncClientExt = simpleAsyncSpec.build();

            AcpSchema.FileSystemCapability fileSystemCapability = new AcpSchema.FileSystemCapability(readTextFile, writeTextFile);
            AcpSchema.ClientCapabilities clientCapabilities = new AcpSchema.ClientCapabilities(fileSystemCapability, terminal);
            AcpSchema.InitializeRequest initializeRequest = new AcpSchema.InitializeRequest(
                    1,
                    clientCapabilities,
                    null,
                    Map.of(
                            AcpSchemaExt.AGENT_NAME, this.agentName
                    ));

            AcpSchema.InitializeResponse initializeResponse = acpAsyncClientExt.initialize(initializeRequest).block();
            log.debug("Init async acp client: {}, {}", uri, XDataUtils.toJSONString(initializeResponse));
            return new SimpleAsyncAcpClient(
                    uri,
                    this.agentName,
                    webSocketAcpClientTransport,
                    acpAsyncClientExt);
        }
    }
}
