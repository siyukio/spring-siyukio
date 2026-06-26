package io.github.siyukio.tools.acp.sdk.agent;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import reactor.core.publisher.Mono;

/**
 *
 * @author Bugee
 */
public class ToolContext {

    private final AcpAsyncAgentExt agent;

    private final String toolCallId;

    public ToolContext(AcpAsyncAgentExt agent, String toolCallId) {
        this.agent = agent;
        this.toolCallId = toolCallId;
    }

    public Mono<Void> sendUpdate(AcpSchema.SessionUpdate update) {
        return this.agent.sendToolUpdate(this.toolCallId, update);
    }

    public Mono<Void> sendUpdate(String sessionId, AcpSchema.SessionUpdate update) {
        return this.agent.sendSessionUpdate(sessionId, update);
    }
}
