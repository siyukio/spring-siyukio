package io.github.siyukio.application.test;

import io.github.siyukio.application.model.authorization.CreateAuthorizationRequest;
import io.github.siyukio.client.MyMcpSyncClient;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@SpringBootTest
class McpStreamableClientTests {

    @Autowired
    private TokenProvider tokenProvider;
    private final Supplier<String> tokenSupplier = () -> {
        Token token = Token.builder().uid("321")
                .name("hello")
                .roles(List.of("admin"))
                .refresh(false).build();
        return this.tokenProvider.createAuthorization(token);
    };

    @Autowired
    private McpClientCommonProperties mcpClientCommonProperties;

    @Test
    void testInit() {
        MyMcpSyncClient client = MyMcpSyncClient.builder()
                .useTokenSupplier(this.tokenSupplier)
                .setMcpClientCommonProperties(this.mcpClientCommonProperties)
                .build();
        client.getMcpSyncClient().initialize().block();
    }

    @Test
    void testListTools() {
        MyMcpSyncClient client = MyMcpSyncClient.builder()
                .useTokenSupplier(this.tokenSupplier)
                .setMcpClientCommonProperties(this.mcpClientCommonProperties)
                .build();

        McpSchema.ListToolsResult toolsList = client.listTools();

        for (McpSchema.Tool tool : toolsList.tools()) {
            log.info("{}", JsonUtils.toPrettyJSONString(tool));
        }
    }

    @Test
    void testCallTool() {
        MyMcpSyncClient client = MyMcpSyncClient.builder()
                .useTokenSupplier(this.tokenSupplier)
                .setMcpClientCommonProperties(this.mcpClientCommonProperties)
                .build();

        CreateAuthorizationRequest createAuthorizationRequest = CreateAuthorizationRequest.builder()
                .uid("123")
                .name("Buddy")
                .roles(List.of("user"))
                .build();

        JSONObject result = client.callTool("/createAuthorization",
                createAuthorizationRequest, JSONObject.class);

        log.info("{}", JsonUtils.toPrettyJSONString(result));
    }

    @Test
    void testCallToolOnSampling() throws InterruptedException {
        // Configure sampling handler
        Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler = request -> {
            // Sampling implementation that interfaces with LLM
            log.info("sampling CreateMessageRequest: {}", request);
            return McpSchema.CreateMessageResult.builder()
                    .role(McpSchema.Role.USER)
                    .message("sse ok")
                    .build();
        };

        MyMcpSyncClient client = MyMcpSyncClient.builder()
                .useTokenSupplier(this.tokenSupplier)
                .setMcpClientCommonProperties(this.mcpClientCommonProperties)
                .setSamplingHandler(samplingHandler)
                .build();

        JSONObject result = client.callTool("/getToken", JSONObject.class);

        log.info("{}", JsonUtils.toPrettyJSONString(result));
    }

    @Test
    void testCallToolOnProgress() throws InterruptedException {

        Consumer<McpSchema.ProgressNotification> progressHandler = progressNotification -> {
            log.info("progressNotification:{}", JsonUtils.toPrettyJSONString(progressNotification));
        };

        MyMcpSyncClient client = MyMcpSyncClient.builder()
                .useTokenSupplier(this.tokenSupplier)
                .setMcpClientCommonProperties(this.mcpClientCommonProperties)
                .setProgressHandler(progressHandler)
                .build();

        JSONObject result = client.callTool("/getTokenByProgress", JSONObject.class);

        log.info("{}", JsonUtils.toPrettyJSONString(result));
    }

}
