package io.github.siyukio.application.acp;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.tools.acp.annotation.Agent;
import io.github.siyukio.tools.acp.sdk.agent.AcpSessionContext;
import io.github.siyukio.tools.acp.sdk.spec.AcpSchemaExt;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author Bugee
 */
@Slf4j
@Agent("test")
public class TestAgentAcpSessionHandler implements AcpSessionHandler {

    @Override
    public AcpSchema.InitializeResponse handleInit(Token token, AcpSchema.InitializeRequest req) {
        log.debug("Test agent AcpSchema.InitializeRequest: {}, {}", token, req);
        return AcpSchema.InitializeResponse.ok();
    }

    @Override
    public AcpSchema.NewSessionResponse handleNewSession(Token token, AcpSchema.NewSessionRequest req) {
        log.debug("Test agent AcpSchema.NewSessionRequest: {}, {}", token, req);
        AcpSchema.SessionModelState sessionModelState = new AcpSchema.SessionModelState("default", List.of());
        AcpSchema.SessionModeState sessionModeState = new AcpSchema.SessionModeState("default", List.of());
        return new AcpSchema.NewSessionResponse(token.jwtId(), sessionModeState, sessionModelState);
    }

    @Override
    public AcpSchema.PromptResponse handlePrompt(Token token, AcpSchema.PromptRequest req, AcpSessionContext acpSessionContext) {
        acpSessionContext.sendThought("Processing test agent..." + req.sessionId());
        AcpSchemaExt.SessionInfoUpdate sessionInfoUpdate = new AcpSchemaExt.SessionInfoUpdate(
                "session_info_update", "Test agent", XDataUtils.format(LocalDateTime.now())
        );
        acpSessionContext.sendUpdate(sessionInfoUpdate);
        return new AcpSchema.PromptResponse(AcpSchema.StopReason.END_TURN);
    }
}
