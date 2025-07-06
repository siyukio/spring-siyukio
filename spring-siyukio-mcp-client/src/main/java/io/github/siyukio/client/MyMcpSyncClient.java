package io.github.siyukio.client;

import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.util.AsyncUtils;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.client.MyMcpAsyncClient;
import io.modelcontextprotocol.client.MyMcpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientWebSocketClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Bugee
 */
@Slf4j
public class MyMcpSyncClient {

    private final ReentrantLock lock = new ReentrantLock();

    private final String baseUri;

    private final String authorization;

    private final Map<String, String> headers = new HashMap<>();

    private final Duration requestTimeout;

    private final boolean webSocket;

    private final String name;

    private final String version;

    private final Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler;
    private final Supplier<String> tokenSupplier;
    private MyMcpAsyncClient mcpAsyncClient = null;
    private McpSchema.InitializeResult result = null;
    private volatile long lastPingTime = System.currentTimeMillis();

    public MyMcpSyncClient(String baseUri,
                           Duration requestTimeout,
                           boolean webSocket, String name, String version,
                           Supplier<String> tokenSupplier,
                           String authorization,
                           Map<String, String> headers,
                           Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler) {
        this.baseUri = baseUri;
        this.requestTimeout = requestTimeout;
        this.name = name;
        this.version = version;
        this.tokenSupplier = tokenSupplier;
        this.authorization = authorization;
        this.headers.putAll(headers);
        this.samplingHandler = samplingHandler;
        this.webSocket = webSocket;
    }

    public static Builder builder(String baseUri) {
        return new Builder(baseUri);
    }

