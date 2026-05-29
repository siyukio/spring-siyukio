package io.github.siyukio.application.test;

import io.github.siyukio.application.dto.CreateAuthorizationRequest;
import io.github.siyukio.tools.api.ApiMock;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.IdUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;

@Slf4j
@SpringBootTest
class AuthorizationControllerTests {

    @Autowired
    private ApiMock apiMock;

    @Autowired
    private TokenProvider tokenProvider;

    @Test
    void testCreateToken() {
        {
            Token token = new Token(new Token.InternalPrincipal());
            String auth = this.tokenProvider.createAuthorization(token, Duration.ofDays(360 * 100));
            log.info("Internal: {}", auth);
        }

        {
            Token token = new Token(new Token.MemberPrincipal("member-001", "member"));
            String auth = this.tokenProvider.createAuthorization(token, Duration.ofDays(360 * 100));
            log.info("Member: {}", auth);
        }

        {
            Token token = new Token(new Token.UserPrincipal("user-001", "user"));
            String auth = this.tokenProvider.createAuthorization(token, Duration.ofDays(360 * 100));
            log.info("User: {}", auth);
        }

        {
            Token token = new Token(new Token.AdminUserPrincipal("admin-user-001", "admin"));
            String auth = this.tokenProvider.createAuthorization(token, Duration.ofDays(360 * 100));
            log.info("Admin user: {}", auth);
        }

        {
            Token token = new Token(new Token.AppPrincipal("app-001", "app"));
            String auth = this.tokenProvider.createAuthorization(token, Duration.ofDays(360 * 100));
            log.info("App: {}", auth);
        }

        {
            Token token = new Token(
                    new Token.AppPrincipal("app-001", "app"),
                    new Token.MemberPrincipal("member-001", "member")
            );
            String auth = this.tokenProvider.createAuthorization(token, Duration.ofDays(360 * 100));
            log.info("App with member: {}", auth);
        }
    }

    @Test
    void testTokenProvider() {
        Token token = new Token(new Token.UserPrincipal("123", "Bugee"));
        String auth = this.tokenProvider.createAuthorization(token);
        log.info("{}", auth);
        token = this.tokenProvider.verifyToken(auth);
        log.info("{}", token);
    }

