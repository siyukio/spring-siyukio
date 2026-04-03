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
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
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

    private final AcpAsyncClient acpAsyncClient;

    private final String sessionId;

    private final Map<String, String> toolCallUpdateCache;

    private SimpleAcpClient(AcpAsyncClient acpAsyncClient,
                            String sessionId,
                            Map<String, String> toolCallUpdateCache) {
        this.acpAsyncClient = acpAsyncClient;
        this.sessionId = sessionId;
        this.toolCallUpdateCache = toolCallUpdateCache;
    }

    public static Builder builder(String uri) {
        return new Builder(uri);
    }

    public <T> T callTool(String tool, Object params, Class<T> typeClass) {
        Invoke invoke = Invoke.create(tool, params);
        String toolCallText = WebSocketAcpClientTransport.createAcpToolCall(invoke);
        List<AcpSchema.ContentBlock> prompts = new ArrayList<>();
        prompts.add(new AcpSchema.TextContent(toolCallText));

        this.toolCallUpdateCache.put(invoke.toolCallId(), "IN_PROGRESS");
        try {
            AcpSchema.PromptRequest promptRequest = new AcpSchema.PromptRequest(this.sessionId, prompts);
            AcpSchema.PromptResponse promptResponse = this.acpAsyncClient.prompt(promptRequest).block();
            log.debug("{}", XDataUtils.toPrettyJSONString(promptResponse));
            String cacheValue = this.toolCallUpdateCache.get(invoke.toolCallId());
            if (!cacheValue.equals("IN_PROGRESS")) {
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

    public void close() {
        this.acpAsyncClient.close();
    }

    @FunctionalInterface
    public interface ProgressNotificationHandler {

        void handle(AcpSchemaExt.ProgressNotification progressNotification);

    }

    public static class Builder {

        private final String uri;
        private final List<ProgressNotificationHandler> progressNotificationHandlers = new ArrayList<>();
        private Duration requestTimeout = Duration.ofSeconds(60);
        private Duration connectTimeout = Duration.ofSeconds(12);
        private String authorization = "";
        private String cwd = "/" + IdUtils.getUniqueId();

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

        public Builder cwd(String cwd) {
            this.cwd = cwd;
            return this;
        }

        public Builder progressNotificationHandler(ProgressNotificationHandler progressNotificationHandler) {
            this.progressNotificationHandlers.add(progressNotificationHandler);
            return this;
        }

        public String readJsonText(AcpSchema.ToolCallUpdateNotification toolCallUpdateNotification) {
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
                        if (notification instanceof Map notificationMap) {
                            Object update = notificationMap.get("update");
                            if (update instanceof Map updateMap) {
                                String sessionUpdate = updateMap.get("sessionUpdate").toString();
                                if (sessionUpdate.equals("tool_call_update")) {
                                    AcpSchema.ToolCallUpdateNotification toolCallUpdateNotification = XDataUtils.copy(update, AcpSchema.ToolCallUpdateNotification.class);
                                    String toolCallId = toolCallUpdateNotification.toolCallId();
                                    String cacheValue = toolCallUpdateCache.get(toolCallId);
                                    if (cacheValue.equals("IN_PROGRESS")) {
                                        if (toolCallUpdateNotification.status().equals(AcpSchema.ToolCallStatus.COMPLETED)) {
                                            String jsonText = this.readJsonText(toolCallUpdateNotification);
                                            toolCallUpdateCache.put(toolCallId, jsonText);
                                        } else if (toolCallUpdateNotification.status().equals(AcpSchema.ToolCallStatus.IN_PROGRESS)) {
                                            String jsonText = this.readJsonText(toolCallUpdateNotification);
                                            AcpSchemaExt.ProgressNotification progressNotification = XDataUtils.parse(jsonText, AcpSchemaExt.ProgressNotification.class);
                                            this.progressNotificationHandlers.forEach(handler -> {
                                                try {
                                                    handler.handle(progressNotification);
                                                } catch (Exception ignored) {
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        }
                        return Mono.empty();
                    }));

            AcpAsyncClient acpAsyncClient = simpleAsyncSpec.build();
            AcpSchema.InitializeResponse initializeResponse = acpAsyncClient.initialize().block();
            log.debug("Init acp client: {}, {}", this.uri, XDataUtils.toJSONString(initializeResponse));

            AcpSchema.NewSessionRequest newSessionRequest = new AcpSchema.NewSessionRequest(this.cwd, List.of(), Map.of());
            AcpSchema.NewSessionResponse newSessionResponse = acpAsyncClient.newSession(newSessionRequest).block();
            log.debug("Get acp client session: {}, {}", this.uri, XDataUtils.toJSONString(newSessionResponse));
            assert newSessionResponse != null;
            return new SimpleAcpClient(acpAsyncClient,
                    newSessionResponse.sessionId(),
                    toolCallUpdateCache);
        }
    }
}
