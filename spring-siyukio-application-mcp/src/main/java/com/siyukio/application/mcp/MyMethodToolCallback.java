package com.siyukio.application.mcp;

import com.siyukio.tools.api.ApiException;
import com.siyukio.tools.api.ApiHandler;
import com.siyukio.tools.api.definition.ApiDefinition;
import com.siyukio.tools.api.token.Token;
import com.siyukio.tools.util.JsonUtils;
import com.siyukio.tools.util.OpenApiUtils;
import io.modelcontextprotocol.server.MyMcpSyncServerExchange;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
