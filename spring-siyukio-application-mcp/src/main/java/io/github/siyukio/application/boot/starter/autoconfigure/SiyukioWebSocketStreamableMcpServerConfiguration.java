package io.github.siyukio.application.boot.starter.autoconfigure;


import io.github.siyukio.application.mcp.MethodToolCallback;
import io.github.siyukio.tools.api.AipHandlerManager;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.XDataUtils;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.server.transport.WebSocketHandler;
import io.modelcontextprotocol.server.transport.WebSocketServerSession;
import io.modelcontextprotocol.server.transport.WebSocketStreamableContext;
import io.modelcontextprotocol.server.transport.WebSocketStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketConfiguration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bugee
 */
@Import(DelegatingWebSocketConfiguration.class)
@EnableConfigurationProperties(SiyukioMcpServerProperties.class)
@AutoConfigureAfter({SiyukioApplicationAutoConfiguration.class})
@Slf4j
public class SiyukioWebSocketStreamableMcpServerConfiguration implements WebSocketConfigurer, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Bean
    public WebSocketStreamableContext myWebsocketStreamableContext() {
        return new WebSocketStreamableContext();
    }

    @Bean
    public WebSocketStreamableServerTransportProvider myWebSocketStreamableServerTransportProvider(SiyukioMcpServerProperties siyukioMcpServerProperties) {
        WebSocketStreamableContext webSocketStreamableContext = this.applicationContext.getBean(WebSocketStreamableContext.class);

        McpTransportContextExtractor<WebSocketServerSession> contextExtractor = (webSocketServerSession) ->
        {
            Map<String, Object> metadata = new HashMap<>();
            Token token = webSocketServerSession.getToken();
            if (token != null && !token.refresh() && !token.expired()) {
                metadata.put(HttpHeaders.AUTHORIZATION, token);
            }
            return McpTransportContext.create(metadata);
        };
        return new WebSocketStreamableServerTransportProvider(XDataUtils.getObjectMapper(), webSocketStreamableContext,
                siyukioMcpServerProperties.getMcpEndpoint() + "/ws", contextExtractor, Duration.ofSeconds(20));
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        WebSocketStreamableContext webSocketStreamableContext = this.applicationContext.getBean(WebSocketStreamableContext.class);
        WebSocketStreamableServerTransportProvider myWebSocketServerTransportProvider = this.applicationContext.getBean(WebSocketStreamableServerTransportProvider.class);
        TokenProvider tokenProvider = this.applicationContext.getBean(TokenProvider.class);
        WebSocketHandler webSocketHandler = new WebSocketHandler(webSocketStreamableContext, tokenProvider);
        registry.addHandler(webSocketHandler, myWebSocketServerTransportProvider.getMcpEndpoint())
                .setHandshakeHandler(webSocketHandler)
                .setAllowedOrigins("*");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    public McpSyncServer websocketMcpServer(WebSocketStreamableServerTransportProvider webSocketStreamableServerTransportProvider,
                                            SiyukioMcpServerProperties siyukioMcpServerProperties,
                                            AipHandlerManager aipHandlerManager) {
        // Configure server capabilities with resource support
        McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
                .tools(true) // Tool support with list changes notifications
//                .logging() // Logging support
                .build();

        // Create the server with both tool and resource capabilities
        String serverName = siyukioMcpServerProperties.getName();
        String serverVersion = siyukioMcpServerProperties.getVersion();

        McpServer.SyncSpecification<McpServer.StreamableSyncSpecification> spec = McpServer.sync(webSocketStreamableServerTransportProvider)
                .serverInfo(serverName, serverVersion)
                .requestTimeout(siyukioMcpServerProperties.getRequestTimeout())
                .capabilities(capabilities);

        List<McpServerFeatures.SyncToolSpecification> tools = MethodToolCallback.getSyncToolSpecifications(aipHandlerManager);
        spec.tools(tools);

        log.info("start websocket streamable mcp server {},{}", serverName, serverVersion);
        return spec.build();
    }
}
