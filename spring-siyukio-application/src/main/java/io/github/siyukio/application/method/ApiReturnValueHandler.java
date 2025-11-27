package io.github.siyukio.application.method;

import io.github.siyukio.tools.api.AipHandlerManager;
import io.github.siyukio.tools.api.ApiHandler;
import io.github.siyukio.tools.api.ApiProfiles;
import io.github.siyukio.tools.util.XDataUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;


/**
 * @author Buddy
 */
public final class ApiReturnValueHandler implements HandlerMethodReturnValueHandler {

    private final AipHandlerManager aipHandlerManager;

    //RequestResponseBodyMethodProcessor
    private final HandlerMethodReturnValueHandler handlerMethodReturnValueHandler;

    public ApiReturnValueHandler(AipHandlerManager aipHandlerManager, HandlerMethodReturnValueHandler requestResponseBodyMethodProcessor) {
        this.aipHandlerManager = aipHandlerManager;
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
        String apiPath = ApiProfiles.getApiPath(httpServletRequest.getRequestURI());
        ApiHandler apiHandler = this.aipHandlerManager.getApiHandler(apiPath);
        if (apiHandler == null) {
            if (returnValue instanceof JSONObject) {
                MediaType mediaType = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);
                Set<MediaType> mediaTypes = new HashSet<>();
                mediaTypes.add(mediaType);
                httpServletRequest.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
            }
            this.handlerMethodReturnValueHandler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
            return;
        }

        if (apiHandler.apiDefinition().realReturnType() == String.class) {

            MediaType mediaType = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);
            String fileName = httpServletRequest.getParameter("fileName");
            String text = String.valueOf(returnValue);
            boolean fileError = text.startsWith("fileError:");
            if (fileError) {
                returnValue = text.replace("fileError:", "");
                returnValue = "<p style=\"font-size: 4em;\">" + returnValue + "</p>";

                mediaType = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);
            }
            if (StringUtils.hasText(fileName) && !fileError) {
                HttpServletResponse httpServletResponse = webRequest.getNativeResponse(HttpServletResponse.class);
                fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                assert httpServletResponse != null;
                httpServletResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
            }
            Set<MediaType> mediaTypes = new HashSet<>();
            mediaTypes.add(mediaType);
            httpServletRequest.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
            this.handlerMethodReturnValueHandler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
            return;
        }
        MediaType mediaType = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);
        Set<MediaType> mediaTypes = new HashSet<>();
        mediaTypes.add(mediaType);
        httpServletRequest.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);


        if (apiHandler.apiDefinition().returnType() == void.class || apiHandler.apiDefinition().realReturnType() == Void.class) {
            JSONObject responseJson = new JSONObject();
            this.handlerMethodReturnValueHandler.handleReturnValue(responseJson, returnType, mavContainer, webRequest);
            return;
        }

        JSONObject resultJson = XDataUtils.copy(returnValue, JSONObject.class);

        apiHandler.responseFilter().filter(resultJson);
        this.handlerMethodReturnValueHandler.handleReturnValue(resultJson, returnType, mavContainer, webRequest);
    }

}
