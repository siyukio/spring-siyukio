package com.siyukio.application.boot.starter.autoconfigure;


import com.siyukio.application.mcp.MyMethodToolCallback;
import com.siyukio.tools.api.AipHandlerManager;
import com.siyukio.tools.api.ApiException;
import com.siyukio.tools.api.ApiHandler;
import com.siyukio.tools.api.definition.ApiDefinition;
import com.siyukio.tools.api.token.TokenProvider;
import com.siyukio.tools.util.JsonUtils;
import com.siyukio.tools.util.OpenApiUtils;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.MySyncSpecification;
import io.modelcontextprotocol.server.transport.MyWebMvcSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bugee
 */
@Slf4j
@EnableConfigurationProperties(McpServerProperties.class)
@AutoConfigureAfter({MyApplicationConfiguration.class})
public class MySseMcpServerConfiguration {

    private ToolCallback addTool(String path, ApiHandler apiHandler) {
        ApiDefinition apiDefinition = apiHandler.apiDefinition();
        String description = apiDefinition.summary();
        if (StringUtils.hasText(apiDefinition.description())) {
            description += System.lineSeparator() + apiDefinition.description();
        }

        JSONObject inputSchemaJson = OpenApiUtils.createObjectRequest(apiDefinition.requestParameters());
        ToolDefinition toolDefinition = new DefaultToolDefinition(path,
                description,
                JsonUtils.toJSONString(inputSchemaJson));

        return new MyMethodToolCallback(toolDefinition, apiHandler);
    }

    private McpServerFeatures.SyncToolSpecification toSyncToolSpecification(ToolCallback toolCallback) {
        var tool = new McpSchema.Tool(toolCallback.getToolDefinition().name(),
                toolCallback.getToolDefinition().description(), toolCallback.getToolDefinition().inputSchema());

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, args) -> {
            Map<String, Object> context = new HashMap<>();
            context.put(McpToolUtils.TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange);
            ToolContext tooContext = new ToolContext(context);
            try {
                String callResult = toolCallback.call(ModelOptionsUtils.toJsonString(args), tooContext);
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(callResult)), false);
            } catch (ApiException ex) {
                JSONObject responseJson = ex.toJson();
                String text = JsonUtils.toJSONString(responseJson);
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), true);
            } catch (Exception e) {
                ApiException apiException = ApiException.getUnknownApiException(e);
                JSONObject responseJson = apiException.toJson();
                String text = JsonUtils.toJSONString(responseJson);
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), true);
            }
        });
    }

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
                spec.tools(this.toSyncToolSpecification(this.addTool(entry.getKey(), entry.getValue())));
            }
        }

        log.info("start sse {}, {}", serverName, serverVersion);
        return spec.build();
    }
}