    private boolean ping(MyMcpAsyncClient mcpAsyncClient) {
        Future<Boolean> future = AsyncUtils.submit(() -> {
            try {
                mcpAsyncClient.ping().block();
                this.lastPingTime = System.currentTimeMillis();
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        });
        boolean result;
        try {
            result = future.get(3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            result = false;
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    private MyMcpAsyncClient initMcpAsyncClient() {
        String newBaseUri = this.baseUri;
        log.debug("use baseUri: {}", newBaseUri);
        if (this.webSocket) {
            URI uri = URI.create(newBaseUri);
            newBaseUri = "";
            if (uri.getScheme().equalsIgnoreCase("https")) {
                newBaseUri += "wss://";
            } else {
                newBaseUri += "ws://";
            }
            newBaseUri += uri.getHost();
            if (uri.getPort() > 0) {
                newBaseUri += ":" + uri.getPort();
            }
            log.debug("use webSocket baseUri: {}", newBaseUri);
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
            headerMap.put(ApiConstants.AUTHORIZATION, token);
        }

        final McpClientTransport transport;
        if (this.webSocket) {
            transport = new HttpClientWebSocketClientTransport(newBaseUri, headerMap, JsonUtils.getObjectMapper());
        } else {

            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder();
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                httpRequestBuilder.header(entry.getKey(), entry.getValue());
            }
            httpRequestBuilder.header("Content-Type", "application/json");

            transport = HttpClientSseClientTransport
                    .builder(newBaseUri)
                    .objectMapper(JsonUtils.getObjectMapper())
                    .requestBuilder(httpRequestBuilder)
                    .build();
        }

        MyMcpClient.AsyncSpec asyncSpec = MyMcpClient.async(transport)
                .clientInfo(new McpSchema.Implementation(this.name, this.version))
                .requestTimeout(this.requestTimeout)
                .initializationTimeout(Duration.ofSeconds(6));
        if (this.samplingHandler != null) {
            Function<McpSchema.CreateMessageRequest, Mono<McpSchema.CreateMessageResult>> samplingHandler = r -> Mono
                    .fromCallable(() -> this.samplingHandler.apply(r))
                    .subscribeOn(Schedulers.boundedElastic());
            asyncSpec.capabilities(McpSchema.ClientCapabilities.builder()
                            .sampling()
                            .build())
                    .sampling(samplingHandler);
        }
        MyMcpAsyncClient client = asyncSpec.build();

        this.result = client.initialize().block();
        return client;
    }

    private MyMcpAsyncClient getMcpSyncClient() {
        this.lock.lock();
        try {
            if (this.mcpAsyncClient != null && System.currentTimeMillis() - this.lastPingTime >= 6000) {
                boolean result = this.ping(this.mcpAsyncClient);
                if (!result) {
                    this.mcpAsyncClient = null;
                }
            }

            if (this.mcpAsyncClient == null) {
                this.mcpAsyncClient = this.initMcpAsyncClient();
            }
            return this.mcpAsyncClient;
        } finally {
            this.lock.unlock();
        }
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
        return this.getMcpSyncClient().callTool(callToolRequest).block();
    }

    private void throwError(String text) {
        if (text.startsWith("{") || text.endsWith("}")) {
            JSONObject json = JsonUtils.parseObject(text);
            int error = json.optInt("error", HttpStatus.OK.value());
            if (error != HttpStatus.OK.value()) {
                String message = json.optString("message", "");
                throw new ApiException(error, message);
            }
        } else {
            throw new ApiException(text);
        }
    }

    public McpSchema.CallToolResult callTool(String toolName, Object params) {
        Map<String, Object> arguments = JsonUtils.copy(params, Map.class, String.class, Object.class);
        return this.callTool(new McpSchema.CallToolRequest(toolName, arguments));
    }

    public <T> T callTool(String toolName, Object params, Class<T> returnType) {
        McpSchema.CallToolResult result = this.callTool(toolName, params);
        JSONObject contentJson = JsonUtils.copy(result.content().getFirst(), JSONObject.class);
        String text = contentJson.optString("text");
        if (result.isError()) {
            this.throwError(text);
        }
        return JsonUtils.parse(text, returnType);
    }

    public <T> T callTool(String toolName, Class<T> returnType) {
        return this.callTool(toolName, Map.of(), returnType);
    }

    // --------------------------
    // Tools
    // --------------------------

    /**
     * Retrieves the list of all tools provided by the server.
     *
     * @return The list of tools result containing: - tools: List of available tools, each
     * with a name, description, and input schema - nextCursor: Optional cursor for
     * pagination if more tools are available
     */
    public McpSchema.ListToolsResult listTools() {
        return this.getMcpSyncClient().listTools().block();
    }

    public McpSchema.InitializeResult initialize() {
        if (this.result == null) {
            this.result = this.getMcpSyncClient().initialize().block();
        }
        return this.result;
    }

    public void closeGracefully() {
        if (this.mcpAsyncClient == null) {
            return;
        }
        try {
            this.mcpAsyncClient.closeGracefully().block();
        } catch (RuntimeException ignored) {
        }
    }

    public static class Builder {

        private final String baseUri;

        private final Map<String, String> headers = new HashMap<>();
        private boolean webSocket = false;
        private String authorization = "";
        private Duration requestTimeout = Duration.ofSeconds(60);
        private String name = "mcp-client";
        private String version = "0.10.0";
        private Supplier<String> tokenSupplier;

        private Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler = null;

        private Builder(String baseUri) {
            this.baseUri = baseUri;
        }

        public Builder setAuthorization(String authorization) {
            this.authorization = authorization;
            return this;
        }

        public void addHeader(String name, String value) {
            this.headers.put(name, value);
        }

        public Builder setWebsocket(boolean webSocket) {
            this.webSocket = webSocket;
            return this;
        }

        public Builder setTokenSupplier(Supplier<String> tokenSupplier) {
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
            this.name = mcpClientCommonProperties.getName();
            this.version = mcpClientCommonProperties.getVersion();
            this.requestTimeout = mcpClientCommonProperties.getRequestTimeout();
            return this;
        }

        public Builder setSamplingHandler(Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler) {
            this.samplingHandler = samplingHandler;
            return this;
        }

        public MyMcpSyncClient build() {
            return new MyMcpSyncClient(this.baseUri, this.requestTimeout, this.webSocket,
                    this.name, this.version, this.tokenSupplier,
                    this.authorization, this.headers,
                    this.samplingHandler);
        }
    }
}
