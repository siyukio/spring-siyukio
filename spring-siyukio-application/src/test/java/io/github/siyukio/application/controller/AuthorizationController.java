package io.github.siyukio.application.controller;

import io.github.siyukio.application.model.authorization.CreateAuthorizationRequest;
import io.github.siyukio.application.model.authorization.CreateResponseResponse;
import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.model.TokenResponse;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Buddy
 */
@Slf4j
@ApiController(tags = "authorization")
public class AuthorizationController {

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

    @ApiMapping(path = "/getToken")
    public TokenResponse getToken(Token token) {
        log.info("{}", JsonUtils.toPrettyJSONString(token));
        return JsonUtils.copy(token, TokenResponse.class);
    }

}
