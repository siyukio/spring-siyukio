package io.github.siyukio.application.acp;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.tools.api.token.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 *
 * @author Bugee
 */
@Slf4j
@Service
public class AcpSessionHandlerImpl implements AcpSessionHandler {

    @Override
    public AcpSchema.InitializeResponse handleInit(Token token, AcpSchema.InitializeRequest req) {
        log.debug("AcpSchema.InitializeRequest: {}, {}", token, req);
        return AcpSessionHandler.super.handleInit(token, req);
    }

    @Override
    public AcpSchema.NewSessionResponse handleNewSession(Token token, AcpSchema.NewSessionRequest req) {
        log.debug("AcpSchema.NewSessionResponse: {}, {}", token, req);
        return AcpSessionHandler.super.handleNewSession(token, req);
    }
}
