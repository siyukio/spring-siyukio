package com.siyukio.tools.api;

import com.siyukio.tools.api.definition.ApiDefinition;
import com.siyukio.tools.api.parameter.request.RequestValidator;
import com.siyukio.tools.api.parameter.response.ResponseFilter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Buddy
 */
@Slf4j
public class AipHandlerManager {

    private final Map<String, ApiHandler> apiHandlerMap = new HashMap<>();

    public void addApiHandler(ApiDefinition apiDefinition, Object bean, Method method) {
        RequestValidator requestValidator = RequestValidator.createRequestValidator(apiDefinition);
        ResponseFilter responseFilter = ResponseFilter.createResponseFilter(apiDefinition);
        ApiInvoker apiInvoker = new ApiInvoker(bean, method);
        ApiHandler apiHandler = ApiHandler.builder()
                .apiDefinition(apiDefinition)
                .responseFilter(responseFilter)
                .requestValidator(requestValidator)
                .apiInvoker(apiInvoker)
                .build();

        for (String path : apiDefinition.paths()) {
            this.apiHandlerMap.put(path, apiHandler);
            log.info("find api:{}, authorization:{}, signature:{}, roles:{}", path,
                    apiDefinition.authorization(),
                    apiDefinition.signature(),
                    apiDefinition.roles());
        }
    }

    public ApiHandler getApiHandler(String path) {
        return this.apiHandlerMap.get(path);
    }

    public Map<String, ApiHandler> getApiHandlerMap() {
        return Collections.unmodifiableMap(this.apiHandlerMap);
    }
}
