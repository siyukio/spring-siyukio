package io.github.siyukio.application.acp;

import com.agentclientprotocol.sdk.agent.transport.AuthSession;
import com.agentclientprotocol.sdk.error.AcpProtocolException;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.tools.acp.AcpSchemaExt;
import io.github.siyukio.tools.acp.AcpSessionContext;
import io.github.siyukio.tools.api.token.Token;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author Bugee
 */
public interface AcpSessionHandler {

    static <T> Mono<T> withContext(
            Map<String, AuthSession> sessionMap,
            Function<AuthSession, Mono<T>> handler) {

        return Mono.deferContextual(ctx -> {
            String transportId = ctx.get(AcpSchemaExt.TRANSPORT_ID);
            if (!StringUtils.hasText(transportId)) {
                return Mono.error(new AcpProtocolException(HttpStatus.NOT_EXTENDED.value(), "Acp transport ID is null"));
            }
            AuthSession authSession = sessionMap.get(transportId);
            if (authSession == null) {
                return Mono.empty();
            }
            return handler.apply(authSession);
        });
    }

    default AcpSchema.InitializeResponse handleInit(Token token, AcpSchema.InitializeRequest req) {
        return AcpSchema.InitializeResponse.ok();
    }

    default AcpSchema.NewSessionResponse handleNewSession(Token token, AcpSchema.NewSessionRequest req) {
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Acp new session unsupported");
    }

    default AcpSchema.LoadSessionResponse handleLoadSession(Token token, AcpSchema.LoadSessionRequest req) {
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Acp load session unsupported");
    }

    default AcpSchema.PromptResponse handlePrompt(Token token, AcpSchema.PromptRequest req, AcpSessionContext acpSessionContext) {
        acpSessionContext.sendThought("Processing...");
        acpSessionContext.sendMessage("""
                The server has not implemented this method.
                Please implement AcpSessionHandler.handlePrompt() to process this request.
                """);

        return new AcpSchema.PromptResponse(AcpSchema.StopReason.END_TURN);
    }

    default void handleCancel(Token token, AcpSchema.CancelNotification req) {
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Acp cancel unsupported");
    }

    default AcpSchema.SetSessionModeResponse handleSetSessionMode(Token token, AcpSchema.SetSessionModeRequest req) {
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Acp set session mode unsupported");
    }

    default AcpSchema.SetSessionModelResponse handleSetSessionModel(Token token, AcpSchema.SetSessionModelRequest req) {
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Acp set session model unsupported");
    }

}
