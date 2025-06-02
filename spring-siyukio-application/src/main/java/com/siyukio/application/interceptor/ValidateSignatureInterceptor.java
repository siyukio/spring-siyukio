package com.siyukio.application.interceptor;

import com.siyukio.tools.api.AipHandlerManager;
import com.siyukio.tools.api.ApiHandler;
import com.siyukio.tools.api.ApiProfiles;
import com.siyukio.tools.api.signature.SignatureProvider;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Buddy
 */
public final class ValidateSignatureInterceptor implements HandlerInterceptor {

    private final AipHandlerManager aipHandlerManager;

    private final SignatureProvider signatureProvider;

    public ValidateSignatureInterceptor(AipHandlerManager aipHandlerManager, SignatureProvider signatureProvider) {
        this.aipHandlerManager = aipHandlerManager;
        this.signatureProvider = signatureProvider;
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

        String path = ApiProfiles.getApiPath(request.getRequestURI());
        ApiHandler apiHandler = this.aipHandlerManager.getApiHandler(path);
        if (apiHandler == null || !apiHandler.apiDefinition.signature) {
            return true;
        }

        String ts = request.getHeader("timestamp");
        long timestamp;
        try {
            timestamp = Long.parseLong(ts);
        } catch (NumberFormatException ignored) {
            timestamp = 0;
        }
        String nonce = request.getHeader("nonce");
        String signature = request.getHeader("signature");

        this.signatureProvider.validate(timestamp, nonce, signature);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}
