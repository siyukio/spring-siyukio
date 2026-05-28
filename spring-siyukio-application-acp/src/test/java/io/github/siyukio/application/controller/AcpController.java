package io.github.siyukio.application.controller;

import com.agentclientprotocol.sdk.agent.Command;
import com.agentclientprotocol.sdk.agent.CommandResult;
import io.github.siyukio.application.dto.CreateAuthorizationRequest;
import io.github.siyukio.application.dto.CreateAuthorizationResponse;
import io.github.siyukio.tools.acp.sdk.agent.AcpSessionContext;
import io.github.siyukio.tools.acp.sdk.spec.AcpSchemaExt;
import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.annotation.Authorization;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Bugee
 */
@Slf4j
@ApiController(tags = {"acp"})
public class AcpController {

    @Autowired
    private TokenProvider tokenProvider;

    @ApiMapping(path = "/authorization/create", authorization = @Authorization(state = Authorization.State.DISABLED), acpAvailable = true,
            summary = "Retrieve JWT Token",
            description = """
                    A utility tool that authenticates with the target service and returns a valid JWT token for subsequent API requests.
                    """
    )
    public CreateAuthorizationResponse createAuthorization(Token token, CreateAuthorizationRequest createAuthorizationRequest) {
        log.info("{}", token);
        log.info("{}", XDataUtils.toPrettyJSONString(createAuthorizationRequest));
        Token refreshToken = new Token(
                new Token.UserPrincipal(createAuthorizationRequest.uid(), createAuthorizationRequest.name()),
                Token.Type.REFRESH);
        String refreshTokenAuth = this.tokenProvider.createAuthorization(refreshToken);

        Token accessToken = refreshToken.createAccessToken();
        String accessTokenAuth = this.tokenProvider.createAuthorization(accessToken);

        return CreateAuthorizationResponse.builder()
                .accessToken(accessTokenAuth)
                .refreshToken(refreshTokenAuth)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @ApiMapping(path = "/toolCallProgress", acpAvailable = true)
    public JSONObject toolCallProgress(Token token, AcpSessionContext acpSessionContext) {
        if (acpSessionContext != null) {
            for (int i = 0; i < 3; i++) {
                JSONObject messageJson = new JSONObject();
                messageJson.put("data", i);
                AcpSchemaExt.ProgressNotification progressNotification = new AcpSchemaExt.ProgressNotification(
                        i + 1, 3, XDataUtils.toJSONString(messageJson)
                );
                acpSessionContext.sendToolProgress(progressNotification);
            }
        }
        return XDataUtils.copy(token, JSONObject.class);
    }

    @ApiMapping(path = "/askPermission", acpAvailable = true)
    public JSONObject askPermission(Token token, AcpSessionContext acpSessionContext) {
        Boolean result = acpSessionContext.askPermission("Can you do this?", Duration.ofSeconds(30));
        return new JSONObject(Map.of("result", result));
    }

    @ApiMapping(path = "/askChoice", acpAvailable = true)
    public JSONObject askChoice(Token token, AcpSessionContext acpSessionContext) {
        List<String> colors = List.of("red", "green", "blue");
        String result = acpSessionContext.askChoice("What's your favorite color?", colors, Duration.ofSeconds(30));
        return new JSONObject(Map.of("result", result));
    }

    @ApiMapping(path = "/execute", acpAvailable = true)
    public JSONObject execute(Token token, AcpSessionContext acpSessionContext) {
        Command command = Command.of("uname", "-a");
        CommandResult result = acpSessionContext.execute(command, Duration.ofSeconds(10));
        return XDataUtils.copy(result, JSONObject.class);
    }

    @ApiMapping(path = "/readFile", acpAvailable = true)
    public JSONObject readFile(Token token, AcpSessionContext acpSessionContext) {
        CommandResult result = acpSessionContext.readFile("/README.md", Duration.ofSeconds(10));
        return XDataUtils.copy(result, JSONObject.class);
    }

    @ApiMapping(path = "/writeFile", acpAvailable = true)
    public JSONObject writeFile(Token token, AcpSessionContext acpSessionContext) {
        CommandResult result = acpSessionContext.writeFile("/README.md", "Hello, world!", Duration.ofSeconds(10));
        return XDataUtils.copy(result, JSONObject.class);
    }
}
