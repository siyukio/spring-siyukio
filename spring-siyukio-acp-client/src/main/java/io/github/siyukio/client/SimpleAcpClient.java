package io.github.siyukio.client;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.client.loadbalancer.DirectAcpClientLoadBalancer;
import io.github.siyukio.client.loadbalancer.DnsRandomAcpClientLoadBalancer;
import io.github.siyukio.client.loadbalancer.SimpleAsyncAcpClientLoadBalancer;
import io.github.siyukio.tools.acp.sdk.spec.AcpSchemaExt;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Simplified ACP client that delegates requests to an underlying {@link SimpleAsyncAcpClient}
 * obtained from a load balancer. Provides synchronous blocking call style.
 *
 * @author Bugee
 */
@Slf4j
public class SimpleAcpClient {

    private final SimpleAsyncAcpClientLoadBalancer simpleAsyncAcpClientLoadBalancer;

    /**
     * Creates a new SimpleAcpClient with the given load balancer.
     *
     * @param simpleAsyncAcpClientLoadBalancer the load balancer for selecting ACP clients
     */
    private SimpleAcpClient(SimpleAsyncAcpClientLoadBalancer simpleAsyncAcpClientLoadBalancer) {
        this.simpleAsyncAcpClientLoadBalancer = simpleAsyncAcpClientLoadBalancer;
    }

    /**
     * Creates a new builder for configuring and creating a SimpleAcpClient instance.
     *
     * @param uri the ACP server URI (e.g., "ws://localhost:8090/acp")
     * @return a new Builder instance
     */
    public static Builder builder(String uri) {
        return new Builder(uri);
    }

