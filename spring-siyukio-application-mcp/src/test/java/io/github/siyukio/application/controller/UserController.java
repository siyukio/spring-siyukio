package io.github.siyukio.application.controller;

import io.github.siyukio.application.dto.UserResponse;
import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.token.Token;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ApiController(summary = "User authentication")
public class UserController {

    @ApiMapping(path = "/user/me", summary = "Get current user by accessToken")
    public UserResponse me(Token token) {
        return UserResponse.builder()
                .id(token.uid())
                .nickname(token.name())
                .roles(List.of())
                .build();
    }

}
