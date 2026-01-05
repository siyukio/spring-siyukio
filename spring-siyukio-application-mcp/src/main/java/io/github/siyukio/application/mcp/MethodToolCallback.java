package io.github.siyukio.application.mcp;

import io.github.siyukio.tools.api.AipHandlerManager;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.ApiHandler;
import io.github.siyukio.tools.api.definition.ApiDefinition;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.XDataUtils;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author Bugee
 */
@Slf4j
public class MethodToolCallback {

    private static final List<McpServerFeatures.SyncToolSpecification> SYNC_TOOL_SPECIFICATIONS_CACHES = new ArrayList<>();

    private final ApiHandler apiHandler;
    private final String name;

    public MethodToolCallback(ApiHandler apiHandler, String name) {
        this.apiHandler = apiHandler;
        this.name = name;
    }

    public static List<McpServerFeatures.SyncToolSpecification> getSyncToolSpecifications(AipHandlerManager aipHandlerManager) {
        if (SYNC_TOOL_SPECIFICATIONS_CACHES.isEmpty()) {
            for (Map.Entry<String, ApiHandler> entry : aipHandlerManager.getApiHandlerMap().entrySet()) {
                if (entry.getValue().apiDefinition().mcpTool()) {
                    SYNC_TOOL_SPECIFICATIONS_CACHES.add(MethodToolCallback.toSyncToolSpecification(entry.getKey(), entry.getValue()));
                }
            }
        }
        return Collections.unmodifiableList(SYNC_TOOL_SPECIFICATIONS_CACHES);
    }

    private static McpSchema.CallToolResult createCallToolResult(Boolean isError, JSONObject responseJson) {
        // CallToolResult content collection cannot be empty,
        // otherwise it will automatically convert structuredContent to content during output,
        // causing duplicate JSON output.
        // see StructuredOutputCallToolHandler.java
        return McpSchema.CallToolResult.builder().isError(isError)
                .structuredContent(responseJson.toMap())
                .addTextContent("").build();
    }

    public static McpServerFeatures.SyncToolSpecification toSyncToolSpecification(String path, ApiHandler apiHandler) {
        String name = path;
        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        name = name.replaceAll("/", ".");

        ApiDefinition apiDefinition = apiHandler.apiDefinition();
        String title = apiDefinition.summary();
        String description = apiDefinition.description();
        if (!StringUtils.hasText(description)) {
            description = title;
        }

        McpSchema.JsonSchema inputSchema = XDataUtils.copy(apiDefinition.requestBodyParameter().schema(), McpSchema.JsonSchema.class);

        Map<String, Object> outputSchema = XDataUtils.copy(apiDefinition.responseBodyParameter().schema(), Map.class, String.class, Object.class);

        MethodToolCallback toolCallback = new MethodToolCallback(apiHandler, name);

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
                        String text = XDataUtils.toJSONString(responseJson);
                        return new McpSchema.CallToolResult(text, true);
                    } catch (Exception e) {
                        ApiException apiException = ApiException.getUnknownApiException(e);
                        JSONObject responseJson = apiException.toJson();
                        String text = XDataUtils.toJSONString(responseJson);
                        return new McpSchema.CallToolResult(text, true);
                    }
                }).build();
    }

    public McpSchema.CallToolResult call(McpSyncServerExchange exchange, McpSchema.CallToolRequest callToolRequest) {
        ApiDefinition apiDefinition = this.apiHandler.apiDefinition();
        JSONObject requestJson = XDataUtils.copy(callToolRequest.arguments(), JSONObject.class);
        Token token = this.getToken(exchange);

        if (apiDefinition.authorization()) {
            if (token == null) {
                ApiException exception = new ApiException(HttpStatus.UNAUTHORIZED);
                return createCallToolResult(true, exception.toJson());
            }

            //validate authorization
            if (!apiDefinition.roles().isEmpty()) {
                // validate role
                Set<String> roleSet = new HashSet<>(apiDefinition.roles());
                if (!CollectionUtils.isEmpty(token.roles())) {
                    roleSet.retainAll(token.roles());
                }
                if (roleSet.isEmpty()) {
                    ApiException exception = new ApiException(HttpStatus.FORBIDDEN);
                    return createCallToolResult(true, exception.toJson());
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
            return createCallToolResult(true, exception.toJson());
        } catch (InvocationTargetException ex) {
            Throwable throwable = ex.getTargetException();
            log.error("callTool error: {}, {}", this.name, throwable.getMessage());
            ApiException exception = ApiException.getUnknownApiException(ex);
            return createCallToolResult(true, exception.toJson());
        }
        //
        Class<?> returnType = apiDefinition.realReturnType();
        //
        if (returnType == void.class) {
            return createCallToolResult(false, new JSONObject());
        }

        JSONObject resultJson = XDataUtils.copy(resultValue, JSONObject.class);

        this.apiHandler.responseFilter().filter(resultJson);

        return createCallToolResult(false, resultJson);
    }

    private Token getToken(McpSyncServerExchange exchange) {
        Object obj = exchange.transportContext().get(HttpHeaders.AUTHORIZATION);
        if (obj instanceof Token token) {
            return token;
        }
        return null;
    }

}
