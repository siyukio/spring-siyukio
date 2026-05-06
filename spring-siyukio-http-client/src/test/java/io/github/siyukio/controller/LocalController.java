package io.github.siyukio.controller;

import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.dto.TokenResponse;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Bugee
 */
@Slf4j
@ApiController(tags = "local")
public class LocalController {

    @Autowired
    private TokenProvider tokenProvider;

    @ApiMapping(path = "/token/get")
    public TokenResponse getToken(Token token) {
        log.info("{}", XDataUtils.toPrettyJSONString(token));
        return XDataUtils.copy(token, TokenResponse.class);
    }

}
