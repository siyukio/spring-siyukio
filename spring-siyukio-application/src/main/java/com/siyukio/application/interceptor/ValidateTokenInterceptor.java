package com.siyukio.application.interceptor;

import com.siyukio.tools.api.ApiError;
import com.siyukio.tools.api.ApiException;
import com.siyukio.tools.api.ApiProfiles;
import com.siyukio.tools.api.constants.ApiConstants;
import com.siyukio.tools.api.definition.ApiDefinition;
import com.siyukio.tools.api.definition.ApiDefinitionManager;
import com.siyukio.tools.api.token.Token;
import com.siyukio.tools.api.token.TokenProvider;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashSet;
import java.util.Set;

/**
 * This interceptor used for validating authorization in incoming requests.
 *
 * @author Buddy
 */
public final class ValidateTokenInterceptor implements HandlerInterceptor {

    private final ApiDefinitionManager apiDefinitionManager;

    private final TokenProvider tokenProvider;

    public ValidateTokenInterceptor(ApiDefinitionManager apiDefinitionManager, TokenProvider tokenProvider) {
        this.apiDefinitionManager = apiDefinitionManager;
        this.tokenProvider = tokenProvider;
    }

    private String getAuthorization(HttpServletRequest request) {
        String authorization = request.getHeader(ApiConstants.AUTHORIZATION);
        if (authorization == null) {
            Object attr = request.getAttribute(ApiConstants.AUTHORIZATION);
            if (attr != null) {
                authorization = attr.toString();
            }
        }
        return authorization;
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
        //
        String path = ApiProfiles.getApiPath(request.getRequestURI());
        ApiDefinition apiDefinition = this.apiDefinitionManager.getApiDefinition(path);
        if (apiDefinition == null || !apiDefinition.authorization) {
            return true;
        }

        Token token = null;
        String authorization = this.getAuthorization(request);
        if (StringUtils.hasText(authorization)) {
            token = this.tokenProvider.verifyToken(authorization);
        }

        if (token == null || token.refreshing || token.expired) {
            throw new ApiException(ApiError.AUTHORIZED_ERROR);
        }

        if (!apiDefinition.roles.isEmpty()) {
            Set<String> roleSet = new HashSet<>(apiDefinition.roles);

            roleSet.retainAll(token.roles);
            if (roleSet.size() != apiDefinition.roles.size()) {
                throw new ApiException(ApiError.FORBIDDEN_ROLE);
            }
        }

        request.setAttribute(ApiConstants.ATTRIBUTE_TOKEN, token);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
