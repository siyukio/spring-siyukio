package io.github.siyukio.tools.acp.sdk.agent;

import com.agentclientprotocol.sdk.agent.AcpAsyncAgent;
import io.github.siyukio.tools.acp.sdk.spec.AcpSchemaExt;
import reactor.core.publisher.Mono;

/**
 *
 * @author Bugee
 */
public interface AcpAsyncAgentExt extends AcpAsyncAgent {

    Mono<Void> sendToolProgress(String toolCallId, AcpSchemaExt.ProgressNotification update);
}
