package io.github.siyukio.application.controller;

import io.github.siyukio.application.model.authorization.CreateAuthorizationRequest;
import io.github.siyukio.application.model.authorization.CreateResponseResponse;
import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.model.TokenResponse;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.XDataUtils;
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

    @ApiMapping(path = "/authorization/create", authorization = false)
    public CreateResponseResponse createAuthorization(CreateAuthorizationRequest createAuthorizationRequest) {
        log.info("{}", XDataUtils.toPrettyJSONString(createAuthorizationRequest));
        Token token = Token.builder().uid(createAuthorizationRequest.uid())
                .name(createAuthorizationRequest.name())
                .roles(createAuthorizationRequest.roles())
                .refresh(false).build();
        String authorization = this.tokenProvider.createAuthorization(token);
        return CreateResponseResponse.builder()
                .authorization(authorization).build();
    }

    @ApiMapping(path = "/token/get")
    public TokenResponse getToken(Token token) {
        log.info("{}", XDataUtils.toPrettyJSONString(token));
        return XDataUtils.copy(token, TokenResponse.class);
    }

}
