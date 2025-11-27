package io.github.siyukio.application.method;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.github.siyukio.tools.api.ApiRequest;
import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.definition.ApiDefinitionManager;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.XDataUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.springframework.core.MethodParameter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Buddy
 */
public final class RequestArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Class<?> parameterType = parameter.getParameterType();
        if (parameterType == ApiRequest.class || parameterType == JSONObject.class || parameter.hasParameterAnnotation(RequestBody.class)) {
            return true;
        }
        if (ApiDefinitionManager.isBasicType(parameterType) || parameterType.isArray() || Collection.class.isAssignableFrom(parameterType) || Token.class.isAssignableFrom(parameterType) || parameterType.getPackageName().startsWith("java.")) {
            return false;
        }
        return true;
    }

    private ApiRequest getApiRequest(HttpServletRequest httpServletRequest) {
        //parameter
        Map<String, String> simpleParameterMap = new HashMap<>();
        Map<String, String[]> parameterMap = httpServletRequest.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            simpleParameterMap.put(entry.getKey(), entry.getValue()[0]);
        }

        String body = String.valueOf(httpServletRequest.getAttribute(ApiConstants.ATTRIBUTE_REQUEST_BODY));
        String ip = httpServletRequest.getHeader("X-FORWARDED-FOR");
        if (!StringUtils.hasText(ip)) {
            ip = httpServletRequest.getRemoteAddr();
        }
        if (ip == null) {
            ip = "";
        }
        String[] ips = ip.split(",");
        if (ips.length > 1) {
            ip = ips[0].trim();
        }
        //ua
        String userAgent = httpServletRequest.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "";
        }
        return new ApiRequest(simpleParameterMap, ip, body, userAgent);
    }

    private String getRequestBody(HttpServletRequest httpServletRequest) throws IOException {
        Object obj = httpServletRequest.getAttribute(ApiConstants.ATTRIBUTE_REQUEST_BODY);
        String requestBody = null;
        if (obj != null) {
            requestBody = String.valueOf(obj);
        }
        if (!StringUtils.hasText(requestBody)) {
            ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(httpServletRequest);
            requestBody = new String(inputMessage.getBody().readAllBytes());
        }
        return requestBody;
    }

    private JSONObject getJSONObject(HttpServletRequest httpServletRequest) throws IOException {
        String requestBody = this.getRequestBody(httpServletRequest);
        if (!StringUtils.hasText(requestBody)) {
            ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(httpServletRequest);
            requestBody = new String(inputMessage.getBody().readAllBytes());
        }
        return XDataUtils.parseObject(requestBody);
    }

    private Object getRequestBodyObject(MethodParameter parameter, HttpServletRequest httpServletRequest) throws IOException {
        JSONObject requestBodyJson = this.getJSONObject(httpServletRequest);
        Type type = parameter.getGenericParameterType();
        JavaType javaType = TypeFactory.defaultInstance().constructType(type);
        return XDataUtils.copy(requestBodyJson, javaType);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Class<?> targetClass = parameter.getParameterType();
        HttpServletRequest httpServletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        assert httpServletRequest != null;
        if (targetClass == ApiRequest.class) {
            return this.getApiRequest(httpServletRequest);
        } else if (targetClass == JSONObject.class) {
            return this.getJSONObject(httpServletRequest);
        } else if (parameter.hasParameterAnnotation(RequestBody.class)) {
            return this.getRequestBodyObject(parameter, httpServletRequest);
        } else {
            Object requestBodyJson = httpServletRequest.getAttribute(ApiConstants.ATTRIBUTE_REQUEST_BODY_JSON);
            Type type = parameter.getGenericParameterType();
            JavaType javaType = TypeFactory.defaultInstance().constructType(type);
            return XDataUtils.copy(requestBodyJson, javaType);
        }
    }

}
