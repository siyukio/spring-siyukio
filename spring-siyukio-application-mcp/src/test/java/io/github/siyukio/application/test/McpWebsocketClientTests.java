package io.github.siyukio.application.test;

import io.github.siyukio.application.model.authorization.CreateAuthorizationRequest;
import io.github.siyukio.client.MyMcpSyncClient;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.MyMcpSchema;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@SpringBootTest
class McpWebsocketClientTests {

    private final String baseUri = "http://localhost:8080";

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
    void testListTools() {
        MyMcpSyncClient client = MyMcpSyncClient.builder(this.baseUri)
                .setWebsocket(true)
                .setTokenSupplier(this.tokenSupplier)
                .setMcpClientCommonProperties(this.mcpClientCommonProperties)
                .build();

        McpSchema.ListToolsResult toolsList = client.listTools();

        for (McpSchema.Tool tool : toolsList.tools()) {
            log.info("{}", JsonUtils.toPrettyJSONString(tool));
        }

        client.closeGracefully();
    }

    @Test
    void testGetOutputSchema() {

        MyMcpSyncClient client = MyMcpSyncClient.builder(this.baseUri)
                .setWebsocket(true)
                .setTokenSupplier(this.tokenSupplier)
                .setMcpClientCommonProperties(this.mcpClientCommonProperties)
                .build();

        JSONObject result = client.callTool("/getToken",
                Map.of("_outputSchema", "true"), JSONObject.class);

        log.info("{}", JsonUtils.toPrettyJSONString(result));

        client.closeGracefully();
    }

    @Test
    void testCallTool() {
        MyMcpSyncClient client = MyMcpSyncClient.builder(this.baseUri)
                .setWebsocket(true)
                .setTokenSupplier(this.tokenSupplier)
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

        client.closeGracefully();
    }

    @Test
    void testCallToolOnSampling() throws InterruptedException {
        // Configure sampling handler
        Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler = request -> {
            // Sampling implementation that interfaces with LLM
            log.info("client: {}", request);
            return McpSchema.CreateMessageResult.builder()
                    .role(McpSchema.Role.USER)
                    .message("ok")
                    .build();
        };

        MyMcpSyncClient client = MyMcpSyncClient.builder(this.baseUri)
                .setWebsocket(true)
                .setTokenSupplier(this.tokenSupplier)
                .setMcpClientCommonProperties(this.mcpClientCommonProperties)
                .setSamplingHandler(samplingHandler)
                .build();

        JSONObject result = client.callTool("/getToken", JSONObject.class);

        log.info("{}", JsonUtils.toPrettyJSONString(result));

        Thread.sleep(12000);
        client.closeGracefully();
    }

    @Test
    void testCallToolOnProgress() throws InterruptedException {

        Consumer<MyMcpSchema.ProgressMessageNotification> progressHandler = progressMessageNotification -> {
            log.info("progressMessageNotification:{}", JsonUtils.toPrettyJSONString(progressMessageNotification));
        };

        MyMcpSyncClient client = MyMcpSyncClient.builder(this.baseUri)
                .setWebsocket(true)
                .setTokenSupplier(this.tokenSupplier)
                .setMcpClientCommonProperties(this.mcpClientCommonProperties)
                .setProgressHandler(progressHandler)
                .build();

        JSONObject result = client.callTool("/getTokenByProgress", JSONObject.class);

        log.info("{}", JsonUtils.toPrettyJSONString(result));

        Thread.sleep(12000);
        client.closeGracefully();
    }

}
