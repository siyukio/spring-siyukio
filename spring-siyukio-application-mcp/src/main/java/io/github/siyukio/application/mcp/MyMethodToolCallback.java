package io.github.siyukio.application.mcp;

import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.ApiHandler;
import io.github.siyukio.tools.api.definition.ApiDefinition;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.JsonUtils;
import io.github.siyukio.tools.util.OpenApiUtils;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.MyMcpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author Bugee
 */
@Slf4j
public class MyMethodToolCallback {

    private final ApiHandler apiHandler;

    private final String name;

    public MyMethodToolCallback(ApiHandler apiHandler, String name) {
        this.apiHandler = apiHandler;
        this.name = name;
    }

    public static McpServerFeatures.SyncToolSpecification toSyncToolSpecification(String path, ApiHandler apiHandler) {
        String name = path;

        ApiDefinition apiDefinition = apiHandler.apiDefinition();
        String title = apiDefinition.summary();
        String description = apiDefinition.description();
        if (!StringUtils.hasText(description)) {
            description = title;
        }

        JSONObject inputSchemaJson = OpenApiUtils.createObjectRequest(apiDefinition.requestParameters());
        McpSchema.JsonSchema inputSchema = JsonUtils.copy(inputSchemaJson, McpSchema.JsonSchema.class);

        JSONObject outputSchemaJson = OpenApiUtils.createObjectResponse(apiDefinition.responseParameters());
        Map<String, Object> outputSchema = JsonUtils.copy(outputSchemaJson, Map.class, String.class, Object.class);


        MyMethodToolCallback toolCallback = new MyMethodToolCallback(apiHandler, name);

        var tool = McpSchema.Tool.builder()
                .name(name)
                .title(title)
                .description(description)
                .inputSchema(inputSchema)
                .outputSchema(outputSchema)
                .build();

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, req) -> {
                    try {
                        return toolCallback.call(exchange, req);
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
                }).build();
    }

    public McpSchema.CallToolResult call(McpSyncServerExchange exchange, McpSchema.CallToolRequest callToolRequest) {
        ApiDefinition apiDefinition = this.apiHandler.apiDefinition();
        JSONObject requestJson = JsonUtils.copy(callToolRequest.arguments(), JSONObject.class);

        Token token = this.getToken(exchange);

        if (apiDefinition.authorization()) {
            if (token == null) {
                ApiException exception = new ApiException(HttpStatus.UNAUTHORIZED);
                return new McpSchema.CallToolResult(List.of(), true, exception.toJson().toMap());
            }
            
            //validate authorization
            if (!apiDefinition.roles().isEmpty()) {
                // validate role
                Set<String> roleSet = new HashSet<>(apiDefinition.roles());
                roleSet.retainAll(token.roles);
                if (roleSet.isEmpty()) {
                    ApiException exception = new ApiException(HttpStatus.FORBIDDEN);
                    return new McpSchema.CallToolResult(List.of(), true, exception.toJson().toMap());
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
            log.error("callTool error:{}, {}", this.name, ex.getMessage());
            ApiException exception = ApiException.getUnknownApiException(ex);
            return new McpSchema.CallToolResult(List.of(), true, exception.toJson().toMap());
        } catch (InvocationTargetException ex) {
            Throwable throwable = ex.getTargetException();
            log.error("callTool error: {}, {}", this.name, throwable.getMessage());
            ApiException exception = ApiException.getUnknownApiException(ex);
            return new McpSchema.CallToolResult(List.of(), true, exception.toJson().toMap());
        }
        //
        Class<?> returnType = apiDefinition.realReturnType();
        //
        if (returnType == void.class) {
            return new McpSchema.CallToolResult(List.of(), false, Map.of());
        }

        JSONObject resultJson = JsonUtils.copy(resultValue, JSONObject.class);

        this.apiHandler.responseFilter().filter(resultJson);

        return new McpSchema.CallToolResult(List.of(), false, resultJson.toMap());
    }

    private Token getToken(Object exchange) {
        if (exchange instanceof MyMcpSyncServerExchange myMcpSyncServerExchange) {
            return myMcpSyncServerExchange.getToken();
        }
        return null;
    }

}
