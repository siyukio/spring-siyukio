package io.github.siyukio.client;

import com.agentclientprotocol.sdk.client.AcpAsyncClient;
import com.agentclientprotocol.sdk.client.SimpleAsyncSpec;
import com.agentclientprotocol.sdk.spec.AcpClientSession;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.client.transport.WebSocketAcpClientTransport;
import io.github.siyukio.tools.acp.AcpSchemaExt;
import io.github.siyukio.tools.acp.Invoke;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import io.modelcontextprotocol.util.Assert;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Bugee
 */
@Slf4j
public class SimpleAcpClient {

    private final static String IN_PROGRESS = "in_progress";

    private final AcpAsyncClient acpAsyncClient;
    private final Map<String, String> toolCallUpdateCache;

    @Getter
    private final String callToolSessionId;

    private SimpleAcpClient(AcpAsyncClient acpAsyncClient,
                            Map<String, String> toolCallUpdateCache,
                            String callToolSessionId) {
        this.acpAsyncClient = acpAsyncClient;
        this.toolCallUpdateCache = toolCallUpdateCache;
        this.callToolSessionId = callToolSessionId;
    }

    public static Builder builder(String uri) {
        return new Builder(uri);
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
            String cacheValue = this.toolCallUpdateCache.get(invoke.toolCallId());
            if (!cacheValue.equals(IN_PROGRESS)) {
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
            this.toolCallUpdateCache.remove(invoke.toolCallId());
        }

        throw new ApiException("CallTool no response:" + invoke.tool() + "," + invoke.toolCallId());
    }

    public <T> T callTool(String tool, Class<T> typeClass) {
        return this.callTool(tool, new JSONObject(), typeClass);
    }

    public void callTool(String tool) {
        this.callTool(tool, new JSONObject(), Void.class);
    }

    public void callTool(String tool, Object params) {
        this.callTool(tool, params, Void.class);
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

    public void setSessionMode(String sessionId, String modeId) {
        AcpSchema.SetSessionModeRequest setModeRequest = new AcpSchema.SetSessionModeRequest(sessionId, modeId);
        this.acpAsyncClient.setSessionMode(setModeRequest).block();
    }

    public void setSessionModel(String sessionId, String modelId) {
        AcpSchema.SetSessionModelRequest setModelRequest = new AcpSchema.SetSessionModelRequest(sessionId, modelId);
        this.acpAsyncClient.setSessionModel(setModelRequest).block();
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

    public static class Builder {

        private final String uri;
        private final List<ProgressNotificationHandler> progressNotificationHandlers = new ArrayList<>();
        private final List<SessionNotificationHandler> sessionNotificationHandlers = new ArrayList<>();
        private Duration requestTimeout = Duration.ofSeconds(60);
        private Duration connectTimeout = Duration.ofSeconds(12);
        private String authorization = "";

        public Builder(String uri) {
            this.uri = uri;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder authorization(String authorization) {
            this.authorization = authorization;
            return this;
        }

        public Builder progressNotificationHandler(ProgressNotificationHandler progressNotificationHandler) {
            this.progressNotificationHandlers.add(progressNotificationHandler);
            return this;
        }

        public Builder sessionNotificationHandler(SessionNotificationHandler sessionNotificationHandler) {
            this.sessionNotificationHandlers.add(sessionNotificationHandler);
            return this;
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

        public SimpleAcpClient build() {
            Assert.hasText(uri, "uri is required");
            WebSocketAcpClientTransport clientTransport = new WebSocketAcpClientTransport(this.uri, Map.of("authorization", this.authorization))
                    .connectTimeout(this.connectTimeout);
            Map<String, String> toolCallUpdateCache = new ConcurrentHashMap<>();
            SimpleAsyncSpec simpleAsyncSpec = new SimpleAsyncSpec(clientTransport)
                    .requestTimeout(this.requestTimeout)
                    .notificationHandler(AcpSchema.METHOD_SESSION_UPDATE, (notification -> {
                        log.debug("{}: {}", AcpSchema.METHOD_SESSION_UPDATE, XDataUtils.toPrettyJSONString(notification));
                        AcpSchema.SessionNotification sessionNotification = XDataUtils.copy(notification, AcpSchema.SessionNotification.class);
                        AcpSchema.SessionUpdate sessionUpdate = sessionNotification.update();
                        if (sessionUpdate instanceof AcpSchema.ToolCallUpdateNotification toolCallUpdateNotification) {
                            String toolCallId = toolCallUpdateNotification.toolCallId();
                            String cacheValue = toolCallUpdateCache.get(toolCallId);
                            if (cacheValue.equals(IN_PROGRESS)) {
                                if (toolCallUpdateNotification.status().equals(AcpSchema.ToolCallStatus.COMPLETED)) {
                                    String jsonText = this.readJsonText(toolCallUpdateNotification);
                                    toolCallUpdateCache.put(toolCallId, jsonText);
                                } else if (toolCallUpdateNotification.status().equals(AcpSchema.ToolCallStatus.IN_PROGRESS)) {
                                    String jsonText = this.readJsonText(toolCallUpdateNotification);
                                    AcpSchemaExt.ProgressNotification progressNotification = XDataUtils.parse(jsonText, AcpSchemaExt.ProgressNotification.class);
                                    AcpSchemaExt.ProgressNotification completeProgressNotification = progressNotification.withToolCallId(toolCallId);
                                    this.progressNotificationHandlers.forEach(handler -> {
                                        try {
                                            handler.handle(completeProgressNotification);
                                        } catch (Exception ignored) {
                                        }
                                    });
                                }
                            } else {
                                this.sessionNotificationHandlers.forEach(handler -> {
                                    try {
                                        handler.handle(sessionNotification);
                                    } catch (Exception ignored) {
                                    }
                                });
                            }
                        } else {
                            this.sessionNotificationHandlers.forEach(handler -> {
                                try {
                                    handler.handle(sessionNotification);
                                } catch (Exception ignored) {
                                }
                            });
                        }
                        return Mono.empty();
                    }));

            AcpAsyncClient acpAsyncClient = simpleAsyncSpec.build();
            AcpSchema.InitializeResponse initializeResponse = acpAsyncClient.initialize().block();
            log.debug("Init acp client: {}, {}", this.uri, XDataUtils.toJSONString(initializeResponse));
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
            return new SimpleAcpClient(acpAsyncClient, toolCallUpdateCache, callToolSessionId);
        }
    }
}
