package io.github.siyukio.application.client;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.application.dto.CreateAuthorizationRequest;
import io.github.siyukio.application.dto.CreateAuthorizationResponse;
import io.github.siyukio.client.SimpleAcpClient;
import io.github.siyukio.tools.acp.AcpSchemaExt;
import io.github.siyukio.tools.api.dto.TokenResponse;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;

/**
 *
 * @author Bugee
 */

@Slf4j
@SpringBootTest
public class SimpleAcpClientTest {

    private static SimpleAcpClient SIMPLE_ACP_CLIENT = null;

    private static String SESSION_ID = null;

    @Autowired
    private TokenProvider tokenProvider;

    @AfterAll
    static void setAfter() {
        if (SIMPLE_ACP_CLIENT != null) {
            SIMPLE_ACP_CLIENT.close();
        }
    }

    @BeforeEach
    void setUp() {
        if (SIMPLE_ACP_CLIENT == null) {
            String authorization = this.tokenProvider.createAuthorization(Token.builder()
                    .uid("siyukio").name("siyukio").build());
            log.debug("authorization: {}", authorization);
            String serverUri = "ws://localhost:8080";
            SIMPLE_ACP_CLIENT = SimpleAcpClient.builder(serverUri)
//                    .loadBalance(true)
                    .requestTimeout(Duration.ofSeconds(60))
                    .authorization(authorization)
                    .writeTextFileHandler(request -> {
                        log.debug("WriteTextFileRequest: {}", request);
                        return new AcpSchema.WriteTextFileResponse();
                    })
                    .readTextFileHandler(request -> {
                        log.debug("ReadTextFileRequest: {}", request);
                        try {
                            Thread.sleep(15000);
                        } catch (InterruptedException ignored) {
                        }
                        return new AcpSchema.ReadTextFileResponse("Hello, world!");
                    })
                    .terminalHandler(new SimpleAcpClient.TerminalHandler() {
                        @Override
                        public SimpleAcpClient.CreateTerminalHandler createTerminalHandler() {
                            return request -> {
                                return new AcpSchema.CreateTerminalResponse(IdUtils.getUniqueId());
                            };
                        }

                        @Override
                        public SimpleAcpClient.WaitForTerminalExitHandler waitForTerminalExitHandler() {
                            return request -> {
                                return new AcpSchema.WaitForTerminalExitResponse(0, null);
                            };
                        }

                        @Override
                        public SimpleAcpClient.TerminalOutputHandler terminalOutputHandler() {
                            return request -> {
                                return new AcpSchema.TerminalOutputResponse("Darwin bogon 25.3.0 Darwin Kernel Version 25.3.0: Wed Jan 28 20:53:05 PST 2026; root:xnu-12377.81.4~5/RELEASE_ARM64_T6020 arm64", true, new AcpSchema.TerminalExitStatus(0, null));
                            };
                        }

                        @Override
                        public SimpleAcpClient.ReleaseTerminalHandler releaseTerminalHandler() {
                            return request -> {
                                return new AcpSchema.ReleaseTerminalResponse();
                            };
                        }
                    })
                    .requestPermissionHandler(request -> {
                        log.debug("RequestPermission: {}", XDataUtils.toPrettyJSONString(request));
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ignored) {
                        }
                        return new AcpSchema.RequestPermissionResponse(new AcpSchema.PermissionCancelled());
                    })
                    .progressNotificationHandler(notification -> {
                        log.debug("ProgressNotification: {}", XDataUtils.toPrettyJSONString(notification));
                    })
                    .sessionNotificationHandler(notification -> {
                        log.debug("SessionNotification: {}", XDataUtils.toPrettyJSONString(notification));
                    }).build();
            AcpSchema.NewSessionResponse newSessionResponse = SIMPLE_ACP_CLIENT.newSession();
            log.info("newSessionResponse: {}", XDataUtils.toPrettyJSONString(newSessionResponse));
            SESSION_ID = newSessionResponse.sessionId();
        }
    }

    @Test
    void testListTools() {
        AcpSchemaExt.ListToolsResult listToolsResult = SIMPLE_ACP_CLIENT.listTools();
        log.info("{}", XDataUtils.toPrettyJSONString(listToolsResult));
    }

    @Test
    void testToolCall() {
        CreateAuthorizationRequest createAuthorizationRequest = CreateAuthorizationRequest.builder()
                .uid("test").name("test").roles(List.of()).build();
        CreateAuthorizationResponse createAuthorizationResponse = SIMPLE_ACP_CLIENT.callTool(
                "authorization.create",
                createAuthorizationRequest,
                CreateAuthorizationResponse.class);
        log.info("{}", XDataUtils.toPrettyJSONString(createAuthorizationResponse));
    }

    @Test
    void testToolCallProgress() {
        TokenResponse tokenResponse = SIMPLE_ACP_CLIENT.callTool(
                "toolCallProgress",
                TokenResponse.class);
        log.info("{}", XDataUtils.toPrettyJSONString(tokenResponse));
    }

    @Test
    void testAskPermission() {
        TokenResponse response = SIMPLE_ACP_CLIENT.callTool(
                "askPermission",
                TokenResponse.class);
        log.info("{}", XDataUtils.toPrettyJSONString(response));
    }

    @Test
    void testAskChoice() {
        TokenResponse response = SIMPLE_ACP_CLIENT.callTool(
                "askChoice",
                TokenResponse.class);
        log.info("{}", XDataUtils.toPrettyJSONString(response));
    }

    @Test
    void testExecute() {
        TokenResponse response = SIMPLE_ACP_CLIENT.callTool(
                "execute",
                TokenResponse.class);
        log.info("{}", XDataUtils.toPrettyJSONString(response));
    }

    @Test
    void testReadFile() {
        TokenResponse response = SIMPLE_ACP_CLIENT.callTool(
                "readFile",
                TokenResponse.class);
        log.info("{}", XDataUtils.toPrettyJSONString(response));
    }

    @Test
    void testWriteFile() {
        TokenResponse response = SIMPLE_ACP_CLIENT.callTool(
                "writeFile",
                TokenResponse.class);
        log.info("{}", XDataUtils.toPrettyJSONString(response));
    }

    @Test
    void testLoadSession() {
        String sessionId = "";
        AcpSchema.LoadSessionResponse loadSessionResponse = SIMPLE_ACP_CLIENT.loadSession(sessionId);
        log.info("{}", XDataUtils.toPrettyJSONString(loadSessionResponse));
    }

    @Test
    void testCancel() {
        String sessionId = "";
        SIMPLE_ACP_CLIENT.cancel(sessionId);
    }

    @Test
    void testPrompt() {
        AcpSchema.PromptResponse response = SIMPLE_ACP_CLIENT.prompt(SESSION_ID, "hello");
        log.info("{}", XDataUtils.toPrettyJSONString(response));
    }

    @Test
    void testSetSessionMode() {
        AcpSchema.SetSessionModeResponse response = SIMPLE_ACP_CLIENT.setSessionMode(SESSION_ID, "cli");
        log.info("{}", XDataUtils.toPrettyJSONString(response));
    }

    @Test
    void testSetSessionModel() {
        AcpSchema.SetSessionModelResponse response = SIMPLE_ACP_CLIENT.setSessionModel(SESSION_ID, "gpt-5.4");
        log.info("{}", XDataUtils.toPrettyJSONString(response));
    }

}
