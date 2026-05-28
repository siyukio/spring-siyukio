package io.github.siyukio.application.controller;

import io.github.siyukio.application.dto.CreateAuthorizationRequest;
import io.github.siyukio.application.dto.CreateResponseResponse;
import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.annotation.Authorization;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Buddy
 */
@Slf4j
@ApiController(tags = "authorization")
public class AuthorizationController {

    @Autowired
    private TokenProvider tokenProvider;

    @ApiMapping(path = "/authorization/create", authorization = @Authorization(state = Authorization.State.DISABLED))
    public CreateResponseResponse createAuthorization(CreateAuthorizationRequest createAuthorizationRequest) {
        log.info("{}", XDataUtils.toPrettyJSONString(createAuthorizationRequest));
        Token token = new Token(new Token.UserPrincipal(createAuthorizationRequest.uid(), createAuthorizationRequest.name()));
        String authorization = this.tokenProvider.createAuthorization(token);
        return CreateResponseResponse.builder()
                .authorization(authorization).build();
    }

    @ApiMapping(path = "/token/get")
    public JSONObject getToken(Token token) {
        log.info("{}", XDataUtils.toPrettyJSONString(token));
        return XDataUtils.copy(token, JSONObject.class);
    }

    @ApiMapping(path = "/user/token/get", authorization = @Authorization(type = Token.PRINCIPAL_TYPE_USER))
    public JSONObject getUserToken(Token token) {
        log.info("{}", XDataUtils.toPrettyJSONString(token));
        return XDataUtils.copy(token, JSONObject.class);
    }

    @ApiMapping(path = "/app/token/get", authorization = @Authorization(type = Token.PRINCIPAL_TYPE_APP))
    public JSONObject getAppToken(Token token) {
        log.info("{}", XDataUtils.toPrettyJSONString(token));
        return XDataUtils.copy(token, JSONObject.class);
    }

    @ApiMapping(path = "/member/token/get", authorization = @Authorization(type = Token.PRINCIPAL_TYPE_MEMBER))
    public JSONObject getMemberToken(Token token) {
        log.info("{}", XDataUtils.toPrettyJSONString(token));
        return XDataUtils.copy(token, JSONObject.class);
    }

    @ApiMapping(path = "/internal/token/get", authorization = @Authorization(type = Token.PRINCIPAL_TYPE_INTERNAL))
    public JSONObject getInternalToken(Token token) {
        log.info("{}", XDataUtils.toPrettyJSONString(token));
        return XDataUtils.copy(token, JSONObject.class);
    }

}
