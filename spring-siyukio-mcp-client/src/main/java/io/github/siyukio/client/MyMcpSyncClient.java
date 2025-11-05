package io.github.siyukio.client;

import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.WebSocketClientStreamableTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Bugee
 */
@Slf4j
public class MyMcpSyncClient {

    private final String baseUrl;

    private final String mcpEndpoint;

    private final String authorization;

    private final Map<String, String> headers = new HashMap<>();

    private final Duration requestTimeout;

    private final boolean webSocket;

    private final String name;

    private final String version;

    private final Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler;
    private final Consumer<McpSchema.ProgressNotification> progressHandler;
    private final Supplier<String> tokenSupplier;

    public MyMcpSyncClient(String baseUrl,
                           String mcpEndpoint,
                           Duration requestTimeout,
                           boolean webSocket, String name, String version,
                           Supplier<String> tokenSupplier,
                           String authorization,
                           Map<String, String> headers,
                           Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler,
                           Consumer<McpSchema.ProgressNotification> progressHandler
    ) {
        this.baseUrl = baseUrl;
        this.mcpEndpoint = mcpEndpoint;
        this.requestTimeout = requestTimeout;
        this.name = name;
        this.version = version;
        this.tokenSupplier = tokenSupplier;
        this.authorization = authorization;
        this.headers.putAll(headers);
        this.samplingHandler = samplingHandler;
        this.progressHandler = progressHandler;
        this.webSocket = webSocket;
    }

    public static Builder builder() {
        return new Builder();
    }

