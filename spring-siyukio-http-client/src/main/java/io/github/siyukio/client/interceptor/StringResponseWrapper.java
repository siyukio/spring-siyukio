package io.github.siyukio.client.interceptor;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Response wrapper that returns the original string as body.
 *
 * @author Bugee
 */
public class StringResponseWrapper implements ClientHttpResponse {
    private final ClientHttpResponse originalResponse;
    private final byte[] bodyBytes;

    StringResponseWrapper(ClientHttpResponse originalResponse, String body) {
        this.originalResponse = originalResponse;
        this.bodyBytes = body.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public org.springframework.http.HttpHeaders getHeaders() {
        return originalResponse.getHeaders();
    }

    @Override
    public InputStream getBody() {
        return new ByteArrayInputStream(bodyBytes);
    }

    @Override
    public HttpStatusCode getStatusCode() throws IOException {
        return originalResponse.getStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return originalResponse.getStatusText();
    }

    @Override
    public void close() {
        originalResponse.close();
    }
}
