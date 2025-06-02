package com.siyukio.tools.api;

import com.siyukio.tools.api.definition.ApiDefinition;
import com.siyukio.tools.api.parameter.request.RequestValidator;
import com.siyukio.tools.api.parameter.response.ResponseFilter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Buddy
 */
public class AipHandlerManager {

    private final Map<String, ApiHandler> apiHandlerMap = new HashMap<>();

    public void addApiHandler(ApiDefinition apiDefinition, Object bean, Method method) {
        ApiHandler apiHandler = new ApiHandler();
        apiHandler.apiDefinition = apiDefinition;
        apiHandler.requestValidator = RequestValidator.createRequestValidator(apiDefinition);
        apiHandler.responseFilter = ResponseFilter.createResponseFilter(apiDefinition);
        apiHandler.apiInvoker = new ApiInvoker(bean, method);

        for (String path : apiDefinition.paths) {
            this.apiHandlerMap.put(path, apiHandler);
        }
    }

    public ApiHandler getApiHandler(String path) {
        return this.apiHandlerMap.get(path);
    }
}