    private boolean ping(McpAsyncClient mcpAsyncClient) {
        try {
            mcpAsyncClient.ping().block();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private McpAsyncClient createMcpAsyncClient(String targetUri, String targetMcpEndpoint) {
        log.debug("use targetUri: {}, {}", targetUri, targetMcpEndpoint);
        if (this.webSocket) {
            URI uri = URI.create(targetUri);
            targetUri = "";
            if (uri.getScheme().equalsIgnoreCase("https")) {
                targetUri += "wss://";
            } else {
                targetUri += "ws://";
            }
            targetUri += uri.getHost();
            if (uri.getPort() > 0) {
                targetUri += ":" + uri.getPort();
            }
            targetMcpEndpoint += "/ws";
            log.debug("use webSocket targetUri: {}, {}", targetUri, targetMcpEndpoint);
        }

        String token = this.authorization;
        if (this.tokenSupplier != null) {
            String autoToken = this.tokenSupplier.get();
            if (StringUtils.hasText(autoToken)) {
                token = autoToken;
            }
        }

        Map<String, String> headerMap = new HashMap<>(this.headers);
        if (StringUtils.hasText(token)) {
            headerMap.put(HttpHeaders.AUTHORIZATION, token);
        }

        final McpClientTransport transport;
        if (this.webSocket) {
            transport = new WebSocketClientStreamableTransport(headerMap, targetUri, targetMcpEndpoint);
        } else {

            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder();
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                httpRequestBuilder.header(entry.getKey(), entry.getValue());
            }
            httpRequestBuilder.header("Content-Type", "application/json");

            transport = HttpClientStreamableHttpTransport
                    .builder(targetUri)
                    .endpoint(targetMcpEndpoint)
                    .objectMapper(JsonUtils.getObjectMapper())
                    .requestBuilder(httpRequestBuilder)
                    .build();
        }

        McpClient.AsyncSpec asyncSpec = McpClient.async(transport)
                .clientInfo(new McpSchema.Implementation(this.name, this.version))
                .requestTimeout(this.requestTimeout)
                .initializationTimeout(Duration.ofSeconds(60));
        if (this.samplingHandler != null) {
            Function<McpSchema.CreateMessageRequest, Mono<McpSchema.CreateMessageResult>> samplingConsumer = r -> Mono
                    .fromCallable(() -> this.samplingHandler.apply(r))
                    .subscribeOn(Schedulers.boundedElastic());
            asyncSpec.capabilities(McpSchema.ClientCapabilities.builder()
                            .sampling()
                            .build())
                    .sampling(samplingConsumer);
        }
        if (this.progressHandler != null) {
            Function<McpSchema.ProgressNotification, Mono<Void>> progressConsumer = r -> Mono
                    .fromRunnable(() -> this.progressHandler.accept(r));
            asyncSpec.progressConsumer(progressConsumer);
        }
        return asyncSpec.build();
    }

    public McpAsyncClient getMcpSyncClient() {
        return this.createMcpAsyncClient(this.baseUrl, this.mcpEndpoint);
    }

    /**
     * Calls a tool provided by the server. Tools enable servers to expose executable
     * functionality that can interact with external systems, perform computations, and
     * take actions in the real world.
     *
     * @param callToolRequest The request containing: - name: The name of the tool to call
     *                        (must match a tool name from tools/list) - arguments: Arguments that conform to the
     *                        tool's input schema
     * @return The tool execution result containing: - content: List of content items
     * (text, images, or embedded resources) representing the tool's output - isError:
     * Boolean indicating if the execution failed (true) or succeeded (false/absent)
     */
    public McpSchema.CallToolResult callTool(McpSchema.CallToolRequest callToolRequest) {
        McpAsyncClient mcpAsyncClient = this.getMcpSyncClient();
        McpSchema.CallToolResult result = mcpAsyncClient.callTool(callToolRequest).block();
        mcpAsyncClient.close();
        return result;
    }

    public McpSchema.CallToolResult callTool(String toolName, Object params) {
        Map<String, Object> arguments = JsonUtils.copy(params, Map.class, String.class, Object.class);
        return this.callTool(new McpSchema.CallToolRequest(toolName, arguments));
    }

    public <T> T callTool(String toolName, Object params, Class<T> returnType) {
        McpSchema.CallToolResult result = this.callTool(toolName, params);

        if (result.isError()) {
            if (result.structuredContent() != null) {
                JSONObject contentJson = JsonUtils.copy(result.structuredContent(), JSONObject.class);
                JSONObject errorJson = contentJson.optJSONObject("error");
                int code = errorJson.optInt("code", HttpStatus.OK.value());
                String message = errorJson.optString("message", "");
                throw new ApiException(code, message);
            }

            JSONObject contentJson = JsonUtils.copy(result.content().getFirst(), JSONObject.class);
            String text = contentJson.optString("text");
            throw new ApiException(text);
        }
        return JsonUtils.copy(result.structuredContent(), returnType);
    }

    // --------------------------
    // Tools
    // --------------------------

    public <T> T callTool(String toolName, Class<T> returnType) {
        return this.callTool(toolName, Map.of(), returnType);
    }

    /**
     * Retrieves the list of all tools provided by the server.
     *
     * @return The list of tools result containing: - tools: List of available tools, each
     * with a name, description, and input schema - nextCursor: Optional cursor for
     * pagination if more tools are available
     */
    public McpSchema.ListToolsResult listTools() {
        McpAsyncClient mcpAsyncClient = this.getMcpSyncClient();
        McpSchema.ListToolsResult result = mcpAsyncClient.listTools().block();
        mcpAsyncClient.close();
        return result;
    }

    public static class Builder {

        private final Map<String, String> headers = new HashMap<>();
        private String baseUrl = "http://localhost";
        private String mcpEndpoint = "/mcp";
        private boolean webSocket = false;
        private String authorization = "";
        private Duration requestTimeout = Duration.ofSeconds(60);
        private String name = "mcp-client";
        private String version = "0.12.1";
        private Supplier<String> tokenSupplier;

        private Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler = null;
        private Consumer<McpSchema.ProgressNotification> progressHandler = null;

        private Builder() {
        }

        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder setMcpEndpoint(String mcpEndpoint) {
            this.mcpEndpoint = mcpEndpoint;
            return this;
        }

        public Builder setAuthorization(String authorization) {
            this.authorization = authorization;
            return this;
        }

        public void addHeader(String name, String value) {
            this.headers.put(name, value);
        }

        public Builder useWebsocket(boolean webSocket) {
            this.webSocket = webSocket;
            return this;
        }

        public Builder useTokenSupplier(Supplier<String> tokenSupplier) {
            this.tokenSupplier = tokenSupplier;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder setMcpClientCommonProperties(McpClientCommonProperties mcpClientCommonProperties) {
            this.baseUrl = mcpClientCommonProperties.getBaseUrl();
            this.mcpEndpoint = mcpClientCommonProperties.getMcpEndpoint();
            this.name = mcpClientCommonProperties.getName();
            this.version = mcpClientCommonProperties.getVersion();
            this.requestTimeout = mcpClientCommonProperties.getRequestTimeout();
            return this;
        }

        public Builder setSamplingHandler(Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler) {
            this.samplingHandler = samplingHandler;
            return this;
        }

        public Builder setProgressHandler(Consumer<McpSchema.ProgressNotification> progressHandler) {
            this.progressHandler = progressHandler;
            return this;
        }

        public MyMcpSyncClient build() {
            return new MyMcpSyncClient(this.baseUrl, this.mcpEndpoint, this.requestTimeout, this.webSocket,
                    this.name, this.version, this.tokenSupplier,
                    this.authorization, this.headers,
                    this.samplingHandler, this.progressHandler);
        }
    }
}
