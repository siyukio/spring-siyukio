package io.github.siyukio.application.acp;

import com.agentclientprotocol.sdk.error.AcpProtocolException;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.application.acp.transport.AuthSession;
import io.github.siyukio.tools.acp.AcpSchemaExt;
import io.github.siyukio.tools.acp.AcpSessionContext;
import io.github.siyukio.tools.api.token.Token;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author Bugee
 */
public interface AcpSessionHandler {

    static Mono<AuthSession> validateAndGetAuthSession(
            Map<String, Object> meta,
            Map<String, AuthSession> sessionMap) {

        Object wsSessionIdValue = meta.remove(AcpSchemaExt.WS_SESSION_ID);
        if (wsSessionIdValue == null) {
            return Mono.error(new AcpProtocolException(HttpStatus.NOT_EXTENDED.value(), "Acp auth session ID is null"));
        }

        String sessionId = wsSessionIdValue.toString();
        AuthSession authSession = sessionMap.get(sessionId);
        if (authSession == null) {
            return Mono.error(new AcpProtocolException(HttpStatus.NOT_EXTENDED.value(), "Acp auth session not found"));
        }

        return Mono.just(authSession);
    }

    /**
     * Generic method: validate session and process with custom handler
     */
    static <T> Mono<T> withAuthSession(
            Map<String, Object> meta,
            Map<String, AuthSession> sessionMap,
            Function<AuthSession, Mono<T>> handler) {

        return validateAndGetAuthSession(meta, sessionMap).flatMap(handler);
    }

    default AcpSchema.InitializeResponse handleInit(Token token, AcpSchema.InitializeRequest req) {
        return AcpSchema.InitializeResponse.ok();
    }

    default AcpSchema.NewSessionResponse handleNewSession(Token token, AcpSchema.NewSessionRequest req) {
        // TODO: Please override this method to new session
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Acp new session unsupported");
    }

    default AcpSchema.LoadSessionResponse handleLoadSession(Token token, AcpSchema.LoadSessionRequest req) {
        // TODO: Please override this method to load session
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Acp load session unsupported");
    }

    default AcpSchema.PromptResponse handlePrompt(Token token, AcpSchema.PromptRequest req, AcpSessionContext acpSessionContext) {
        acpSessionContext.sendMessage("Processing...");
        acpSessionContext.sendMessage("""
                The server has not implemented this method.
                Please implement AcpSessionHandler.handlePrompt() to process this request.
                """);

        return new AcpSchema.PromptResponse(AcpSchema.StopReason.END_TURN);
    }

    default void handleCancel(Token token, AcpSchema.CancelNotification req) {
    }

}