    @Test
    void testCreateAuthorization() {
        CreateAuthorizationRequest createAuthorizationRequest = CreateAuthorizationRequest.builder()
                .uid("123")
                .name("Bugee")
                .scopes(List.of("user"))
                .build();
        JSONObject resultJson = this.apiMock.perform("/authorization/create", createAuthorizationRequest);
        log.info("{}", createAuthorizationRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testGetToken() {
        Token token = new Token(new Token.UserPrincipal(
                IdUtils.getUniqueId(), "test", List.of("admin")
        ));
        this.apiMock.setToken(token);

        JSONObject resultJson = this.apiMock.perform("/token/get", new JSONObject());
        log.info("{}", resultJson);
    }

    @Test
    void testGetUserTokenByAuthorization() {
        String authorization;
        // User
        authorization = "eyJhbGciOiJFUzI1NiJ9.eyJ0eXAiOiJhY2MiLCJleHAiOjQ4OTAzNzI2NTIsInBybiI6eyJ1aWQiOiJ1c2VyLTAwMSIsInVubSI6InVzZXIiLCJ0eXAiOiJ1c3IifSwianRpIjoiYVdlWnB6WWZTZjVFSEpDV1B2Q2VZIn0.iBAhFSLsS0OQrh-Jauu0-NuGQhZLUuMfp1ATUHen9UPJQdvLK7xho1hr693NkZfPHnzpG5dbrex7F2bZzgCk8w";
        this.apiMock.setAuthorization(authorization);
        this.apiMock.perform("/user/token/get", new JSONObject());

    }

    @Test
    void testGetAdminUserTokenByAuthorization() {
        String authorization;
        // Admin user
        authorization = "eyJhbGciOiJFUzI1NiJ9.eyJ0eXAiOiJhY2MiLCJleHAiOjQ4OTA0MTg3OTYsInBybiI6eyJ1aWQiOiJhZG1pbi11c2VyLTAwMSIsInVubSI6ImFkbWluIHVzZXIiLCJ0eXAiOiJhZG0ifSwianRpIjoiYVdoQ1RKVE5nRTdIMjg0UHJhVnloIn0.P--7CDC1mtBaHBMP8HEmD8dTcp_TihnxFxFenpch1snJRu2-BA6CbqlU05mhLuoqLXkrbnAvRRhROHBZwx00Mw";
        this.apiMock.setAuthorization(authorization);
        this.apiMock.perform("/admin/token/get", new JSONObject());

    }

    @Test
    void testGetAppTokenByAuthorization() {
        String authorization;
        // App
        authorization = "eyJhbGciOiJFUzI1NiJ9.eyJ0eXAiOiJhY2MiLCJleHAiOjQ4OTAzNzI2NTIsInBybiI6eyJhbm0iOiJhcHAiLCJ0eXAiOiJhcHAiLCJhaWQiOiJhcHAtMDAxIn0sImp0aSI6ImFXZVpwemJHWUtQRXlaeGpLdzhYQiJ9.UolaS4w5Kp4ak9rd2Fuw-ZkNAnivC2bGs1RvEFAMfPQXYPjuj3Ym7wt5T2YO-SjToxXO3oWsXfjEJeh2T4ncEw";
        this.apiMock.setAuthorization(authorization);
        this.apiMock.perform("/app/token/get", new JSONObject());

        // App with member
        authorization = "eyJhbGciOiJFUzI1NiJ9.eyJ0eXAiOiJhY2MiLCJhY3QiOnsibWlkIjoibWVtYmVyLTAwMSIsInR5cCI6Im1iciIsIm1ubSI6Im1lbWJlciJ9LCJleHAiOjQ4OTAzNzI2NTIsInBybiI6eyJhbm0iOiJhcHAiLCJ0eXAiOiJhcHAiLCJhaWQiOiJhcHAtMDAxIn0sImp0aSI6ImFXZVpwemJxV1hQMW03VjF3cXpNayJ9.M1shd35YtVrKtCu7XyRWXX4a5i7bXV64idFi2bfuAqWjHbh9tAtOBveJx6ZGRPwt4QbtPDIW2ggEnaTYY7__Cw";
        this.apiMock.setAuthorization(authorization);
        this.apiMock.perform("/app/token/get", new JSONObject());
    }

    @Test
    void testGetMemberTokenByAuthorization() {
        String authorization;
        // Member
        authorization = "eyJhbGciOiJFUzI1NiJ9.eyJ0eXAiOiJhY2MiLCJleHAiOjQ4OTAzNzI2NTIsInBybiI6eyJtaWQiOiJtZW1iZXItMDAxIiwidHlwIjoibWJyIiwibW5tIjoibWVtYmVyIn0sImp0aSI6ImFXZVpwelhkbjJTYjFqdFFoWWZMQSJ9.lHQp77oZ6KEGfE-oDlaFCJxmRVuAR6yfxYwXq0wx9FsAam4h1BA4J7i1qx22BUz3LUTA2q0c1r7EQxHopXqfXg";
        this.apiMock.setAuthorization(authorization);
        this.apiMock.perform("/member/token/get", new JSONObject());
    }

    @Test
    void testGetInternalTokenByAuthorization() {
        String authorization;
        // Internal
        authorization = "eyJhbGciOiJFUzI1NiJ9.eyJ0eXAiOiJhY2MiLCJleHAiOjQ4OTAzNzQ3NDQsInBybiI6eyJ0eXAiOiJpbnQifSwianRpIjoiYVdlZ01xZTJzSzRnVnpjelp4UXdSIn0.VVtYKuPwwRdZBg7Z8NBKeMs85a7ZHJmDgCgI-x_O061uhA_SBfBU8CZfHWCVpCSM70HA8vOYFOsy8evzhPUlWA";
        this.apiMock.setAuthorization(authorization);
        this.apiMock.perform("/internal/token/get", new JSONObject());
    }
}
