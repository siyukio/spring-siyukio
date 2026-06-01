package io.github.siyukio.client.interceptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Local JSON response implementation of ClientHttpResponse.
 *
 * @author Bugee
 */
public class LocalJsonResponse implements ClientHttpResponse {

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
