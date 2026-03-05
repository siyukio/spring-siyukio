package io.github.siyukio.client.interceptor;

import org.brotli.dec.BrotliInputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Interceptor for handling Brotli compressed HTTP responses.
 * Automatically decompresses response bodies when Content-Encoding header contains "br".
 *
 * @author Bugee
 */
public class BrotliResponseInterceptor implements ClientHttpRequestInterceptor {

    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String BR = "br";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);

        String contentEncoding = response.getHeaders().getFirst(CONTENT_ENCODING);
        if (contentEncoding != null && contentEncoding.toLowerCase().contains(BR)) {
            return new BrotliWrappedResponse(response);
        }

        return response;
    }

    /**
     * Wrapper for ClientHttpResponse that decompresses Brotli content.
     */
    private static class BrotliWrappedResponse implements ClientHttpResponse {

        private final ClientHttpResponse delegate;
        private InputStream body;

        BrotliWrappedResponse(ClientHttpResponse delegate) {
            this.delegate = delegate;
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(delegate.getHeaders());
            headers.remove(CONTENT_ENCODING);
            return headers;
        }

        @Override
        public InputStream getBody() throws IOException {
            if (body == null) {
                InputStream originalBody = delegate.getBody();
                try {
                    BrotliInputStream brotliInputStream = new BrotliInputStream(originalBody);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = brotliInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    brotliInputStream.close();
                    body = new ByteArrayInputStream(outputStream.toByteArray());
                } catch (IOException e) {
                    originalBody.close();
                    throw e;
                }
            }
            return body;
        }

        @Override
        public void close() {
            delegate.close();
            if (body != null) {
                try {
                    body.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