    public <T> T callTool(String tool, Object params, Class<T> typeClass, SimpleAsyncAcpClient.ProgressNotificationHandler progressNotificationHandler) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.callTool(tool, params, typeClass, progressNotificationHandler);
    }

    /**
     * Calls a tool with the given parameters and returns the result as the specified type.
     *
     * @param tool      the name of the tool to call
     * @param params    the parameters to pass to the tool
     * @param typeClass the expected return type class
     * @param <T>       the return type
     * @return the tool result deserialized to the specified type
     */
    public <T> T callTool(String tool, Object params, Class<T> typeClass) {
        return this.callTool(tool, params, typeClass, null);
    }

    /**
     * Calls a tool without parameters and returns the result as the specified type.
     *
     * @param tool      the name of the tool to call
     * @param typeClass the expected return type class
     * @param <T>       the return type
     * @return the tool result deserialized to the specified type
     */
    public <T> T callTool(String tool, Class<T> typeClass) {
        return this.callTool(tool, new JSONObject(), typeClass);
    }

    /**
     * Calls a tool with the given parameters and ignores the result.
     *
     * @param tool   the name of the tool to call
     * @param params the parameters to pass to the tool
     */
    public void callTool(String tool, Object params) {
        this.callTool(tool, params, Void.class);
    }

    /**
     * Calls a tool without parameters and ignores the result.
     *
     * @param tool the name of the tool to call
     */
    public void callTool(String tool) {
        this.callTool(tool, new JSONObject());
    }

    /**
     * Lists all available tools from the ACP server.
     *
     * @return the list of available tools
     */
    public AcpSchemaExt.ListToolsResult listTool() {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.listTools();
    }

    /**
     * Creates a new ACP session.
     *
     * @return the new session response containing session information
     */
    public AcpSchema.NewSessionResponse newSession() {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.newSession();
    }

    /**
     * Loads an existing ACP session by session ID.
     *
     * @param sessionId the ID of the session to load
     * @return the load session response containing session information
     */
    public AcpSchema.LoadSessionResponse loadSession(String sessionId) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.loadSession(sessionId);
    }

    /**
     * Cancels an ongoing operation in the specified session.
     *
     * @param sessionId the ID of the session to cancel
     */
    public void cancel(String sessionId) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        simpleAsyncAcpClient.cancel(sessionId);
    }

    /**
     * Sets the session mode for the specified session.
     *
     * @param sessionId the ID of the session
     * @param modeId    the mode ID to set
     * @return the set session mode response
     */
    public AcpSchema.SetSessionModeResponse setSessionMode(String sessionId, String modeId) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.setSessionMode(sessionId, modeId);
    }

    /**
     * Sets the session model for the specified session.
     *
     * @param sessionId the ID of the session
     * @param modelId   the model ID to set
     * @return the set session model response
     */
    public AcpSchema.SetSessionModelResponse setSessionModel(String sessionId, String modelId) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.setSessionModel(sessionId, modelId);
    }

    /**
     * Sends a prompt to the specified session and returns the response.
     *
     * @param sessionId the ID of the session
     * @param prompt    the prompt text to send
     * @return the prompt response
     */
    public AcpSchema.PromptResponse prompt(String sessionId, String prompt) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.prompt(sessionId, prompt);
    }

    /**
     * Closes this client and releases all underlying resources.
     */
    public void close() {
        this.simpleAsyncAcpClientLoadBalancer.close();
    }

    /**
     * Builder for creating {@link SimpleAcpClient} instances with custom configuration.
     */
    public static class Builder {

        private final String uri;
        private final List<SimpleAsyncAcpClient.SessionNotificationHandler> sessionNotificationHandlers = new ArrayList<>();
        private SimpleAsyncAcpClient.RequestPermissionHandler requestPermissionHandler = null;
        private SimpleAsyncAcpClient.TerminalHandler terminalHandler = null;
        private SimpleAsyncAcpClient.ReadTextFileHandler readTextFileHandler = null;
        private SimpleAsyncAcpClient.WriteTextFileHandler writeTextFileHandler = null;
        private Duration requestTimeout = Duration.ofSeconds(60);
        private Duration connectTimeout = Duration.ofSeconds(12);
        private String authorization = "";
        private boolean loadBalance = false;
        private String agentName = "";

        /**
         * Creates a new Builder with the specified ACP server URI.
         *
         * @param uri the ACP server URI (e.g., "ws://localhost:8090/acp" or "http://localhost:8090/acp")
         */
        public Builder(String uri) {
            this.uri = uri;
        }

        public Builder agentName(String agentName) {
            this.agentName = agentName;
            return this;
        }

        /**
         * Sets the timeout for individual requests.
         *
         * @param requestTimeout the request timeout duration
         * @return this builder for chaining
         */
        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        /**
         * Sets the timeout for establishing connections.
         *
         * @param connectTimeout the connection timeout duration
         * @return this builder for chaining
         */
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * Enables or disables client-side load balancing.
         * Load balancing only works with "ws" scheme.
         *
         * @param loadBalance true to enable load balancing, false to disable
         * @return this builder for chaining
         */
        public Builder loadBalance(boolean loadBalance) {
            this.loadBalance = loadBalance;
            return this;
        }

        /**
         * Sets the authorization header value for authenticated requests.
         *
         * @param authorization the authorization value (e.g., "Bearer xxx")
         * @return this builder for chaining
         */
        public Builder authorization(String authorization) {
            this.authorization = authorization;
            return this;
        }

        /**
         * Sets the handler for reading text files requested by the ACP server.
         *
         * @param readTextFileHandler the read text file handler
         * @return this builder for chaining
         */
        public Builder readTextFileHandler(SimpleAsyncAcpClient.ReadTextFileHandler readTextFileHandler) {
            this.readTextFileHandler = readTextFileHandler;
            return this;
        }

        /**
         * Sets the handler for writing text files requested by the ACP server.
         *
         * @param writeTextFileHandler the write text file handler
         * @return this builder for chaining
         */
        public Builder writeTextFileHandler(SimpleAsyncAcpClient.WriteTextFileHandler writeTextFileHandler) {
            this.writeTextFileHandler = writeTextFileHandler;
            return this;
        }

        /**
         * Adds a handler for session notifications from the ACP server.
         *
         * @param sessionNotificationHandler the session notification handler
         * @return this builder for chaining
         */
        public Builder sessionNotificationHandler(SimpleAsyncAcpClient.SessionNotificationHandler sessionNotificationHandler) {
            this.sessionNotificationHandlers.add(sessionNotificationHandler);
            return this;
        }

        /**
         * Sets the handler for permission requests from the ACP server.
         *
         * @param requestPermissionHandler the request permission handler
         * @return this builder for chaining
         */
        public Builder requestPermissionHandler(SimpleAsyncAcpClient.RequestPermissionHandler requestPermissionHandler) {
            this.requestPermissionHandler = requestPermissionHandler;
            return this;
        }

        /**
         * Sets the handler for terminal interactions with the ACP server.
         *
         * @param terminalHandler the terminal handler
         * @return this builder for chaining
         */
        public Builder terminalHandler(SimpleAsyncAcpClient.TerminalHandler terminalHandler) {
            this.terminalHandler = terminalHandler;
            return this;
        }

        /**
         * Builds a new {@link SimpleAcpClient} instance with the configured settings.
         * Automatically converts http/https URI schemes to ws/wss.
         *
         * @return a new SimpleAcpClient instance
         */
        public SimpleAcpClient build() {
            SimpleAsyncAcpClient.Builder builder = new SimpleAsyncAcpClient.Builder(
                    this.sessionNotificationHandlers, this.requestPermissionHandler,
                    this.terminalHandler, this.readTextFileHandler, this.writeTextFileHandler,
                    this.requestTimeout, this.connectTimeout, this.authorization, this.agentName
            );
            URI uri = URI.create(this.uri);
            if ("http".equals(uri.getScheme())) {
                uri = URI.create("ws://" + uri.getHost() + ":" + uri.getPort() + uri.getPath());
            } else if ("https".equals(uri.getScheme())) {
                uri = URI.create("wss://" + uri.getHost() + ":" + uri.getPort() + uri.getPath());
            }
            boolean loadBalance = this.loadBalance;
            if (loadBalance) {
                loadBalance = "ws".equals(uri.getScheme());
            }
            SimpleAsyncAcpClientLoadBalancer simpleAsyncAcpClientLoadBalancer;
            if (loadBalance) {
                simpleAsyncAcpClientLoadBalancer = new DnsRandomAcpClientLoadBalancer(uri, builder);
            } else {
                simpleAsyncAcpClientLoadBalancer = new DirectAcpClientLoadBalancer(uri, builder);
            }
            return new SimpleAcpClient(
                    simpleAsyncAcpClientLoadBalancer
            );
        }
    }
}
