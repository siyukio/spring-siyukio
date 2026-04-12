package io.github.siyukio.application.acp;

import com.agentclientprotocol.sdk.error.AcpProtocolException;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.tools.api.token.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
        if (req.sessionId().equals(token.id())) {
            AcpSchema.SessionModelState sessionModelState = new AcpSchema.SessionModelState("default", List.of());
            AcpSchema.SessionModeState sessionModeState = new AcpSchema.SessionModeState("default", List.of());
            return new AcpSchema.LoadSessionResponse(sessionModeState, sessionModelState);
        }
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Session not found:" + req.sessionId());
    }
}
