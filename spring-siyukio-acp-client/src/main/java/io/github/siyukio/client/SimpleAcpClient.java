package io.github.siyukio.client;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.client.loadbalancer.DirectAcpClientLoadBalancer;
import io.github.siyukio.client.loadbalancer.DnsRandomAcpClientLoadBalancer;
import io.github.siyukio.client.loadbalancer.SimpleAsyncAcpClientLoadBalancer;
import io.github.siyukio.tools.acp.AcpSchemaExt;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Bugee
 */
@Slf4j
public class SimpleAcpClient {

    private final SimpleAsyncAcpClientLoadBalancer simpleAsyncAcpClientLoadBalancer;

    private SimpleAcpClient(SimpleAsyncAcpClientLoadBalancer simpleAsyncAcpClientLoadBalancer) {
        this.simpleAsyncAcpClientLoadBalancer = simpleAsyncAcpClientLoadBalancer;
    }

    public static Builder builder(String uri) {
        return new Builder(uri);
    }


    public <T> T callTool(String tool, Object params, Class<T> typeClass) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.callTool(tool, params, typeClass);
    }

    public <T> T callTool(String tool, Class<T> typeClass) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.callTool(tool, new JSONObject(), typeClass);
    }

    public void callTool(String tool) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        simpleAsyncAcpClient.callTool(tool, new JSONObject(), Void.class);
    }

    public void callTool(String tool, Object params) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        simpleAsyncAcpClient.callTool(tool, params, Void.class);
    }

    public AcpSchemaExt.ListToolsResult listTools() {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.listTools();
    }

    public AcpSchema.NewSessionResponse newSession() {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.newSession();
    }

    public AcpSchema.LoadSessionResponse loadSession(String sessionId) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.loadSession(sessionId);
    }

    public void cancel(String sessionId) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        simpleAsyncAcpClient.cancel(sessionId);
    }

    public AcpSchema.SetSessionModeResponse setSessionMode(String sessionId, String modeId) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.setSessionMode(sessionId, modeId);
    }

    public AcpSchema.SetSessionModelResponse setSessionModel(String sessionId, String modelId) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.setSessionModel(sessionId, modelId);
    }

    public AcpSchema.PromptResponse prompt(String sessionId, String prompt) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientLoadBalancer.getClient();
        return simpleAsyncAcpClient.prompt(sessionId, prompt);
    }

    public void close() {
        this.simpleAsyncAcpClientLoadBalancer.close();
    }

    public static class Builder {

        private final String uri;
        private final List<SimpleAsyncAcpClient.ProgressNotificationHandler> progressNotificationHandlers = new ArrayList<>();
        private final List<SimpleAsyncAcpClient.SessionNotificationHandler> sessionNotificationHandlers = new ArrayList<>();
        private SimpleAsyncAcpClient.RequestPermissionHandler requestPermissionHandler = null;
        private SimpleAsyncAcpClient.TerminalHandler terminalHandler = null;
        private SimpleAsyncAcpClient.ReadTextFileHandler readTextFileHandler = null;
        private SimpleAsyncAcpClient.WriteTextFileHandler writeTextFileHandler = null;
        private Duration requestTimeout = Duration.ofSeconds(60);
        private Duration connectTimeout = Duration.ofSeconds(12);
        private String authorization = "";
        private boolean loadBalance = false;

        public Builder(String uri) {
            this.uri = uri;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder loadBalance(boolean loadBalance) {
            this.loadBalance = loadBalance;
            return this;
        }

        public Builder authorization(String authorization) {
            this.authorization = authorization;
            return this;
        }

        public Builder readTextFileHandler(SimpleAsyncAcpClient.ReadTextFileHandler readTextFileHandler) {
            this.readTextFileHandler = readTextFileHandler;
            return this;
        }

        public Builder writeTextFileHandler(SimpleAsyncAcpClient.WriteTextFileHandler writeTextFileHandler) {
            this.writeTextFileHandler = writeTextFileHandler;
            return this;
        }

        public Builder progressNotificationHandler(SimpleAsyncAcpClient.ProgressNotificationHandler progressNotificationHandler) {
            this.progressNotificationHandlers.add(progressNotificationHandler);
            return this;
        }

        public Builder sessionNotificationHandler(SimpleAsyncAcpClient.SessionNotificationHandler sessionNotificationHandler) {
            this.sessionNotificationHandlers.add(sessionNotificationHandler);
            return this;
        }

        public Builder requestPermissionHandler(SimpleAsyncAcpClient.RequestPermissionHandler requestPermissionHandler) {
            this.requestPermissionHandler = requestPermissionHandler;
            return this;
        }

        public Builder terminalHandler(SimpleAsyncAcpClient.TerminalHandler terminalHandler) {
            this.terminalHandler = terminalHandler;
            return this;
        }

        public SimpleAcpClient build() {
            SimpleAsyncAcpClient.Builder builder = new SimpleAsyncAcpClient.Builder(
                    this.progressNotificationHandlers, this.sessionNotificationHandlers,
                    this.requestPermissionHandler, this.terminalHandler, this.readTextFileHandler, this.writeTextFileHandler,
                    this.requestTimeout, this.connectTimeout, this.authorization
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
