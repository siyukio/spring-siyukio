package io.github.siyukio.application.boot.starter.autoconfigure;


import io.github.siyukio.application.mcp.MyMethodToolCallback;
import io.github.siyukio.tools.api.AipHandlerManager;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.server.transport.MyWebSocketHandler;
import io.modelcontextprotocol.server.transport.MyWebSocketStreamableServerTransportProvider;
import io.modelcontextprotocol.server.transport.MyWebSocketStreamableSession;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
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
@EnableConfigurationProperties(McpServerProperties.class)
@AutoConfigureAfter({WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class})
@Slf4j
public class MyWebSocketStreamableMcpServerConfiguration implements WebSocketConfigurer, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Bean
    public MyWebSocketStreamableServerTransportProvider myWebSocketStreamableServerTransportProvider(McpServerProperties mcpServerProperties) {
        McpTransportContextExtractor<MyWebSocketStreamableSession> contextExtractor = (myWebSocketStreamableSession) ->
        {
            Map<String, Object> metadata = new HashMap<>();
            Token token = myWebSocketStreamableSession.getToken();
            if (token != null && !token.refresh && !token.expired) {
                metadata.put(HttpHeaders.AUTHORIZATION, token);
            }
            return McpTransportContext.create(metadata);
        };
        return new MyWebSocketStreamableServerTransportProvider(JsonUtils.getObjectMapper(),
                mcpServerProperties.getMcpEndpoint() + "/ws", contextExtractor, Duration.ofSeconds(20));
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        MyWebSocketStreamableServerTransportProvider myWebSocketServerTransportProvider = this.applicationContext.getBean(MyWebSocketStreamableServerTransportProvider.class);
        TokenProvider tokenProvider = this.applicationContext.getBean(TokenProvider.class);
        MyWebSocketHandler myWebSocketHandler = new MyWebSocketHandler(myWebSocketServerTransportProvider, tokenProvider);
        registry.addHandler(myWebSocketHandler, myWebSocketServerTransportProvider.getMcpEndpoint())
                .setHandshakeHandler(myWebSocketHandler)
                .setAllowedOrigins("*");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    public McpSyncServer websocketMcpServer(MyWebSocketStreamableServerTransportProvider myWebSocketStreamableServerTransportProvider,
                                            McpServerProperties mcpServerProperties,
                                            AipHandlerManager aipHandlerManager) {
        // Configure server capabilities with resource support
        McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
                .tools(true) // Tool support with list changes notifications
//                .logging() // Logging support
                .build();

        // Create the server with both tool and resource capabilities
        String serverName = mcpServerProperties.getName();
        String serverVersion = mcpServerProperties.getVersion();

        McpServer.SyncSpecification<McpServer.StreamableSyncSpecification> spec = McpServer.sync(myWebSocketStreamableServerTransportProvider)
                .serverInfo(serverName, serverVersion)
                .requestTimeout(mcpServerProperties.getRequestTimeout())
                .capabilities(capabilities);

        List<McpServerFeatures.SyncToolSpecification> tools = MyMethodToolCallback.getSyncToolSpecifications(aipHandlerManager);
        spec.tools(tools);

        log.info("start websocket streamable mcp server {},{}", serverName, serverVersion);
        return spec.build();
    }
}
