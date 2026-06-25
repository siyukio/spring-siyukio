package io.github.siyukio.tools.acp.sdk.agent;

import com.agentclientprotocol.sdk.agent.Command;
import com.agentclientprotocol.sdk.agent.CommandResult;
import com.agentclientprotocol.sdk.agent.PromptContext;
import com.agentclientprotocol.sdk.error.AcpCapabilityException;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.tools.acp.sdk.spec.AcpSchemaExt;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.IdUtils;
import lombok.Getter;

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

    private final ToolContext toolContext;

    @Getter
    private final Token token;

    @Getter
    private final String transportId;

    public AcpSessionContext(PromptContext promptContext, Token token, String transportId) {
        this.promptContext = promptContext;
        this.token = token;
        this.transportId = transportId;
        this.toolContext = null;
    }

    public AcpSessionContext(ToolContext toolContext, Token token, String transportId) {
        this.promptContext = null;
        this.token = token;
        this.transportId = transportId;
        this.toolContext = toolContext;
    }

    public void sendToolProgress(AcpSchemaExt.ProgressNotification progressNotification) {
        if (this.toolContext == null) {
            return;
        }
        this.toolContext.sendProgress(progressNotification)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .block();
    }

    public void sendUpdate(AcpSchema.SessionUpdate sessionUpdate) {
        if (this.promptContext == null) {
            return;
        }
        this.promptContext.sendUpdate(this.promptContext.getSessionId(), sessionUpdate)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .block();
    }

    public void sendUpdate(String sessionId, AcpSchema.SessionUpdate sessionUpdate) {
        if (this.toolContext == null) {
            return;
        }
        this.toolContext.sendUpdate(sessionId, sessionUpdate)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .block();
    }

    public void sendMessage(String text) {
        if (this.promptContext == null) {
            return;
        }
        this.promptContext.sendMessage(text)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .block();
    }


    public void sendThought(String text) {
        if (this.promptContext == null) {
            return;
        }
        this.promptContext.sendThought(text)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .block();
    }

    public Boolean askPermission(String action, Duration duration) {
        if (this.promptContext == null) {
            return false;
        }
        AcpSchema.ToolCallUpdate toolCall = new AcpSchema.ToolCallUpdate(
                IdUtils.getUniqueId(), action, AcpSchema.ToolKind.EDIT, AcpSchema.ToolCallStatus.PENDING,
                null, null, null, null);

        List<AcpSchema.PermissionOption> options = List.of(
                new AcpSchema.PermissionOption("allow", "Allow", AcpSchema.PermissionOptionKind.ALLOW_ONCE),
                new AcpSchema.PermissionOption("deny", "Deny", AcpSchema.PermissionOptionKind.REJECT_ONCE));
        AcpSchema.RequestPermissionRequest request = new AcpSchema.RequestPermissionRequest(this.promptContext.getSessionId(), toolCall, options);

        AcpSchema.RequestPermissionResponse response = this.requestPermission(request, duration);
        return response.outcome() instanceof AcpSchema.PermissionSelected s
                && "allow".equals(s.optionId());
    }

    public AcpSchema.RequestPermissionResponse requestPermission(
            AcpSchema.RequestPermissionRequest request,
            Duration duration) {
        if (this.promptContext == null) {
            return new AcpSchema.RequestPermissionResponse(new AcpSchema.PermissionCancelled());
        }
        return this.promptContext.requestPermission(request)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .timeout(duration)
                .onErrorReturn(new AcpSchema.RequestPermissionResponse(new AcpSchema.PermissionCancelled()))
                .block();
    }

    public String askChoice(String question, List<String> options, Duration duration) {
        if (this.promptContext == null) {
            return "";
        }
        List<AcpSchema.PermissionOption> permOptions = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            permOptions.add(new AcpSchema.PermissionOption(
                    String.valueOf(i), options.get(i), AcpSchema.PermissionOptionKind.ALLOW_ONCE));
        }

        AcpSchema.ToolCallUpdate toolCall = new AcpSchema.ToolCallUpdate(
                IdUtils.getUniqueId(), question, AcpSchema.ToolKind.OTHER,
                AcpSchema.ToolCallStatus.PENDING, null, null, null, null);

        AcpSchema.RequestPermissionRequest request = new AcpSchema.RequestPermissionRequest(this.promptContext.getSessionId(), toolCall, permOptions);
        AcpSchema.RequestPermissionResponse response = this.requestPermission(request, duration);

        if (response.outcome() instanceof AcpSchema.PermissionSelected s) {
            int idx = Integer.parseInt(s.optionId());
            return options.get(idx);
        }
        return "";
    }

    public CommandResult execute(Command command, Duration duration) {
        if (this.promptContext == null) {
            return new CommandResult("Client terminal not supported", 1);
        }
        return this.promptContext.execute(command)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .timeout(duration)
                .onErrorReturn(AcpCapabilityException.class, new CommandResult("Client terminal not supported", 1))
                .onErrorReturn(TimeoutException.class, new CommandResult("Client terminal timeout", 1, true))
                .block();
    }

    public CommandResult readFile(String path, Duration duration) {
        if (this.promptContext == null) {
            return new CommandResult("Client read file not supported", 1);
        }
        return this.promptContext.readFile(path)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .timeout(duration)
                .map(result -> new CommandResult(result, 0))
                .onErrorReturn(AcpCapabilityException.class, new CommandResult("Client read file not supported", 1))
                .onErrorReturn(TimeoutException.class, new CommandResult("Client read file timeout", 1, true))
                .block();
    }

    public CommandResult writeFile(String path, String content, Duration duration) {
        if (this.promptContext == null) {
            return new CommandResult("Client write file not supported", 1);
        }
        return this.promptContext.writeFile(path, content)
                .contextWrite(ctx -> ctx.put(AcpSchemaExt.TRANSPORT_ID, transportId))
                .timeout(duration)
                .thenReturn(new CommandResult("Client write file completed", 0))
                .onErrorReturn(AcpCapabilityException.class, new CommandResult("Client write file not supported", 1))
                .onErrorReturn(TimeoutException.class, new CommandResult("Client write file timeout", 1, true))
                .block();
    }
}
