package io.github.siyukio.application.controller;

import io.github.siyukio.application.dto.CreateAuthorizationRequest;
import io.github.siyukio.application.dto.CreateAuthorizationResponse;
import io.github.siyukio.application.dto.RefreshAuthorizationRequest;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

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

    @ApiMapping(path = "/authorization/refresh", authorization = false, acpAvailable = true)
    public CreateAuthorizationResponse refreshAuthorization(RefreshAuthorizationRequest refreshAuthorizationRequest) {
        Token refreshToken = this.tokenProvider.verifyToken(refreshAuthorizationRequest.refreshToken());
        if (refreshToken == null || !refreshToken.refresh()) {
            throw new ApiException("Invalid refresh token");
        }
        Token accessToken = refreshToken.createAccessToken();
        String authorization = this.tokenProvider.createAuthorization(accessToken);
        return CreateAuthorizationResponse.builder()
                .accessToken(authorization)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
