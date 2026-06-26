package io.github.siyukio.tools.acp.sdk.agent;

import com.agentclientprotocol.sdk.agent.AcpAsyncAgent;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import reactor.core.publisher.Mono;

/**
 *
 * @author Bugee
 */
public interface AcpAsyncAgentExt extends AcpAsyncAgent {

    Mono<Void> sendToolUpdate(String toolCallId, AcpSchema.SessionUpdate update);
}
