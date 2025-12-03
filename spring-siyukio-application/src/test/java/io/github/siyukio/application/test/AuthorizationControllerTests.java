package io.github.siyukio.application.test;

import io.github.siyukio.application.dto.authorization.CreateAuthorizationRequest;
import io.github.siyukio.tools.api.ApiMock;
import io.github.siyukio.tools.api.token.Token;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
class AuthorizationControllerTests {

    @Autowired
    private ApiMock apiMock;

    @Test
    void testCreateAuthorization() {
        CreateAuthorizationRequest createAuthorizationRequest = CreateAuthorizationRequest.builder()
                .uid("123")
                .name("Bugee")
                .roles(List.of("user"))
                .build();
        JSONObject resultJson = this.apiMock.perform("/authorization/create", createAuthorizationRequest);
        log.info("{}", createAuthorizationRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testGetToken() {
        Token token = Token.builder().uid("321")
                .name("hello")
                .roles(List.of("admin"))
                .refresh(false).build();
        this.apiMock.setToken(token);

        JSONObject resultJson = this.apiMock.perform("/token/get", new JSONObject());
        log.info("{}", resultJson);
    }

    @Test
    void testGetTokenByAuthorization() {
        String authorization = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJhRHRYRHNjaFdMQXJQbU1IUVo0TDIiLCJzdWIiOiIxMjMiLCJuIjoiQnVnZWUiLCJyIjpbInVzZXIiXSwiYSI6dHJ1ZSwiaWF0IjoxNzY0NzQzNjMxLCJleHAiOjE3NjQ3NDQ1MzF9.iCImG8aOmtPDPi_ihqBifC5Fo1KsE9RoNwqIE9oviYP6g5gNvzSBBOnwfntn8UDbHpVIUPyKiLl4BDwgajmKNA";
        this.apiMock.setAuthorization(authorization);

        JSONObject resultJson = this.apiMock.perform("/token/get", new JSONObject());
        log.info("{}", resultJson);
    }
}
