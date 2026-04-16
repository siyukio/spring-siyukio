package io.github.siyukio.client;

import com.agentclientprotocol.sdk.spec.AcpSchema;
import io.github.siyukio.tools.acp.AcpSchemaExt;
import io.github.siyukio.tools.util.HttpClientUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Bugee
 */
@Slf4j
public class SimpleAcpClient {

    private final URI uri;
    private final SimpleAsyncAcpClient.Builder simpleAsyncAcpClientBuilder;

    private final boolean loadBalance;

    private final Lock lock = new ReentrantLock();

    private volatile List<SimpleAsyncAcpClient> resolvedSimpleAcpAsyncClients = new ArrayList<>();

    private volatile long lastResolvedTime = 0L;

    private SimpleAcpClient(URI uri, SimpleAsyncAcpClient.Builder simpleAsyncAcpClientBuilder,
                            boolean loadBalance) {
        this.uri = uri;
        this.simpleAsyncAcpClientBuilder = simpleAsyncAcpClientBuilder;
        if (loadBalance) {
            String schema = this.uri.getScheme();
            loadBalance = "ws".equals(schema) || "http".equals(schema);
        }
        this.loadBalance = loadBalance;
        if (!this.loadBalance) {
            SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientBuilder.build(this.uri);
            this.resolvedSimpleAcpAsyncClients.add(simpleAsyncAcpClient);
        }
    }

    public static Builder builder(String uri) {
        return new Builder(uri);
    }

    private URI buildUri(URI originalUri, String ip) {
        String scheme = originalUri.getScheme();
        int port = originalUri.getPort();
        String path = originalUri.getPath();

        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(ip);
        if (port > 0) {
            sb.append(":").append(port);
        }
        if (path != null && !path.isEmpty()) {
            sb.append(path);
        }
        return URI.create(sb.toString());
    }

    private void ensureResolved() {
        lock.lock();
        if (System.currentTimeMillis() - this.lastResolvedTime < 15000) {
            return;
        }
        String host = this.uri.getHost();
        try {
            List<String> resolvedIps = HttpClientUtils.resolveDomain(host);
            log.debug("Resolved {} ips: {}", host, resolvedIps);
            List<SimpleAsyncAcpClient> newSimpleAcpAsyncClients = new ArrayList<>();
            if (CollectionUtils.isEmpty(resolvedIps)) {
                SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientBuilder.build(this.uri);
                newSimpleAcpAsyncClients.add(simpleAsyncAcpClient);
                this.resolvedSimpleAcpAsyncClients = newSimpleAcpAsyncClients;
                this.lastResolvedTime = System.currentTimeMillis();
                return;
            }

            Set<String> newIps = new HashSet<>(resolvedIps);
            this.resolvedSimpleAcpAsyncClients.forEach(simpleAsyncAcpClient -> {
                String ip = simpleAsyncAcpClient.getUri().getHost();
                if (newIps.contains(ip)) {
                    newSimpleAcpAsyncClients.add(simpleAsyncAcpClient);
                    newIps.remove(ip);
                }
            });
            if (!CollectionUtils.isEmpty(newIps)) {
                newIps.forEach(ip -> {
                    URI newUri = this.buildUri(this.uri, ip);
                    SimpleAsyncAcpClient simpleAsyncAcpClient = this.simpleAsyncAcpClientBuilder.build(newUri);
                    newSimpleAcpAsyncClients.add(simpleAsyncAcpClient);
                });
            }

            this.resolvedSimpleAcpAsyncClients = newSimpleAcpAsyncClients;
            this.lastResolvedTime = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }

    private SimpleAsyncAcpClient selectRandomClient() {
        if (!this.loadBalance) {
            return this.resolvedSimpleAcpAsyncClients.getLast();
        }
        this.ensureResolved();
        if (this.resolvedSimpleAcpAsyncClients.size() == 1) {
            return this.resolvedSimpleAcpAsyncClients.getFirst();
        }
        int index = ThreadLocalRandom.current().nextInt(this.resolvedSimpleAcpAsyncClients.size());
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.resolvedSimpleAcpAsyncClients.get(index);
        log.debug("Selected simpleAsyncAcpClient: {}, {}, {}", this.uri, simpleAsyncAcpClient.getUri(), index);
        return simpleAsyncAcpClient;
    }


    public <T> T callTool(String tool, Object params, Class<T> typeClass) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.selectRandomClient();
        return simpleAsyncAcpClient.callTool(tool, params, typeClass);
    }

    public <T> T callTool(String tool, Class<T> typeClass) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.selectRandomClient();
        return simpleAsyncAcpClient.callTool(tool, new JSONObject(), typeClass);
    }

    public void callTool(String tool) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.selectRandomClient();
        simpleAsyncAcpClient.callTool(tool, new JSONObject(), Void.class);
    }

    public void callTool(String tool, Object params) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.selectRandomClient();
        simpleAsyncAcpClient.callTool(tool, params, Void.class);
    }

    public AcpSchemaExt.ListToolsResult listTools() {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.selectRandomClient();
        return simpleAsyncAcpClient.listTools();
    }

    public AcpSchema.NewSessionResponse newSession() {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.selectRandomClient();
        return simpleAsyncAcpClient.newSession();
    }

    public AcpSchema.LoadSessionResponse loadSession(String sessionId) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.selectRandomClient();
        return simpleAsyncAcpClient.loadSession(sessionId);
    }

    public void cancel(String sessionId) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.selectRandomClient();
        simpleAsyncAcpClient.cancel(sessionId);
    }

    public AcpSchema.SetSessionModeResponse setSessionMode(String sessionId, String modeId) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.selectRandomClient();
        return simpleAsyncAcpClient.setSessionMode(sessionId, modeId);
    }

    public AcpSchema.SetSessionModelResponse setSessionModel(String sessionId, String modelId) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.selectRandomClient();
        return simpleAsyncAcpClient.setSessionModel(sessionId, modelId);
    }

    public AcpSchema.PromptResponse prompt(String sessionId, String prompt) {
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.selectRandomClient();
        return simpleAsyncAcpClient.prompt(sessionId, prompt);
    }

    public void close() {
        this.resolvedSimpleAcpAsyncClients.forEach(simpleAsyncAcpClient -> {
            try {
                simpleAsyncAcpClient.close();
            } catch (Exception ignored) {
            }
        });
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
            return new SimpleAcpClient(
                    uri,
                    builder,
                    this.loadBalance);
        }
    }
}
