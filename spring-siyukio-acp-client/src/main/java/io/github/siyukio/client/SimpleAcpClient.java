package io.github.siyukio.client;

import com.agentclientprotocol.sdk.client.AcpAsyncClient;
import com.agentclientprotocol.sdk.client.AcpClient;
import com.agentclientprotocol.sdk.spec.AcpClientSession;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.client.transport.WebSocketAcpClientTransport;
import io.github.siyukio.tools.acp.Invoke;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import io.modelcontextprotocol.util.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Bugee
 */
@Slf4j
public class SimpleAcpClient {

    private final AcpAsyncClient acpAsyncClient;

    private final String sessionId;

    private final Map<String, String> toolCallUpdateCache;

    public SimpleAcpClient(AcpAsyncClient acpAsyncClient, String sessionId, Map<String, String> toolCallUpdateCache) {
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
                return XDataUtils.parse(cacheValue, typeClass);
            } else {
                throw new ApiException("Acp callTool error:" + invoke.tool() + "," + invoke.toolCallId());
            }
        } finally {
            this.toolCallUpdateCache.remove(invoke.toolCallId());
        }
    }

    public void close() {
        this.acpAsyncClient.close();
    }

    public static class Builder {

        private final String uri;
        private final Map<String, AcpClientSession.NotificationHandler> notificationHandlers = new HashMap<>();
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

        public SimpleAcpClient build() {
            Assert.hasText(uri, "uri is required");
            WebSocketAcpClientTransport clientTransport = new WebSocketAcpClientTransport(this.uri, Map.of("authorization", this.authorization))
                    .connectTimeout(this.connectTimeout);
            Map<String, String> toolCallUpdateCache = new ConcurrentHashMap<>();
            AcpClient.AsyncSpec asyncSpec = AcpClient.async(clientTransport)
                    .requestTimeout(this.requestTimeout)
                    .notificationHandler(AcpSchema.METHOD_SESSION_UPDATE, (notification -> {
                        log.debug("notification: {}", XDataUtils.toPrettyJSONString(notification));
                        if (notification instanceof Map notificationMap) {
                            Object update = notificationMap.get("update");
                            if (update instanceof Map updateMap) {
                                String sessionUpdate = updateMap.get("sessionUpdate").toString();
                                if (sessionUpdate.equals("tool_call_update")) {
                                    AcpSchema.ToolCallUpdateNotification toolCallUpdateNotification = XDataUtils.copy(update, AcpSchema.ToolCallUpdateNotification.class);
                                    String toolCallId = toolCallUpdateNotification.toolCallId();
                                    String cacheValue = toolCallUpdateCache.get(toolCallId);
                                    if (cacheValue.equals("IN_PROGRESS")) {
                                        AcpSchema.ToolCallContent toolCallContent = toolCallUpdateNotification.content().getFirst();
                                        if (toolCallContent instanceof AcpSchema.ToolCallContentBlock block) {
                                            AcpSchema.ContentBlock contentBlock = block.content();
                                            if (contentBlock instanceof AcpSchema.Resource resource) {
                                                AcpSchema.EmbeddedResourceResource embeddedResourceResource = resource.resource();
                                                if (embeddedResourceResource instanceof AcpSchema.TextResourceContents textResourceContents) {
                                                    if (textResourceContents.mimeType().equals(MimeTypeUtils.APPLICATION_JSON_VALUE)) {
                                                        toolCallUpdateCache.put(toolCallId, textResourceContents.text());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return Mono.empty();
                    }));

            AcpAsyncClient acpAsyncClient = asyncSpec.build();
            AcpSchema.InitializeResponse initializeResponse = acpAsyncClient.initialize().block();
            log.debug("Init acp client: {}, {}", this.uri, XDataUtils.toJSONString(initializeResponse));

            AcpSchema.NewSessionRequest newSessionRequest = new AcpSchema.NewSessionRequest(this.cwd, List.of(), Map.of());
            AcpSchema.NewSessionResponse newSessionResponse = acpAsyncClient.newSession(newSessionRequest).block();
            log.debug("Get acp client session: {}, {}", this.uri, XDataUtils.toJSONString(newSessionResponse));
            assert newSessionResponse != null;
            return new SimpleAcpClient(acpAsyncClient, newSessionResponse.sessionId(), toolCallUpdateCache);
        }
    }
}
