package com.siyukio.application.mcp;

import com.siyukio.tools.api.ApiException;
import com.siyukio.tools.api.ApiHandler;
import com.siyukio.tools.api.definition.ApiDefinition;
import com.siyukio.tools.api.token.Token;
import com.siyukio.tools.util.JsonUtils;
import com.siyukio.tools.util.OpenApiUtils;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.MyMcpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author Bugee
 */
@Slf4j
public class MyMethodToolCallback implements ToolCallback {

    private final ToolDefinition toolDefinition;

    private final ApiHandler apiHandler;

    public MyMethodToolCallback(ToolDefinition toolDefinition, ApiHandler apiHandler) {
        this.toolDefinition = toolDefinition;
        this.apiHandler = apiHandler;
    }

    private static ToolCallback addTool(String path, ApiHandler apiHandler) {
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

    public static McpServerFeatures.SyncToolSpecification toSyncToolSpecification(String path, ApiHandler apiHandler) {
        ToolCallback toolCallback = addTool(path, apiHandler);
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

    @Override
    public ToolDefinition getToolDefinition() {
        return this.toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        return this.call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        ApiDefinition apiDefinition = this.apiHandler.apiDefinition();
        JSONObject requestJson = JsonUtils.parseObject(toolInput);
        String outputSchema = requestJson.optString("_outputSchema", "");
        if (StringUtils.hasText(outputSchema)) {
            JSONObject outputSchemaJson = OpenApiUtils.createObjectResponse(apiDefinition.responseParameters());
            JSONObject responseJson = new JSONObject();
            responseJson.put("outputSchema", outputSchemaJson);
            responseJson.put("sampling", apiDefinition.sampling());
            return JsonUtils.toJSONString(responseJson);
        }

        assert toolContext != null;
        Object exchange = toolContext.getContext().get(McpToolUtils.TOOL_CONTEXT_MCP_EXCHANGE_KEY);
        assert exchange != null;
        Token token = this.getToken(exchange);

        if (apiDefinition.authorization()) {
            //validate authorization
            if (!apiDefinition.roles().isEmpty()) {
                // validate role
                Set<String> roleSet = new HashSet<>(apiDefinition.roles());
                roleSet.retainAll(token.roles);
                if (roleSet.isEmpty()) {
                    throw new ApiException(HttpStatus.FORBIDDEN);
                }
            }
        }

        requestJson = this.apiHandler.requestValidator().validate(requestJson);

        List<Object> paramList = new ArrayList<>();
        paramList.add(exchange);
        if (token != null) {
            paramList.add(token);
        }

        Object[] params = paramList.toArray(new Object[0]);

        Object resultValue;
        try {
            resultValue = this.apiHandler.apiInvoker().invoke(requestJson, params);
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            log.error("callTool error:{}, {}", this.toolDefinition.name(), ex.getMessage());
            throw ApiException.getUnknownApiException(ex);
        } catch (InvocationTargetException ex) {
            Throwable throwable = ex.getTargetException();
            log.error("callTool error: {}, {}", this.toolDefinition.name(), throwable.getMessage());
            throw ApiException.getUnknownApiException(ex);
        }
        //
        Class<?> returnType = apiDefinition.realReturnType();
        //
        if (returnType == void.class) {
            JSONObject responseJson = new JSONObject();
            return JsonUtils.toJSONString(responseJson);
        }

        JSONObject resultJson = JsonUtils.copy(resultValue, JSONObject.class);

        this.apiHandler.responseFilter().filter(resultJson);

        return JsonUtils.toJSONString(resultJson);
    }

    private Token getToken(Object exchange) {
        if (exchange instanceof MyMcpSyncServerExchange myMcpSyncServerExchange) {
            return myMcpSyncServerExchange.getToken();
        }
        return null;
    }

}
