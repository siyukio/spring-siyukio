package io.github.siyukio.client.interceptor;

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
import java.util.zip.GZIPInputStream;

/**
 * Interceptor for handling gzip compressed HTTP responses.
 * Automatically decompresses response bodies when Content-Encoding header contains "gzip".
 *
 * @author Bugee
 */
public class GzipResponseInterceptor implements ClientHttpRequestInterceptor {

    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String GZIP = "gzip";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);

        String contentEncoding = response.getHeaders().getFirst(CONTENT_ENCODING);
        if (contentEncoding != null && contentEncoding.toLowerCase().contains(GZIP)) {
            return new GzipWrappedResponse(response);
        }

        return response;
    }

    /**
     * Wrapper for ClientHttpResponse that decompresses gzip content.
     */
    private static class GzipWrappedResponse implements ClientHttpResponse {

        private final ClientHttpResponse delegate;
        private InputStream body;

        GzipWrappedResponse(ClientHttpResponse delegate) {
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
                    GZIPInputStream gzipInputStream = new GZIPInputStream(originalBody);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    gzipInputStream.close();
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
