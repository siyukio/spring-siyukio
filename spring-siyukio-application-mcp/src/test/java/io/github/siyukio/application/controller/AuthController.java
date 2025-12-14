package io.github.siyukio.application.controller;

import io.github.siyukio.application.dto.*;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.IdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@ApiController(summary = "User authentication")
public class AuthController {

    @Autowired
    private TokenProvider tokenProvider;

    @ApiMapping(path = "/auth/register", summary = "Register a new user", authorization = false)
    public RegisterResponse register(RegisterRequest req) {
        String id = IdUtils.getUniqueId();
        return RegisterResponse.builder()
                .userId(id)
                .nickname(req.nickname())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @ApiMapping(path = "/auth/login", summary = "User login", authorization = false)
    public AuthResponse login(LoginRequest req) {
        String id = IdUtils.getUniqueId();
        Token refreshToken = Token.builder()
                .uid(id).name(req.username()).roles(List.of()).refresh(true)
                .build();
        String refreshTokenAuth = this.tokenProvider.createAuthorization(refreshToken);

        Token accessToken = refreshToken.createAccessToken();
        String accessTokenAuth = this.tokenProvider.createAuthorization(accessToken);

        return AuthResponse.builder()
                .userId(id)
                .nickname(req.username())
                .accessToken(accessTokenAuth)
                .refreshToken(refreshTokenAuth)
                .build();
    }

    @ApiMapping(path = "/auth/refresh", summary = "Generate new accessToken from refreshToken", authorization = false)
    public AuthResponse refresh(RefreshTokenRequest req) {
        Token refreshToken = this.tokenProvider.verifyToken(req.refreshToken());
        if (refreshToken == null || !refreshToken.refresh()) {
            throw new ApiException("Invalid refresh token");
        }
        Token accessToken = refreshToken.createAccessToken();
        String authorization = this.tokenProvider.createAuthorization(accessToken);
        return AuthResponse.builder()
                .accessToken(authorization)
                .build();
    }

}
