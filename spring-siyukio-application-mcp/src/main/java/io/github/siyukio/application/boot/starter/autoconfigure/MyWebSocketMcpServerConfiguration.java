package io.github.siyukio.application.boot.starter.autoconfigure;


import io.github.siyukio.application.mcp.MyMethodToolCallback;
import io.github.siyukio.tools.api.AipHandlerManager;
import io.github.siyukio.tools.api.ApiHandler;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.MySyncSpecification;
import io.modelcontextprotocol.server.transport.MyWebSocketHandler;
import io.modelcontextprotocol.server.transport.MyWebSocketServerTransportProvider;
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
import org.springframework.web.socket.config.annotation.DelegatingWebSocketConfiguration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Map;

/**
 * @author Bugee
 */
@Import(DelegatingWebSocketConfiguration.class)
@EnableConfigurationProperties(McpServerProperties.class)
@AutoConfigureAfter({WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class})
@Slf4j
public class MyWebSocketMcpServerConfiguration implements WebSocketConfigurer, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Bean
    public MyWebSocketServerTransportProvider webSocketServerTransportProvider() {
        return new MyWebSocketServerTransportProvider(JsonUtils.getObjectMapper(), "/mcp/message/ws");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        MyWebSocketServerTransportProvider myWebSocketServerTransportProvider = this.applicationContext.getBean(MyWebSocketServerTransportProvider.class);
        TokenProvider tokenProvider = this.applicationContext.getBean(TokenProvider.class);
        MyWebSocketHandler myWebSocketHandler = new MyWebSocketHandler(myWebSocketServerTransportProvider, tokenProvider);
        registry.addHandler(myWebSocketHandler, myWebSocketServerTransportProvider.getMessageEndpoint())
                .setHandshakeHandler(myWebSocketHandler)
                .setAllowedOrigins("*");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    public McpSyncServer websocketMcpServer(MyWebSocketServerTransportProvider webSocketServerTransportProvider,
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
        MySyncSpecification spec = new MySyncSpecification(webSocketServerTransportProvider)
                .serverInfo(serverName, serverVersion)
                .capabilities(capabilities);

        for (Map.Entry<String, ApiHandler> entry : aipHandlerManager.getApiHandlerMap().entrySet()) {
            if (entry.getValue().apiDefinition().mcpTool()) {
                spec.tools(MyMethodToolCallback.toSyncToolSpecification(entry.getKey(), entry.getValue()));
            }
        }

        log.info("start websocket {},{}", serverName, serverVersion);
        return spec.build();
    }
}
