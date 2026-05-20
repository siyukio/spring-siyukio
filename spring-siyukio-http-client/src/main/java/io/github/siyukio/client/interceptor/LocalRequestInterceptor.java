package io.github.siyukio.client.interceptor;

import io.github.siyukio.tools.api.AipHandlerManager;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.ApiHandler;
import io.github.siyukio.tools.api.ApiRequest;
import io.github.siyukio.tools.api.definition.ApiDefinition;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * Interceptor that intercepts requests to localhost and 127.0.0.1,
 * returning local JSON responses instead of executing the actual request.
 *
 * @author Bugee
 */
@Slf4j
public class LocalRequestInterceptor implements ClientHttpRequestInterceptor {

    private final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1");
    private final AipHandlerManager aipHandlerManager;
    private final TokenProvider tokenProvider;

    public LocalRequestInterceptor(AipHandlerManager aipHandlerManager, TokenProvider tokenProvider) {
        this.aipHandlerManager = aipHandlerManager;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public ClientHttpResponse intercept(org.springframework.http.HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        URI uri = request.getURI();
        String host = uri.getHost();

        if (isLocalHost(host)) {
            String path = uri.getPath();
            ApiHandler apiHandler = this.aipHandlerManager.getApiHandler(path);
            if (apiHandler != null) {
                log.debug("LocalHttpClient request: {} {}", request.getMethod(), uri);
                Token token = null;
                ApiDefinition apiDefinition = apiHandler.apiDefinition();
                if (apiDefinition.authorization()) {
                    String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    if (!StringUtils.hasText(authorization)) {
                        ApiException apiException = new ApiException(HttpStatus.UNAUTHORIZED);
                        return new LocalJsonResponse(XDataUtils.toJSONString(apiException.toJson()));
                    }
                    token = this.tokenProvider.verifyToken(authorization);
                    if (token == null) {
                        ApiException apiException = new ApiException(HttpStatus.UNAUTHORIZED);
                        return new LocalJsonResponse(XDataUtils.toJSONString(apiException.toJson()));
                    }
                }
                ApiRequest apiRequest = ApiRequest.builder()
                        .userAgent("HTTPClient")
                        .ip("127.0.0.1")
                        .parameters(Map.of())
                        .headers(Map.of())
                        .body(new String(body, StandardCharsets.UTF_8))
                        .build();
                return createLocalResponse(apiHandler, token, apiRequest);
            }
        }
        return execution.execute(request, body);
    }

    /**
     * Checks if the host is a local host.
     */
    private boolean isLocalHost(String host) {
        if (host == null) {
            return false;
        }
        return LOCAL_HOSTS.contains(host.toLowerCase());
    }

    /**
     * Creates a local ClientHttpResponse.
     */
    private ClientHttpResponse createLocalResponse(ApiHandler apiHandler, Token token, ApiRequest apiRequest) {
        JSONObject requestJson = XDataUtils.parse(apiRequest.body(), JSONObject.class);
        requestJson = apiHandler.requestValidator().validate(requestJson);
        Object[] params;
        if (token == null) {
            params = new Object[]{apiRequest};
        } else {
            params = new Object[]{apiRequest, token};
        }

        ApiDefinition apiDefinition = apiHandler.apiDefinition();
        Object resultValue;
        try {
            resultValue = apiHandler.apiInvoker().invoke(requestJson, params);
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            log.error("LocalHttpClient error:{}, {}", apiDefinition.paths(), ex.getMessage());
            throw ApiException.getUnknownApiException(ex);
        } catch (InvocationTargetException ex) {
            Throwable throwable = ex.getTargetException();
            log.error("LocalHttpClient error: {}, {}", apiDefinition.paths(), throwable.getMessage());
            throw ApiException.getUnknownApiException(throwable);
        }

        Class<?> returnType = apiDefinition.realReturnType();
        String result;
        if (returnType == void.class || returnType == Void.class) {
            result = "{}";
        } else {
            result = XDataUtils.toJSONString(resultValue);
        }
        return new LocalJsonResponse(result);
    }

    /**
     * Local JSON response implementation of ClientHttpResponse.
     */
    private static class LocalJsonResponse implements ClientHttpResponse {

        private final byte[] bodyBytes;
        private final HttpStatusCode statusCode = HttpStatusCode.valueOf(200);
        private final HttpHeaders headers;

        LocalJsonResponse(String jsonBody) {
            this.bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            this.headers = new HttpHeaders();
            this.headers.setContentType(MediaType.APPLICATION_JSON);
            this.headers.setContentLength(bodyBytes.length);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(bodyBytes);
        }

        @Override
        public HttpStatusCode getStatusCode() {
            return statusCode;
        }

        @Override
        public String getStatusText() {
            return statusCode.toString();
        }

        @Override
        public void close() {
            // no-op for mock response
        }
    }
}
