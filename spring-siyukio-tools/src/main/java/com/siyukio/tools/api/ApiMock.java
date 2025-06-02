package com.siyukio.tools.api;

import com.siyukio.tools.api.token.Token;
import com.siyukio.tools.util.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Buddy
 */
@Setter
@Getter
@Slf4j
public class ApiMock {

    private Token token;

    @Autowired
    private AipHandlerManager aipHandlerManager;

    public JSONObject perform(String path, Object request) {
        JSONObject requestJson = JsonUtils.copy(request, JSONObject.class);

        ApiHandler apiHandler = this.aipHandlerManager.getApiHandler(path);
        if (apiHandler == null) {
            throw new ApiException(ApiError.NOT_FOUND);
        }

        if (apiHandler.apiDefinition.authorization) {
            if (token == null) {
                throw new ApiException(ApiError.AUTHORIZED_ERROR);
            }

            if (!apiHandler.apiDefinition.roles.isEmpty()) {
                Set<String> roleSet = new HashSet<>(apiHandler.apiDefinition.roles);

                roleSet.retainAll(token.roles);
                if (roleSet.isEmpty()) {
                    throw new ApiException(ApiError.FORBIDDEN_ROLE);
                }
            }
        }

        requestJson = apiHandler.requestValidator.validate(requestJson);

        List<Object> paramList = new ArrayList<>();
        if (token != null) {
            paramList.add(token);
        }

        Object[] params = paramList.toArray(new Object[0]);
        Object resultValue;
        try {
            resultValue = apiHandler.apiInvoker.invoke(requestJson, params);
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            throw ApiException.getUnknownApiException(ex);
        } catch (InvocationTargetException ex) {
            Throwable throwable = ex.getTargetException();
            throw ApiException.getUnknownApiException(throwable);
        }

        Class<?> returnType = apiHandler.apiDefinition.realReturnType;

        if (returnType == void.class) {
            return new JSONObject();
        }

        JSONObject resultJson = JsonUtils.copy(resultValue, JSONObject.class);

        apiHandler.responseFilter.filter(resultJson);
        return resultJson;
    }
}
