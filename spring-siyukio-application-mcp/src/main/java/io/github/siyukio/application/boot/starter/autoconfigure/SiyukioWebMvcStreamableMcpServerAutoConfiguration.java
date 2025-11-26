package io.github.siyukio.application.boot.starter.autoconfigure;


import io.github.siyukio.application.mcp.MethodToolCallback;
import io.github.siyukio.tools.api.AipHandlerManager;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bugee
 */
@Slf4j
@EnableConfigurationProperties(SiyukioMcpServerProperties.class)
@AutoConfigureAfter({SiyukioApplicationAutoConfiguration.class})
public class SiyukioWebMvcStreamableMcpServerAutoConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Bean
    public WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider(SiyukioMcpServerProperties siyukioMcpServerProperties) {
        TokenProvider tokenProvider = this.applicationContext.getBean(TokenProvider.class);
        return WebMvcStreamableServerTransportProvider.builder()
                .objectMapper(JsonUtils.getObjectMapper())
                .mcpEndpoint(siyukioMcpServerProperties.getMcpEndpoint())
                .keepAliveInterval(Duration.ofSeconds(20))
//                .disallowDelete(true)
                .contextExtractor((serverRequest) -> {
                    Map<String, Object> metadata = new HashMap<>();
                    String authorization = serverRequest.headers().firstHeader(HttpHeaders.AUTHORIZATION);
                    Token token = tokenProvider.verifyToken(authorization);
                    if (token != null && !token.refresh() && !token.expired()) {
                        metadata.put(HttpHeaders.AUTHORIZATION, token);
                    }

                    return McpTransportContext.create(metadata);
                })
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> mcpRouterFunction(WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider) {
        return webMvcStreamableServerTransportProvider.getRouterFunction();
    }

    @Bean
    public McpSyncServer webMvcStreamableMcpServer(WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider,
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
        McpServer.SyncSpecification<McpServer.StreamableSyncSpecification> spec = McpServer.sync(webMvcStreamableServerTransportProvider)
                .serverInfo(serverName, serverVersion)
                .requestTimeout(siyukioMcpServerProperties.getRequestTimeout())
                .capabilities(capabilities);

        List<McpServerFeatures.SyncToolSpecification> tools = MethodToolCallback.getSyncToolSpecifications(aipHandlerManager);
        spec.tools(tools);

        log.info("start http streamable mcp server {}, {}", serverName, serverVersion);
        return spec.build();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
