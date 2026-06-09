package io.github.siyukio.client.interceptor;

import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.util.XDataUtils;
import org.json.JSONObject;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Interceptor for unified error response handling.
 * Checks if response contains an error object and throws ApiException accordingly.
 */
public class UnifiedErrorAfterRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);

        // Check if response is JSON by Content-Type header
        if (!isJsonResponse(response)) {
            return response;
        }

        // Read response body as string
        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);

        // Parse JSON and check for error
        JSONObject jsonObject = XDataUtils.parse(responseBody, JSONObject.class);
        JSONObject errorObject = jsonObject.optJSONObject("error");

        // If error object exists, throw ApiException
        if (errorObject != null) {
            int code = errorObject.optInt("code");
            String message = errorObject.optString("message");
            throw new ApiException(code, message);
        }

        // Return original response if no error found
        return new StringResponseWrapper(response, responseBody);
    }

    /**
     * Check if the response is JSON by Content-Type header.
     */
    private boolean isJsonResponse(ClientHttpResponse response) throws IOException {
        MediaType contentType = response.getHeaders().getContentType();
        return contentType != null && contentType.equals(MediaType.APPLICATION_JSON);
    }

}
