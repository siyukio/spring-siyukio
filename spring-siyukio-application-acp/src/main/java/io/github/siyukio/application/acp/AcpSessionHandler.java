package io.github.siyukio.application.acp;

import com.agentclientprotocol.sdk.agent.transport.AuthSession;
import com.agentclientprotocol.sdk.error.AcpProtocolException;
import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.tools.acp.sdk.agent.AcpSessionContext;
import io.github.siyukio.tools.acp.sdk.spec.AcpSchemaExt;
import io.github.siyukio.tools.api.token.Token;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

/**
 * Handler interface for managing ACP (Agent Client Protocol) session lifecycle events.
 * Implement this interface and register it as a Spring bean to customize session behavior.
 *
 * @author Bugee
 */
public interface AcpSessionHandler {

    /**
     * Executes the given handler within the context of an authenticated ACP session.
     * Retrieves the AuthSession from the session map using the transport ID from the Reactor context.
     *
     * @param sessionMap the map of transport ID to AuthSession
     * @param handler    the function to apply with the resolved AuthSession
     * @param <T>        the type of the result returned by the handler
     * @return a Mono emitting the handler result, or an error if the transport ID is invalid or session not found
     */
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

    /**
     * Handles the initialization request when a client first connects to the ACP server.
     * Override this method to perform custom initialization logic (e.g., validation, logging).
     *
     * @param token the authentication token of the client
     * @param req   the initialization request from the client
     * @return an InitializeResponse indicating success or failure
     */
    default AcpSchema.InitializeResponse handleInit(Token token, AcpSchema.InitializeRequest req) {
        return AcpSchema.InitializeResponse.ok();
    }

    /**
     * Handles the creation of a new ACP session.
     * Override this method to implement custom session creation logic.
     *
     * @param token the authentication token of the client
     * @param req   the new session request from the client
     * @return a NewSessionResponse containing the new session information
     * @throws AcpProtocolException if new session is not supported (default behavior)
     */
    default AcpSchema.NewSessionResponse handleNewSession(Token token, AcpSchema.NewSessionRequest req) {
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Acp new session unsupported");
    }

    /**
     * Handles loading an existing ACP session.
     * Override this method to implement custom session loading logic.
     *
     * @param token the authentication token of the client
     * @param req   the load session request from the client
     * @return a LoadSessionResponse containing the loaded session information
     * @throws AcpProtocolException if load session is not supported (default behavior)
     */
    default AcpSchema.LoadSessionResponse handleLoadSession(Token token, AcpSchema.LoadSessionRequest req) {
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Acp load session unsupported");
    }

    /**
     * Handles a prompt request from the client and generates a response.
     * This is the core method for processing client messages in an ACP session.
     * Override this method to implement custom prompt processing logic.
     *
     * @param token             the authentication token of the client
     * @param req               the prompt request from the client
     * @param acpSessionContext the session context for sending thoughts and messages
     * @return a PromptResponse indicating the stop reason and any additional data
     */
    default AcpSchema.PromptResponse handlePrompt(Token token, AcpSchema.PromptRequest req, AcpSessionContext acpSessionContext) {
        acpSessionContext.sendThought("Processing...");
        acpSessionContext.sendMessage("""
                The server has not implemented this method.
                Please implement AcpSessionHandler.handlePrompt() to process this request.
                """);

        return new AcpSchema.PromptResponse(AcpSchema.StopReason.END_TURN);
    }

    /**
     * Handles a cancel notification from the client to cancel an ongoing request.
     * Override this method to implement custom cancellation logic.
     *
     * @param token the authentication token of the client
     * @param req   the cancel notification from the client
     * @throws AcpProtocolException if cancel is not supported (default behavior)
     */
    default void handleCancel(Token token, AcpSchema.CancelNotification req) {
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Acp cancel unsupported");
    }

    /**
     * Handles setting the session mode for an ACP session.
     * Override this method to implement custom session mode handling.
     *
     * @param token the authentication token of the client
     * @param req   the set session mode request from the client
     * @return a SetSessionModeResponse indicating the result
     * @throws AcpProtocolException if set session mode is not supported (default behavior)
     */
    default AcpSchema.SetSessionModeResponse handleSetSessionMode(Token token, AcpSchema.SetSessionModeRequest req) {
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Acp set session mode unsupported");
    }

    /**
     * Handles setting the session model for an ACP session.
     * Override this method to implement custom session model handling.
     *
     * @param token the authentication token of the client
     * @param req   the set session model request from the client
     * @return a SetSessionModelResponse indicating the result
     * @throws AcpProtocolException if set session model is not supported (default behavior)
     */
    default AcpSchema.SetSessionModelResponse handleSetSessionModel(Token token, AcpSchema.SetSessionModelRequest req) {
        throw new AcpProtocolException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Acp set session model unsupported");
    }

}
