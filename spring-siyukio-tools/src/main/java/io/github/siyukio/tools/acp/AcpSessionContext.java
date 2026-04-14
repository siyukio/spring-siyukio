package io.github.siyukio.tools.acp;

import com.agentclientprotocol.sdk.agent.Command;
import com.agentclientprotocol.sdk.agent.CommandResult;
import com.agentclientprotocol.sdk.agent.PromptContext;
import com.agentclientprotocol.sdk.error.AcpCapabilityException;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.Getter;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

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
        this.promptContext.sendMessage(text)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .block();
    }


    public void sendThought(String text) {
        this.promptContext.sendThought(text)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .block();
    }

    public Boolean askPermission(String action, Duration duration) {
        AcpSchema.ToolCallUpdate toolCall = new AcpSchema.ToolCallUpdate(
                IdUtils.getUniqueId(), action, AcpSchema.ToolKind.EDIT, AcpSchema.ToolCallStatus.PENDING,
                null, null, null, null);

        List<AcpSchema.PermissionOption> options = List.of(
                new AcpSchema.PermissionOption("allow", "Allow", AcpSchema.PermissionOptionKind.ALLOW_ONCE),
                new AcpSchema.PermissionOption("deny", "Deny", AcpSchema.PermissionOptionKind.REJECT_ONCE));
        AcpSchema.RequestPermissionRequest request = new AcpSchema.RequestPermissionRequest(this.getSessionId(), toolCall, options);

        AcpSchema.RequestPermissionResponse response = this.requestPermission(request, duration);
        return response.outcome() instanceof AcpSchema.PermissionSelected s
                && "allow".equals(s.optionId());
    }

    public AcpSchema.RequestPermissionResponse requestPermission(
            AcpSchema.RequestPermissionRequest request,
            Duration duration) {
        return this.promptContext.requestPermission(request)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .timeout(duration)
                .onErrorReturn(new AcpSchema.RequestPermissionResponse(new AcpSchema.PermissionCancelled()))
                .block();
    }

    public String askChoice(String question, List<String> options, Duration duration) {
        List<AcpSchema.PermissionOption> permOptions = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            permOptions.add(new AcpSchema.PermissionOption(
                    String.valueOf(i), options.get(i), AcpSchema.PermissionOptionKind.ALLOW_ONCE));
        }

        AcpSchema.ToolCallUpdate toolCall = new AcpSchema.ToolCallUpdate(
                IdUtils.getUniqueId(), question, AcpSchema.ToolKind.OTHER,
                AcpSchema.ToolCallStatus.PENDING, null, null, null, null);

        AcpSchema.RequestPermissionRequest request = new AcpSchema.RequestPermissionRequest(this.getSessionId(), toolCall, permOptions);
        AcpSchema.RequestPermissionResponse response = this.requestPermission(request, duration);

        if (response.outcome() instanceof AcpSchema.PermissionSelected s) {
            int idx = Integer.parseInt(s.optionId());
            return options.get(idx);
        }
        return "";
    }

    public CommandResult execute(Command command, Duration duration) {
        return this.promptContext.execute(command)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .timeout(duration)
                .onErrorReturn(AcpCapabilityException.class, new CommandResult("Client terminal not supported", 1))
                .onErrorReturn(TimeoutException.class, new CommandResult("Client terminal timeout", 1, true))
                .block();
    }

    public CommandResult readFile(String path, Duration duration) {
        return this.promptContext.readFile(path)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .timeout(duration)
                .map(result -> new CommandResult(result, 0))
                .onErrorReturn(AcpCapabilityException.class, new CommandResult("Client read file not supported", 1))
                .onErrorReturn(TimeoutException.class, new CommandResult("Client read file timeout", 1, true))
                .block();
    }

    public CommandResult writeFile(String path, String content, Duration duration) {
        return this.promptContext.writeFile(path, content)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .timeout(duration)
                .thenReturn(new CommandResult("Client write file completed", 0))
                .onErrorReturn(AcpCapabilityException.class, new CommandResult("Client write file not supported", 1))
                .onErrorReturn(TimeoutException.class, new CommandResult("Client write file timeout", 1, true))
                .block();
    }
}
