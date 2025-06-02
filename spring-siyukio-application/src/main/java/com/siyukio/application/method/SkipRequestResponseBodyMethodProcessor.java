package com.siyukio.application.method;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author Buddy
 */
public class SkipRequestResponseBodyMethodProcessor implements HandlerMethodArgumentResolver {

    //default is RequestResponseBodyMethodProcessor
    private final HandlerMethodArgumentResolver handlerMethodArgumentResolver;

    public SkipRequestResponseBodyMethodProcessor(HandlerMethodArgumentResolver handlerMethodArgumentResolver) {
        this.handlerMethodArgumentResolver = handlerMethodArgumentResolver;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return false;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        return this.handlerMethodArgumentResolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    }

}
