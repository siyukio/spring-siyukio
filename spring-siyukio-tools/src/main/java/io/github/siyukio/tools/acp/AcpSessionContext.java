package io.github.siyukio.tools.acp;

import com.agentclientprotocol.sdk.agent.PromptContext;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.Getter;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

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

    public AcpSessionContext(PromptContext promptContext, Invoke invoke, Token token) {
        this.promptContext = promptContext;
        this.invoke = invoke;
        this.token = token;
    }

    public String getToolCallId() {
        return this.invoke.toolCallId();
    }

    public String getSessionId() {
        return this.promptContext.getSessionId();
    }

    private AcpSchema.ToolCallUpdateNotification getToolCallUpdateNotification(String jsonText, AcpSchema.ToolCallStatus toolCallStatus) {
        AcpSchema.TextResourceContents textResourceContents = new AcpSchema.TextResourceContents(jsonText, "", MimeTypeUtils.APPLICATION_JSON_VALUE);
        AcpSchema.Resource resource = new AcpSchema.Resource("resource", textResourceContents, null, null);
        AcpSchema.ToolCallContentBlock toolCallContentBlock = new AcpSchema.ToolCallContentBlock("content", resource);
        return new AcpSchema.ToolCallUpdateNotification(
                "tool_call_update",
                this.invoke.toolCallId(),
                this.invoke.tool(),
                AcpSchema.ToolKind.EXECUTE,
                toolCallStatus,
                List.of(toolCallContentBlock),
                List.of(),
                "",
                "",
                Map.of());
    }

    public Mono<Void> sendToolCallCompleted(String jsonText) {
        AcpSchema.ToolCallUpdateNotification toolCallUpdateNotification = this.getToolCallUpdateNotification(jsonText, AcpSchema.ToolCallStatus.COMPLETED);
        return this.promptContext.sendUpdate(this.promptContext.getSessionId(), toolCallUpdateNotification);
    }

    public Mono<Void> sendToolCallProgress(AcpSchemaExt.ProgressNotification progressNotification) {
        String jsonText = XDataUtils.toJSONString(progressNotification);
        AcpSchema.ToolCallUpdateNotification toolCallUpdateNotification = this.getToolCallUpdateNotification(jsonText, AcpSchema.ToolCallStatus.IN_PROGRESS);
        return this.promptContext.sendUpdate(this.promptContext.getSessionId(), toolCallUpdateNotification);
    }
}
