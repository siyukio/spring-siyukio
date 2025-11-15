package io.github.siyukio.application.controller;

import io.github.siyukio.application.model.authorization.CreateAuthorizationRequest;
import io.github.siyukio.application.model.authorization.CreateResponseResponse;
import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.model.TokenResponse;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

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

    @ApiMapping(path = "/createAuthorization", authorization = false)
    public CreateResponseResponse createAuthorization(CreateAuthorizationRequest createAuthorizationRequest) {
        log.info("{}", JsonUtils.toPrettyJSONString(createAuthorizationRequest));
        CreateResponseResponse createResponseResponse = new CreateResponseResponse();
        Token token = Token.builder().uid(createAuthorizationRequest.uid)
                .name(createAuthorizationRequest.name)
                .roles(createAuthorizationRequest.roles)
                .refresh(false).build();
        createResponseResponse.authorization = this.tokenProvider.createAuthorization(token);
        return createResponseResponse;
    }

    @ApiMapping(path = "/mockRandomResponse", authorization = false)
    public CreateResponseResponse mockRandomResponse(CreateAuthorizationRequest createAuthorizationRequest) {
        log.info("{},{}", createAuthorizationRequest.uid, "start");
        CreateResponseResponse createResponseResponse = new CreateResponseResponse();
        Token token = Token.builder().uid(createAuthorizationRequest.uid)
                .name(createAuthorizationRequest.name)
                .roles(createAuthorizationRequest.roles)
                .refresh(false).build();
        createResponseResponse.authorization = this.tokenProvider.createAuthorization(token);

        long sleepTime = ThreadLocalRandom.current().nextInt(12000) + 1000;
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {
        }
        log.info("{},{}", createAuthorizationRequest.uid, "finished");
        return createResponseResponse;
    }

    @ApiMapping(path = "/getToken")
    public TokenResponse getToken(Token token, McpSyncServerExchange exchange) {
        if (exchange != null) {
            log.info("getToken exchange: {}", exchange.getClientInfo());
            Map<String, Object> metadata = JsonUtils.copy(token, Map.class, String.class, Object.class);
            McpSchema.CreateMessageRequest request = McpSchema.CreateMessageRequest.builder()
                    .metadata(metadata)
                    .messages(List.of())
                    .build();

            McpSchema.CreateMessageResult result = exchange.createMessage(request);
            log.info("server CreateMessageResult: {}", JsonUtils.toPrettyJSONString(result));
//
//            result = exchange.createMessage(request);
//            log.info("server CreateMessageResult: {}", JsonUtils.toPrettyJSONString(result));
        }
        return new TokenResponse();
    }

    @ApiMapping(path = "/getTokenByProgress")
    public TokenResponse getTokenByProgress(Token token, McpSyncServerExchange exchange) {
        if (exchange != null) {
            log.info("getTokenByProgress exchange: {}", exchange.getClientInfo());

            JSONObject messageJson = new JSONObject();
            messageJson.put("data", "hello");
            McpSchema.ProgressNotification progressNotification = new McpSchema.ProgressNotification(
                    "",
                    0d, 0d,
                    JsonUtils.toJSONString(messageJson)
            );
            exchange.progressNotification(progressNotification);

        }
        return new TokenResponse();
    }
}
