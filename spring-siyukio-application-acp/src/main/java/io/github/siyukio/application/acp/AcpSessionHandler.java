package io.github.siyukio.application.acp;

import com.agentclientprotocol.sdk.error.AcpProtocolException;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.application.acp.transport.AuthSession;
import io.github.siyukio.tools.acp.AcpSessionContext;
import io.github.siyukio.tools.api.token.Token;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author Bugee
 */
public interface AcpSessionHandler {

    String WS_SESSION_ID = "wsSessionId";

    static Mono<AuthSession> validateAndGetSession(
            Map<String, Object> meta,
            Map<String, AuthSession> sessionMap) {

        Object wsSessionIdValue = meta.get(WS_SESSION_ID);
        if (wsSessionIdValue == null) {
            return Mono.error(new AcpProtocolException(HttpStatus.NOT_EXTENDED.value(), "Acp WebSocket Session ID is null"));
        }

        String sessionId = wsSessionIdValue.toString();
        AuthSession authSession = sessionMap.get(sessionId);
        if (authSession == null) {
            return Mono.error(new AcpProtocolException(HttpStatus.NOT_EXTENDED.value(), "Acp WebSocket Session not found"));
        }

        return Mono.just(authSession);
    }

    /**
     * Generic method: validate session and process with custom handler
     */
    static <T> Mono<T> withWebSocketSession(
            Map<String, Object> meta,
            Map<String, AuthSession> sessionMap,
            Function<AuthSession, Mono<T>> handler) {

        return validateAndGetSession(meta, sessionMap).flatMap(handler);
    }

    default AcpSchema.InitializeResponse handleInit(Token token, AcpSchema.InitializeRequest req) {
        return AcpSchema.InitializeResponse.ok();
    }

    default AcpSchema.NewSessionResponse handleNewSession(Token token, AcpSchema.NewSessionRequest req) {
        AcpSchema.SessionModelState sessionModelState = new AcpSchema.SessionModelState("default", List.of());
        AcpSchema.SessionModeState sessionModeState = new AcpSchema.SessionModeState("default", List.of());
        return new AcpSchema.NewSessionResponse(token.id(), sessionModeState, sessionModelState);
    }

    default AcpSchema.LoadSessionResponse handleLoadSession(Token token, AcpSchema.LoadSessionRequest req) {
        // TODO: Please override this method to load session from database
        if (req.sessionId().equals("aRXnwCt7KBpWA9vqZWeLn")) {
            AcpSchema.SessionModelState sessionModelState = new AcpSchema.SessionModelState("default", List.of());
            AcpSchema.SessionModeState sessionModeState = new AcpSchema.SessionModeState("default", List.of());
            return new AcpSchema.LoadSessionResponse(sessionModeState, sessionModelState);
        }
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Session not found:" + req.sessionId());
    }

    default AcpSchema.PromptResponse handlePrompt(Token token, AcpSchema.PromptRequest req, AcpSessionContext acpSessionContext) {
        acpSessionContext.sendMessage("Processing...");
        acpSessionContext.sendMessage("""
                The server has not implemented this feature yet.
                Please implement AcpSessionHandler.handlePrompt() to process this request.
                """);

        return new AcpSchema.PromptResponse(AcpSchema.StopReason.END_TURN);
    }

    default void handleCancel(Token token, AcpSchema.CancelNotification req) {
    }

}
