package io.github.siyukio.application.controller;

import io.github.siyukio.application.dto.authorization.CreateAuthorizationRequest;
import io.github.siyukio.application.dto.authorization.CreateResponseResponse;
import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.model.TokenResponse;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.XDataUtils;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Bugee
 */
@Slf4j
@ApiController(tags = {"mcp"})
public class McpController {

    @Autowired
    private TokenProvider tokenProvider;

    @ApiMapping(path = "/authorization/create", authorization = false, mcpTool = true,
            summary = "Retrieve JWT Token",
            description = """
                    A utility tool that authenticates with the target service and returns a valid JWT token for subsequent API requests.
                    """
    )
    public CreateResponseResponse createAuthorization(CreateAuthorizationRequest createAuthorizationRequest) {
        log.info("{}", XDataUtils.toPrettyJSONString(createAuthorizationRequest));
        Token token = Token.builder().uid(createAuthorizationRequest.uid())
                .name(createAuthorizationRequest.name())
                .roles(createAuthorizationRequest.roles())
                .refresh(false).build();
        String authorization = this.tokenProvider.createAuthorization(token);
        return CreateResponseResponse.builder()
                .authorization(authorization)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @ApiMapping(path = "/mock/simulateRandomResponse", authorization = false, mcpTool = true)
    public CreateResponseResponse simulateRandomResponse(CreateAuthorizationRequest createAuthorizationRequest) {
        log.info("{},{}", createAuthorizationRequest.uid(), "start");
        Token token = Token.builder().uid(createAuthorizationRequest.uid())
                .name(createAuthorizationRequest.name())
                .roles(createAuthorizationRequest.roles())
                .refresh(false).build();
        String authorization = this.tokenProvider.createAuthorization(token);

        long sleepTime = ThreadLocalRandom.current().nextInt(12000) + 1000;
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {
        }
        log.info("{},{}", createAuthorizationRequest.uid(), "finished");

        return CreateResponseResponse.builder()
                .authorization(authorization).build();
    }

    @ApiMapping(path = "/token/get", mcpTool = true)
    public TokenResponse getToken(Token token, McpSyncServerExchange exchange) {
        if (exchange != null) {
            log.info("getToken exchange: {}", exchange.getClientInfo());
            Map<String, Object> metadata = XDataUtils.copy(token, Map.class, String.class, Object.class);
            McpSchema.CreateMessageRequest request = McpSchema.CreateMessageRequest.builder()
                    .metadata(metadata)
                    .messages(List.of())
                    .build();

            McpSchema.CreateMessageResult result = exchange.createMessage(request);
            log.info("server CreateMessageResult: {}", XDataUtils.toPrettyJSONString(result));
//
//            result = exchange.createMessage(request);
//            log.info("server CreateMessageResult: {}", JsonUtils.toPrettyJSONString(result));
        }
        return TokenResponse.builder().build();
    }

    @ApiMapping(path = "/token/getByProgress", mcpTool = true)
    public TokenResponse getTokenByProgress(Token token, McpSyncServerExchange exchange) {
        if (exchange != null) {
            log.info("getTokenByProgress exchange: {}", exchange.getClientInfo());

            JSONObject messageJson = new JSONObject();
            messageJson.put("data", "hello");
            McpSchema.ProgressNotification progressNotification = new McpSchema.ProgressNotification(
                    "",
                    0d, 0d,
                    XDataUtils.toJSONString(messageJson)
            );
            exchange.progressNotification(progressNotification);

        }
        return TokenResponse.builder().build();
    }
}
