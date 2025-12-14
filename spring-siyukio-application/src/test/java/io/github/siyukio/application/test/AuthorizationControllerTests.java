package io.github.siyukio.application.test;

import io.github.siyukio.application.dto.CreateAuthorizationRequest;
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
        // plainText
        String authorization = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiIxMjMiLCJzIjpbInVzZXIiXSwiZSI6IkJ1Z2VlIiwiaCI6ZmFsc2UsImV4cCI6MTc2NDc1MTU3MywiaWF0IjoxNzY0NzUwNjczLCJqdGkiOiJhRHR4SjgzWmVHRmd2VnhmQ0t2WnAifQ.mI_oR9Q7ktVQGlZugneLtDvQ3t3JnJETvmBrOG_R_hzFavLJdkQEf65vpn2zVvu6ALnPyvciORCUCcbZFGrteQ";

        // cipherText
        authorization = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.NFc3WlhrWXVLMkhFUllIenc2MTQrTURDZEQ1MzRIRFVrL1d2MFFJWmRpdTVyOGxJSGVCRXI3WHA4WmxDQ1E3QU40SjRnK2tzVmFaZHVTMzNrVVpmTEhpVHpHdGFkS2w4WHlURHROdWV6TngxRmlvWG1UL3lDQ3lEM2hhMTQzRVZ4ZDRiVmxSV2p4aFhYMHZlVmhXOFNQT3c5ZnVHV0w1YmdmY0g.Pp_3u-H68zmRMYkd79jxQLASgeitZ4Z6b8FePA96NE0MAKdNuZPBg5bqWhklLQUG_d2YQglsjIvSHsSbHIb8PA";
        this.apiMock.setAuthorization(authorization);

        JSONObject resultJson = this.apiMock.perform("/token/get", new JSONObject());
        log.info("{}", resultJson);
    }
}
