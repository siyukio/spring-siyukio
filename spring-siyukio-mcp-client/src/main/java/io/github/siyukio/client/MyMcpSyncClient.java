package io.github.siyukio.client;

import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.client.MyMcpAsyncClient;
import io.modelcontextprotocol.client.MyMcpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientWebSocketClientTransport;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.MyMcpSchema;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Bugee
 */
@Slf4j
public class MyMcpSyncClient {

    private final Map<String, MyMcpAsyncClient> myMcpAsyncClientMap = new ConcurrentHashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    private final String baseUri;

    private final String authorization;

    private final Map<String, String> headers = new HashMap<>();

    private final Duration requestTimeout;

    private final boolean webSocket;

    private final boolean internal;

    private final String name;

    private final String version;

    private final Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler;
    private final Consumer<MyMcpSchema.ProgressMessageNotification> progressHandler;
    private final Supplier<String> tokenSupplier;

    public MyMcpSyncClient(String baseUri,
                           Duration requestTimeout,
                           boolean internal, boolean webSocket, String name, String version,
                           Supplier<String> tokenSupplier,
                           String authorization,
                           Map<String, String> headers,
                           Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler,
                           Consumer<MyMcpSchema.ProgressMessageNotification> progressHandler
    ) {
        this.baseUri = baseUri;
        this.requestTimeout = requestTimeout;
        this.name = name;
        this.version = version;
        this.tokenSupplier = tokenSupplier;
        this.authorization = authorization;
        this.headers.putAll(headers);
        this.samplingHandler = samplingHandler;
        this.progressHandler = progressHandler;
        this.webSocket = webSocket;
        this.internal = internal;
    }

    public static Builder builder(String baseUri) {
        return new Builder(baseUri);
    }

    private boolean ping(MyMcpAsyncClient mcpAsyncClient) {
        try {
            mcpAsyncClient.ping().block();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private MyMcpAsyncClient createMcpAsyncClient(String targetUri) {
        log.debug("use targetUri: {}", targetUri);
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
            log.debug("use webSocket targetUri: {}", targetUri);
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
            transport = new HttpClientWebSocketClientTransport(targetUri, headerMap, JsonUtils.getObjectMapper());
        } else {

            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder();
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                httpRequestBuilder.header(entry.getKey(), entry.getValue());
            }
            httpRequestBuilder.header("Content-Type", "application/json");

            transport = HttpClientSseClientTransport
                    .builder(targetUri)
                    .objectMapper(JsonUtils.getObjectMapper())
                    .requestBuilder(httpRequestBuilder)
                    .build();
        }

        MyMcpClient.AsyncSpec asyncSpec = MyMcpClient.async(transport)
                .clientInfo(new McpSchema.Implementation(this.name, this.version))
                .requestTimeout(this.requestTimeout)
                .initializationTimeout(Duration.ofSeconds(6));
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
            asyncSpec.progressConsumer(this.progressHandler);
        }
        return asyncSpec.build();
    }

    private String getRedirectUri() {
        URI httpUri = URI.create(this.baseUri + HttpServletSseServerTransportProvider.DEFAULT_SSE_ENDPOINT);
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(6))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder().uri(httpUri);
        httpRequestBuilder.header(HttpHeaders.FROM, "internal");
        try {
            HttpResponse<Void> response = client.send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == HttpStatus.TEMPORARY_REDIRECT.value()) {
                String location = response.headers().firstValue(HttpHeaders.LOCATION).orElseThrow();
                int index = location.lastIndexOf(HttpServletSseServerTransportProvider.DEFAULT_SSE_ENDPOINT);
                if (index > 0) {
                    location = location.substring(0, index);
                }
                log.debug("{} redirect to {}", this.baseUri, location);
                return location;
            }
        } catch (IOException | InterruptedException ignored) {
            return this.baseUri;
        }
        return this.baseUri;
    }

    private MyMcpAsyncClient getTargetMcpSyncClient(String targetUri) {
        this.lock.lock();
        try {
            MyMcpAsyncClient myMcpAsyncClient = this.myMcpAsyncClientMap.get(targetUri);
            if (myMcpAsyncClient != null) {
                boolean result = this.ping(myMcpAsyncClient);
                if (!result) {
                    myMcpAsyncClient = null;
                    this.myMcpAsyncClientMap.remove(targetUri);
                }
            }

            if (myMcpAsyncClient == null) {
                myMcpAsyncClient = this.createMcpAsyncClient(targetUri);
                myMcpAsyncClient.initialize().block();
                this.myMcpAsyncClientMap.put(targetUri, myMcpAsyncClient);
                log.info("cache MyMcpAsyncClient, baseUri:{} targetUri:{}", this.baseUri, targetUri);
            }
            return myMcpAsyncClient;
        } finally {
            this.lock.unlock();
        }
    }

    private MyMcpAsyncClient getMcpSyncClient() {
        String targetUri = this.baseUri;
        if (this.internal) {
            targetUri = this.getRedirectUri();
        }
        return this.getTargetMcpSyncClient(targetUri);
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

    public void closeGracefully() {
        for (MyMcpAsyncClient value : this.myMcpAsyncClientMap.values()) {
            try {
                value.closeGracefully().block();
            } catch (RuntimeException ignored) {
            }
        }
        this.myMcpAsyncClientMap.clear();
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
        private boolean internal = false;

        private Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler = null;
        private Consumer<MyMcpSchema.ProgressMessageNotification> progressHandler = null;

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

        public Builder useWebsocket(boolean webSocket) {
            this.webSocket = webSocket;
            return this;
        }

        public Builder useInternal(boolean internal) {
            this.internal = internal;
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
            this.name = mcpClientCommonProperties.getName();
            this.version = mcpClientCommonProperties.getVersion();
            this.requestTimeout = mcpClientCommonProperties.getRequestTimeout();
            return this;
        }

        public Builder setSamplingHandler(Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> samplingHandler) {
            this.samplingHandler = samplingHandler;
            return this;
        }

        public Builder setProgressHandler(Consumer<MyMcpSchema.ProgressMessageNotification> progressHandler) {
            this.progressHandler = progressHandler;
            return this;
        }

        public MyMcpSyncClient build() {
            return new MyMcpSyncClient(this.baseUri, this.requestTimeout, this.internal, this.webSocket,
                    this.name, this.version, this.tokenSupplier,
                    this.authorization, this.headers,
                    this.samplingHandler, this.progressHandler);
        }
    }
}
