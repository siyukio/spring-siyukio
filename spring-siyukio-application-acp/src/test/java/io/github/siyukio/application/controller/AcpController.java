package io.github.siyukio.application.controller;

import com.agentclientprotocol.sdk.agent.Command;
import com.agentclientprotocol.sdk.agent.CommandResult;
import io.github.siyukio.application.dto.CreateAuthorizationRequest;
import io.github.siyukio.application.dto.CreateAuthorizationResponse;
import io.github.siyukio.application.dto.RefreshAuthorizationRequest;
import io.github.siyukio.tools.acp.AcpSchemaExt;
import io.github.siyukio.tools.acp.AcpSessionContext;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.dto.TokenResponse;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Bugee
 */
@Slf4j
@ApiController(tags = {"acp"})
public class AcpController {

    @Autowired
    private TokenProvider tokenProvider;

    @ApiMapping(path = "/authorization/create", authorization = false, acpAvailable = true,
            summary = "Retrieve JWT Token",
            description = """
                    A utility tool that authenticates with the target service and returns a valid JWT token for subsequent API requests.
                    """
    )
    public CreateAuthorizationResponse createAuthorization(CreateAuthorizationRequest createAuthorizationRequest) {
        log.info("{}", XDataUtils.toPrettyJSONString(createAuthorizationRequest));
        Token refreshToken = Token.builder()
                .uid(createAuthorizationRequest.uid()).name(createAuthorizationRequest.name()).roles(List.of()).refresh(true)
                .build();
        String refreshTokenAuth = this.tokenProvider.createAuthorization(refreshToken);

        Token accessToken = refreshToken.createAccessToken();
        String accessTokenAuth = this.tokenProvider.createAuthorization(accessToken);

        return CreateAuthorizationResponse.builder()
                .accessToken(accessTokenAuth)
                .refreshToken(refreshTokenAuth)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @ApiMapping(path = "/authorization/refreshException", authorization = false, acpAvailable = true)
    public CreateAuthorizationResponse refreshException(RefreshAuthorizationRequest refreshAuthorizationRequest) {
        throw new ApiException("Business exception");
    }

    @ApiMapping(path = "/authorization/refreshTimeout", authorization = false, acpAvailable = true)
    public CreateAuthorizationResponse refreshTimeout(RefreshAuthorizationRequest refreshAuthorizationRequest) {
        try {
            Thread.sleep(40000);
        } catch (InterruptedException ignored) {
        }
        return CreateAuthorizationResponse.builder()
                .accessToken("ok").build();
    }

    @ApiMapping(path = "/token/getByProgress", acpAvailable = true)
    public TokenResponse getTokenByProgress(Token token, AcpSessionContext acpSessionContext) {
        if (acpSessionContext != null) {
            log.info("getTokenByProgress acpSessionContext: {}", acpSessionContext.getSessionId());
            for (int i = 0; i < 3; i++) {
                JSONObject messageJson = new JSONObject();
                messageJson.put("data", i);
                AcpSchemaExt.ProgressNotification progressNotification = AcpSchemaExt.ProgressNotification.create(
                        i + 1, 3, XDataUtils.toJSONString(messageJson)
                );
                acpSessionContext.sendToolCallProgress(progressNotification);
            }
        }
        return TokenResponse.builder()
                .name("ok").build();
    }

    @ApiMapping(path = "/askPermission", acpAvailable = true)
    public TokenResponse askPermission(Token token, AcpSessionContext acpSessionContext) {
        Boolean result = acpSessionContext.askPermission("Can you do this?", Duration.ofSeconds(30));
        return TokenResponse.builder()
                .name(result.toString()).build();
    }

    @ApiMapping(path = "/askChoice", acpAvailable = true)
    public TokenResponse askChoice(Token token, AcpSessionContext acpSessionContext) {
        List<String> colors = List.of("red", "green", "blue");
        String result = acpSessionContext.askChoice("What's your favorite color?", colors, Duration.ofSeconds(30));
        return TokenResponse.builder()
                .name(result).build();
    }

    @ApiMapping(path = "/execute", acpAvailable = true)
    public TokenResponse execute(Token token, AcpSessionContext acpSessionContext) {
        Command command = Command.of("uname", "-a");
        CommandResult result = acpSessionContext.execute(command, Duration.ofSeconds(10));
        return TokenResponse.builder()
                .name(result.output()).build();
    }

    @ApiMapping(path = "/readFile", acpAvailable = true)
    public TokenResponse readFile(Token token, AcpSessionContext acpSessionContext) {
        CommandResult result = acpSessionContext.readFile("/README.md", Duration.ofSeconds(10));
        return TokenResponse.builder()
                .name(result.output()).build();
    }

    @ApiMapping(path = "/writeFile", acpAvailable = true)
    public TokenResponse writeFile(Token token, AcpSessionContext acpSessionContext) {
        CommandResult result = acpSessionContext.writeFile("/README.md", "Hello, world!", Duration.ofSeconds(10));
        return TokenResponse.builder()
                .name(result.output()).build();
    }
}
