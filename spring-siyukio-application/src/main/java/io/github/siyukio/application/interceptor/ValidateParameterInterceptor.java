package io.github.siyukio.application.interceptor;

import io.github.siyukio.tools.api.AipHandlerManager;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.ApiHandler;
import io.github.siyukio.tools.api.ApiProfiles;
import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.parameter.request.RequestValidator;
import io.github.siyukio.tools.util.JsonUtils;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * @author Buddy
 */
@Slf4j
public final class ValidateParameterInterceptor implements HandlerInterceptor {

    private final AipHandlerManager aipHandlerManager;

    public ValidateParameterInterceptor(AipHandlerManager aipHandlerManager) {
        this.aipHandlerManager = aipHandlerManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        DispatcherType dispatcherType = request.getDispatcherType();
        if (dispatcherType == DispatcherType.ASYNC) {
            //If the type is ASYNC, the current validation will be skipped.
            return true;
        }

        if (!(handler instanceof HandlerMethod)) {
            //If it is not an API call, the current validation will be skipped.
            return true;
        }

        String apiPath = ApiProfiles.getApiPath(request.getRequestURI());
        ApiHandler apiHandler = this.aipHandlerManager.getApiHandler(apiPath);
        if (apiHandler == null) {
            return true;
        }

        //
        ServletServerHttpRequest inputMessage = new ServletServerHttpRequest(request);

        String requestBody = new String(inputMessage.getBody().readAllBytes());
        MediaType contentType = inputMessage.getHeaders().getContentType();
        if (contentType != null && contentType.equalsTypeAndSubtype(MediaType.APPLICATION_FORM_URLENCODED)) {
            log.warn("error path:{}", apiPath);
            log.warn("error Content-Type:{}", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
            log.warn("error requestBody:{}", requestBody);
            requestBody = URLDecoder.decode(requestBody, StandardCharsets.UTF_8);
            log.warn("error decode requestBody:{}", requestBody);
            //Compatible with application/x-www-form-urlencoded.
            if (requestBody.endsWith("=")) {
                //{}=
                requestBody = requestBody.substring(0, requestBody.length() - 1);
            }
        }
        JSONObject requestBodyJson;
        try {
            requestBodyJson = this.validate(requestBody, apiHandler.requestValidator());
        } catch (ApiException ex) {
            log.debug("request error:{},{},{}", ex.error, ex.message, ex.data);
            log.debug("request error content type:{}", request.getContentType());
            log.debug("request error body:{}", requestBody);
            throw ex;
        }

        request.setAttribute(ApiConstants.ATTRIBUTE_REQUEST_BODY, requestBody);
        request.setAttribute(ApiConstants.ATTRIBUTE_REQUEST_BODY_JSON, requestBodyJson);
        return true;
    }

    private JSONObject validate(String requestBody, RequestValidator requestValidator) {
        JSONObject requestBodyJson;
        try {
            requestBodyJson = JsonUtils.parseObject(requestBody);
        } catch (RuntimeException e) {
            throw ApiException.getInvalidApiException("requestBody to json error");
        }
        return requestValidator.validate(requestBodyJson);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
