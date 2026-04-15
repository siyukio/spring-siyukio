package io.github.siyukio.client.interceptor;

import io.github.siyukio.tools.util.HttpClientUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Request interceptor that resolves domain names to IP addresses using custom DNS
 * and randomly selects an IP for each request.
 *
 * @author Bugee
 */
@Slf4j
public class LoadBalanceInterceptor implements ClientHttpRequestInterceptor {

    private final Lock lock = new ReentrantLock();
    private volatile List<String> resolvedIps = new ArrayList<>();
    private volatile long lastResolvedTime = 0L;

    private void ensureResolved(String host) {
        lock.lock();
        if (System.currentTimeMillis() - this.lastResolvedTime < 15000) {
            return;
        }
        try {
            this.resolvedIps = HttpClientUtils.resolveDomain(host);
            this.lastResolvedTime = System.currentTimeMillis();
            log.debug("Resolved {} ips: {}", host, this.resolvedIps);
        } finally {
            lock.unlock();
        }
    }

    private String selectRandomIp(String host) {
        ensureResolved(host);
        if (resolvedIps.isEmpty()) {
            return "";
        }
        if (resolvedIps.size() == 1) {
            return resolvedIps.getFirst();
        }
        int index = ThreadLocalRandom.current().nextInt(resolvedIps.size());
        String ip = resolvedIps.get(index);
        log.debug("Selected ip: {}, {}, {}, {}", host, ip, index, resolvedIps);
        return ip;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        URI originalUri = request.getURI();
        String host = originalUri.getHost();
        String schema = originalUri.getScheme();
        if (StringUtils.hasText(host) && "http".equals(schema)) {
            String selectedIp = selectRandomIp(host);
            if (StringUtils.hasText(selectedIp)) {
                URI newUri = buildRewrittenUri(originalUri, selectedIp);
                HttpRequest rewrittenRequest = new RewrittenRequest(request, newUri);
                rewrittenRequest.getHeaders().put("Host", List.of(host));
                log.debug("Rewriting URL from {} to {}", originalUri, newUri);
                return execution.execute(rewrittenRequest, body);
            }
            return execution.execute(request, body);
        }
        return execution.execute(request, body);
    }

    private URI buildRewrittenUri(URI originalUri, String ip) {
        String scheme = originalUri.getScheme();
        int port = originalUri.getPort();
        String path = originalUri.getPath();
        String query = originalUri.getQuery();
        String fragment = originalUri.getFragment();

        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(ip);
        if (port > 0) {
            sb.append(":").append(port);
        }
        if (path != null && !path.isEmpty()) {
            sb.append(path);
        }
        if (query != null && !query.isEmpty()) {
            sb.append("?").append(query);
        }
        if (fragment != null && !fragment.isEmpty()) {
            sb.append("#").append(fragment);
        }
        return URI.create(sb.toString());
    }

    public static class RewrittenRequest implements HttpRequest {

        private final HttpRequest originalRequest;
        private final URI newUri;

        public RewrittenRequest(HttpRequest originalRequest, URI newUri) {
            this.originalRequest = originalRequest;
            this.newUri = newUri;
        }

        @Override
        public HttpMethod getMethod() {
            return this.originalRequest.getMethod();
        }

        @Override
        public URI getURI() {
            return this.newUri;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return this.originalRequest.getAttributes();
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.originalRequest.getHeaders();
        }
    }

}
