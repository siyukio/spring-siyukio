package com.siyukio.application.boot.starter.autoconfigure;


import com.siyukio.application.mcp.MyMethodToolCallback;
import com.siyukio.tools.api.AipHandlerManager;
import com.siyukio.tools.api.ApiHandler;
import com.siyukio.tools.api.token.TokenProvider;
import com.siyukio.tools.util.JsonUtils;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.MySyncSpecification;
import io.modelcontextprotocol.server.transport.MyWebMvcSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

/**
 * @author Bugee
 */
@Slf4j
@EnableConfigurationProperties(McpServerProperties.class)
@AutoConfigureAfter({MyApplicationConfiguration.class})
public class MySseMcpServerConfiguration {

    // SSE transport WebMvcSseServerTransportProvider
    @Bean
    public MyWebMvcSseServerTransportProvider webMvcSseServerTransportProvider(TokenProvider tokenProvider,
                                                                               McpServerProperties mcpServerProperties) {
        return new MyWebMvcSseServerTransportProvider(tokenProvider, JsonUtils.getObjectMapper(),
                mcpServerProperties.getSseMessageEndpoint(), mcpServerProperties.getSseEndpoint());
    }

    @Bean
    public RouterFunction<ServerResponse> mcpRouterFunction(MyWebMvcSseServerTransportProvider webMvcSseServerTransportProvider) {
        return webMvcSseServerTransportProvider.getRouterFunction();
    }

    @Bean
    public McpSyncServer webMvcSseMcpServer(MyWebMvcSseServerTransportProvider webMvcSseServerTransportProvider,
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
        MySyncSpecification spec = new MySyncSpecification(webMvcSseServerTransportProvider)
                .serverInfo(serverName, serverVersion)
                .capabilities(capabilities);


        for (Map.Entry<String, ApiHandler> entry : aipHandlerManager.getApiHandlerMap().entrySet()) {
            if (entry.getValue().apiDefinition().mcpTool()) {
                spec.tools(MyMethodToolCallback.toSyncToolSpecification(entry.getKey(), entry.getValue()));
            }
        }

        log.info("start sse {}, {}", serverName, serverVersion);
        return spec.build();
    }
}
