package io.github.siyukio.application.method;

import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;


/**
 * @author Buddy
 */
public final class ExceptionReturnValueHandler implements HandlerMethodReturnValueHandler {

    //RequestResponseBodyMethodProcessor
    private final HandlerMethodReturnValueHandler handlerMethodReturnValueHandler;

    public ExceptionReturnValueHandler(HandlerMethodReturnValueHandler requestResponseBodyMethodProcessor) {
        this.handlerMethodReturnValueHandler = requestResponseBodyMethodProcessor;
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return this.handlerMethodReturnValueHandler.supportsReturnType(returnType);
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        HttpServletRequest httpServletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        if (httpServletRequest == null) {
            this.handlerMethodReturnValueHandler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
            return;
        }

        if (returnValue instanceof JSONObject) {
            MediaType mediaType = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);
            Set<MediaType> mediaTypes = new HashSet<>();
            mediaTypes.add(mediaType);
            httpServletRequest.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
        }
        this.handlerMethodReturnValueHandler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
    }

}
