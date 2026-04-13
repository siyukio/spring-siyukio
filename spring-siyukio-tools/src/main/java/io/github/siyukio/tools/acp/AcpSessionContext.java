package io.github.siyukio.tools.acp;

import com.agentclientprotocol.sdk.agent.PromptContext;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.Getter;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 *
 * @author Bugee
 */
public class AcpSessionContext {

    private final PromptContext promptContext;

    @Getter
    private final Invoke invoke;

    @Getter
    private final Token token;

    private final String transportId;

    public AcpSessionContext(PromptContext promptContext, Invoke invoke, Token token, String transportId) {
        this.promptContext = promptContext;
        this.invoke = invoke;
        this.token = token;
        this.transportId = transportId;
    }

    public String getSessionId() {
        return this.promptContext.getSessionId();
    }

    private AcpSchema.ToolCallUpdateNotification getToolCallUpdateNotification(String jsonText, AcpSchema.ToolCallStatus toolCallStatus) {
        AcpSchema.TextResourceContents textResourceContents = new AcpSchema.TextResourceContents(jsonText, "", MimeTypeUtils.APPLICATION_JSON_VALUE);
        AcpSchema.Resource resource = new AcpSchema.Resource("resource", textResourceContents, null, null);
        AcpSchema.ToolCallContentBlock toolCallContentBlock = new AcpSchema.ToolCallContentBlock("content", resource);
        return new AcpSchema.ToolCallUpdateNotification(
                AcpSchemaExt.TOOL_CALL_UPDATE,
                this.invoke.toolCallId(),
                this.invoke.tool(),
                AcpSchema.ToolKind.EXECUTE,
                toolCallStatus,
                List.of(toolCallContentBlock),
                List.of(),
                "",
                "",
                null);
    }

    public Mono<Void> sendToolCallCompletedAsync(String jsonText) {
        AcpSchema.ToolCallUpdateNotification toolCallUpdateNotification = this.getToolCallUpdateNotification(jsonText, AcpSchema.ToolCallStatus.COMPLETED);
        return this.promptContext.sendUpdate(this.getSessionId(), toolCallUpdateNotification)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId));
    }

    public void sendToolCallCompleted(String jsonText) {
        AcpSchema.ToolCallUpdateNotification toolCallUpdateNotification = this.getToolCallUpdateNotification(jsonText, AcpSchema.ToolCallStatus.COMPLETED);
        this.promptContext.sendUpdate(this.getSessionId(), toolCallUpdateNotification)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .block();
    }

    public void sendToolCallProgress(AcpSchemaExt.ProgressNotification progressNotification) {
        String jsonText = XDataUtils.toJSONString(progressNotification);
        AcpSchema.ToolCallUpdateNotification toolCallUpdateNotification = this.getToolCallUpdateNotification(jsonText, AcpSchema.ToolCallStatus.IN_PROGRESS);
        this.promptContext.sendUpdate(this.getSessionId(), toolCallUpdateNotification)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .block();
    }

    public void sendMessage(String text) {
        AcpSchema.AgentMessageChunk messageChunk = new AcpSchema.AgentMessageChunk(
                AcpSchemaExt.AGENT_MESSAGE_CHUNK,
                new AcpSchema.TextContent(text));
        this.promptContext.sendUpdate(this.getSessionId(), messageChunk)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .block();
    }
}
