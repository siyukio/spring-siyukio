package io.github.siyukio.application.acp;

import com.agentclientprotocol.sdk.error.AcpProtocolException;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.tools.acp.AcpSchemaExt;
import io.github.siyukio.tools.acp.AcpSessionContext;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author Bugee
 */
@Slf4j
@Service
public class AcpSessionHandlerImpl implements AcpSessionHandler {

    @Override
    public AcpSchema.NewSessionResponse handleNewSession(Token token, AcpSchema.NewSessionRequest req) {
        log.debug("AcpSchema.NewSessionRequest: {}, {}", token, req);
        AcpSchema.SessionModelState sessionModelState = new AcpSchema.SessionModelState("default", List.of());
        AcpSchema.SessionModeState sessionModeState = new AcpSchema.SessionModeState("default", List.of());
        return new AcpSchema.NewSessionResponse(token.id(), sessionModeState, sessionModelState);
    }

    @Override
    public AcpSchema.LoadSessionResponse handleLoadSession(Token token, AcpSchema.LoadSessionRequest req) {
        log.debug("AcpSchema.LoadSessionResponse: {}, {}", token, req);
        if (req.sessionId().equals(token.id())) {
            AcpSchema.SessionModelState sessionModelState = new AcpSchema.SessionModelState("default", List.of());
            AcpSchema.SessionModeState sessionModeState = new AcpSchema.SessionModeState("default", List.of());
            return new AcpSchema.LoadSessionResponse(sessionModeState, sessionModelState);
        }
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Session not found:" + req.sessionId());
    }

    @Override
    public AcpSchema.PromptResponse handlePrompt(Token token, AcpSchema.PromptRequest req, AcpSessionContext acpSessionContext) {
        acpSessionContext.sendThought("Processing..." + req.sessionId());
        AcpSchemaExt.SessionInfoUpdate sessionInfoUpdate = new AcpSchemaExt.SessionInfoUpdate(
                "session_info_update", "test", XDataUtils.format(LocalDateTime.now())
        );
        acpSessionContext.sendUpdate(sessionInfoUpdate);
//        try {
//            Thread.sleep(60000);
//        } catch (InterruptedException ignored) {
//        }
        return new AcpSchema.PromptResponse(AcpSchema.StopReason.END_TURN);
    }

    @Override
    public void handleCancel(Token token, AcpSchema.CancelNotification req) {
        log.debug("AcpSchema.CancelNotification: {}, {}", token, req);
    }

    @Override
    public AcpSchema.SetSessionModeResponse handleSetSessionMode(Token token, AcpSchema.SetSessionModeRequest req) {
        log.debug("AcpSchema.SetSessionModeRequest: {}, {}", token, req);
        return new AcpSchema.SetSessionModeResponse();
    }

    @Override
    public AcpSchema.SetSessionModelResponse handleSetSessionModel(Token token, AcpSchema.SetSessionModelRequest req) {
        log.debug("AcpSchema.SetSessionModelRequest: {}, {}", token, req);
        return new AcpSchema.SetSessionModelResponse();
    }
}
