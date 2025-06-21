package io.github.siyukio.application.test;

import io.github.siyukio.application.model.authorization.CreateAuthorizationRequest;
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
                .name("Buddy")
                .roles(List.of("user"))
                .build();
        JSONObject resultJson = this.apiMock.perform("/createAuthorization", createAuthorizationRequest);
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

        JSONObject resultJson = this.apiMock.perform("/getToken", new JSONObject());
        log.info("{}", resultJson);
    }

    @Test
    void testGetTokenByAuthorization() {
        String authorization = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ7XCJ1aWRcIjpcIjEyM1wiLFwibmFtZVwiOlwiQnVkZHlcIixcInJvbGVzXCI6W1widXNlclwiXSxcInJlZnJlc2hcIjpmYWxzZSxcImV4cGlyZWRcIjpmYWxzZX0iLCJleHAiOjE3NDg4NjM0ODd9.OqqyhuryLrrf7k8zNLvH3I-OnArtk3kSRS0CdXhIz376qlXlqVpz2lDfL_x6TyUKzCDauRPrmd42nDyN5wqjLD2Ud_VJBGIPTGFeZl3XqRVn-DDTnbJdST6J4yhTuAXJBrYwqd1f-XEvxA8hlU1vsUGaFGqM4uTMGl5EKfJ_DYm-UWvASYykoGMnN7rtJ2hVyc9hXqyqpDynAN3QVY3SjBx5ItRBrJO3awokpJAMNYiVAYUWjZHg623BaHFeqgAb28H_Jk9wUHh7kk0w-RsMuAihLxg5cAJhqWxrZD7riGIKKGz6Lk9aFuTUc1rcoVibzzDs8I7gaEKZzk7EevK90w";
        this.apiMock.setAuthorization(authorization);

        JSONObject resultJson = this.apiMock.perform("/getToken", new JSONObject());
        log.info("{}", resultJson);
    }
}
