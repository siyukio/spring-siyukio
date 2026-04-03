package io.github.siyukio.application.client;

import io.github.siyukio.application.dto.CreateAuthorizationRequest;
import io.github.siyukio.application.dto.CreateAuthorizationResponse;
import io.github.siyukio.client.SimpleAcpClient;
import io.github.siyukio.tools.api.dto.TokenResponse;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 *
 * @author Bugee
 */

@Slf4j
@SpringBootTest
public class SimpleAcpClientTest {

    private static SimpleAcpClient simpleAcpClient = null;

    @Autowired
    private TokenProvider tokenProvider;

    @AfterAll
    static void setAfter() {
        if (simpleAcpClient != null) {
            simpleAcpClient.close();
        }
    }

    @BeforeEach
    void setUp() {
        if (simpleAcpClient == null) {
            String authorization = this.tokenProvider.createAuthorization(Token.builder().uid("321").build());
            String serverUri = "ws://localhost:8080";
            simpleAcpClient = SimpleAcpClient.builder(serverUri)
                    .authorization(authorization)
                    .progressNotificationHandler((notification) -> {
                        log.debug("ProgressNotification: {}", XDataUtils.toPrettyJSONString(notification));
                    }).build();
        }
    }

    @Test
    void testAcpAsyncClient() {
        CreateAuthorizationRequest createAuthorizationRequest = CreateAuthorizationRequest.builder()
                .uid("test").name("test").roles(List.of()).build();
        CreateAuthorizationResponse createAuthorizationResponse = simpleAcpClient.callTool(
                "authorization.create",
                createAuthorizationRequest,
                CreateAuthorizationResponse.class);
        log.info("{}", XDataUtils.toPrettyJSONString(createAuthorizationResponse));
    }

    @Test
    void testAsyncNotification() {
        TokenResponse tokenResponse = simpleAcpClient.callTool(
                "token.getByProgress",
                TokenResponse.class);
        log.info("{}", XDataUtils.toPrettyJSONString(tokenResponse));
    }

}
