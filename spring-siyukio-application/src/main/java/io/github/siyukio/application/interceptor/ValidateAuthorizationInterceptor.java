package io.github.siyukio.application.interceptor;

import io.github.siyukio.tools.api.AipHandlerManager;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.ApiHandler;
import io.github.siyukio.tools.api.ApiProfiles;
import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.definition.ApiDefinition;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashSet;
import java.util.Set;

/**
 * This interceptor used for validating authorization in incoming requests.
 * support accessToken and token in query string.
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

    private String getAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            Object attr = request.getAttribute(HttpHeaders.AUTHORIZATION);
            if (attr != null) {
                authorization = attr.toString();
            }
        }
        if (!StringUtils.hasText(authorization)) {
            String query = request.getQueryString();
            if (StringUtils.hasText(query)) {
                authorization = request.getParameter("accessToken");
                if (!StringUtils.hasText(authorization)) {
                    authorization = request.getParameter("token");
                }
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
        if (apiHandler == null) {
            return true;
        }

        ApiDefinition.Authorization authorization = apiHandler.apiDefinition().authorization();
        if (authorization == null) {
            return true;
        }

        Token token = null;
        String accessToken = this.getAccessToken(request);
        if (StringUtils.hasText(accessToken)) {
            token = this.tokenProvider.verifyToken(accessToken);
        }

        if (token == null || token.principal() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED);
        }

        if (token.type() == null || token.type().equals(Token.Type.REFRESH)) {
            throw new ApiException(HttpStatus.FORBIDDEN);
        }

        Token.Principal principal = token.principal();
        if (!authorization.type().equals(principal.type())) {
            throw new ApiException(HttpStatus.FORBIDDEN);
        }

        if (StringUtils.hasText(authorization.actorType())) {
            Token.Principal actor = token.actor();
            if (actor == null || !authorization.actorType().equals(actor.type())) {
                throw new ApiException(HttpStatus.FORBIDDEN);
            }
        }

        if (!CollectionUtils.isEmpty(authorization.scopes())) {
            Set<String> scopeSet = new HashSet<>(authorization.scopes());

            if (!CollectionUtils.isEmpty(principal.scopes())) {
                scopeSet.retainAll(principal.scopes());
            }

            if (scopeSet.isEmpty()) {
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
