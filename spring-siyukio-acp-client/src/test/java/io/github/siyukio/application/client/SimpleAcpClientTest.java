package io.github.siyukio.application.client;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.application.dto.CreateAuthorizationRequest;
import io.github.siyukio.application.dto.CreateAuthorizationResponse;
import io.github.siyukio.client.SimpleAcpClient;
import io.github.siyukio.client.SimpleAsyncAcpClient;
import io.github.siyukio.tools.acp.sdk.spec.AcpSchemaExt;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @AfterAll
    static void setAfter() {
        if (SIMPLE_ACP_CLIENT != null) {
            SIMPLE_ACP_CLIENT.close();
        }
    }

    @BeforeEach
    void setUp() {
        if (SIMPLE_ACP_CLIENT == null) {
            String authorization = "eyJhbGciOiJFUzI1NiJ9.eyJ0eXAiOiJhY2MiLCJleHAiOjQ4OTAzNzI2NTIsInBybiI6eyJ1aWQiOiJ1c2VyLTAwMSIsInVubSI6InVzZXIiLCJ0eXAiOiJ1c3IifSwianRpIjoiYVdlWnB6WWZTZjVFSEpDV1B2Q2VZIn0.iBAhFSLsS0OQrh-Jauu0-NuGQhZLUuMfp1ATUHen9UPJQdvLK7xho1hr693NkZfPHnzpG5dbrex7F2bZzgCk8w";
            String serverUri = "ws://localhost:8080";
            SIMPLE_ACP_CLIENT = SimpleAcpClient.builder(serverUri)
//                    .agentName("test")
//                    .loadBalance(true)
                    .requestTimeout(Duration.ofSeconds(100))
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
                    .terminalHandler(new SimpleAsyncAcpClient.TerminalHandler() {
                        @Override
                        public SimpleAsyncAcpClient.CreateTerminalHandler createTerminalHandler() {
                            return request -> {
                                return new AcpSchema.CreateTerminalResponse(IdUtils.getUniqueId());
                            };
                        }

                        @Override
                        public SimpleAsyncAcpClient.WaitForTerminalExitHandler waitForTerminalExitHandler() {
                            return request -> {
                                return new AcpSchema.WaitForTerminalExitResponse(0, null);
                            };
                        }

                        @Override
                        public SimpleAsyncAcpClient.TerminalOutputHandler terminalOutputHandler() {
                            return request -> {
                                return new AcpSchema.TerminalOutputResponse("Darwin bogon 25.3.0 Darwin Kernel Version 25.3.0: Wed Jan 28 20:53:05 PST 2026; root:xnu-12377.81.4~5/RELEASE_ARM64_T6020 arm64", true, new AcpSchema.TerminalExitStatus(0, null));
                            };
                        }

                        @Override
                        public SimpleAsyncAcpClient.ReleaseTerminalHandler releaseTerminalHandler() {
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
                    .sessionNotificationHandler(notification -> {
                        log.debug("Client sessionNotification: {}", XDataUtils.toPrettyJSONString(notification));
                    }).build();
            AcpSchema.NewSessionResponse newSessionResponse = SIMPLE_ACP_CLIENT.newSession();
            log.info("newSessionResponse: {}", XDataUtils.toPrettyJSONString(newSessionResponse));
            SESSION_ID = newSessionResponse.sessionId();
        }
    }

    @Test
    void testListTool() {
        AcpSchemaExt.ListToolsResult listToolsResult = SIMPLE_ACP_CLIENT.listTool();
        log.info("{}", XDataUtils.toPrettyJSONString(listToolsResult));
    }

    @Test
    void testCallTool() {
        CreateAuthorizationRequest createAuthorizationRequest = CreateAuthorizationRequest.builder()
                .uid("test").name("test").scopes(List.of()).build();
        CreateAuthorizationResponse createAuthorizationResponse = SIMPLE_ACP_CLIENT.callTool(
                "/authorization/create",
                createAuthorizationRequest,
                CreateAuthorizationResponse.class);
        log.info("{}", XDataUtils.toPrettyJSONString(createAuthorizationResponse));
    }

    @Test
    void testCallToolProgress() {
        JSONObject result = SIMPLE_ACP_CLIENT.callTool(
                "toolCallProgress",
                new JSONObject(),
                JSONObject.class, (sessionUpdate) -> {
                    log.info("Tool sessionUpdate:{}", XDataUtils.toPrettyJSONString(sessionUpdate));
                });
        log.info("{}", XDataUtils.toPrettyJSONString(result));
    }

    @Test
    void testAskPermission() {
        JSONObject result = SIMPLE_ACP_CLIENT.callTool(
                "askPermission",
                JSONObject.class);
        log.info("{}", XDataUtils.toPrettyJSONString(result));
    }

    @Test
    void testAskChoice() {
        JSONObject result = SIMPLE_ACP_CLIENT.callTool(
                "askChoice",
                JSONObject.class);
        log.info("{}", XDataUtils.toPrettyJSONString(result));
    }

    @Test
    void testExecute() {
        JSONObject result = SIMPLE_ACP_CLIENT.callTool(
                "execute",
                JSONObject.class);
        log.info("{}", XDataUtils.toPrettyJSONString(result));
    }

    @Test
    void testReadFile() {
        JSONObject result = SIMPLE_ACP_CLIENT.callTool(
                "readFile",
                JSONObject.class);
        log.info("{}", XDataUtils.toPrettyJSONString(result));
    }

    @Test
    void testWriteFile() {
        JSONObject result = SIMPLE_ACP_CLIENT.callTool(
                "writeFile",
                JSONObject.class);
        log.info("{}", XDataUtils.toPrettyJSONString(result));
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
    void testPrompt2() {
        AcpSchema.PromptResponse response = SIMPLE_ACP_CLIENT.prompt(IdUtils.getUniqueId(), "hello2");
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
