package io.github.siyukio.application.interceptor;

import io.github.siyukio.tools.api.AipHandlerManager;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.ApiHandler;
import io.github.siyukio.tools.api.ApiProfiles;
import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
public final class ValidateAuthorizationInterceptor implements HandlerInterceptor {

    private final AipHandlerManager aipHandlerManager;

    private final TokenProvider tokenProvider;

    public ValidateAuthorizationInterceptor(AipHandlerManager aipHandlerManager, TokenProvider tokenProvider) {
        this.aipHandlerManager = aipHandlerManager;
        this.tokenProvider = tokenProvider;
    }

    private String getAuthorization(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            Object attr = request.getAttribute(HttpHeaders.AUTHORIZATION);
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
        ApiHandler apiHandler = this.aipHandlerManager.getApiHandler(path);
        if (apiHandler == null || !apiHandler.apiDefinition().authorization()) {
            return true;
        }

        Token token = null;
        String authorization = this.getAuthorization(request);
        if (StringUtils.hasText(authorization)) {
            token = this.tokenProvider.verifyToken(authorization);
        }

        if (token == null || token.refresh || token.expired) {
            throw new ApiException(HttpStatus.UNAUTHORIZED);
        }

        if (!apiHandler.apiDefinition().roles().isEmpty()) {
            Set<String> roleSet = new HashSet<>(apiHandler.apiDefinition().roles());

            roleSet.retainAll(token.roles);
            if (roleSet.size() != apiHandler.apiDefinition().roles().size()) {
                throw new ApiException(HttpStatus.FORBIDDEN);
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
