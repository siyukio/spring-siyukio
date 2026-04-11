package io.github.siyukio.client.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
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
public class DnsUrlRewriteInterceptor implements org.springframework.http.client.ClientHttpRequestInterceptor {

    private final String dnsServer;
    private final int dnsPort;
    private final boolean useTcp;
    private final String originalHost;

    private final List<String> resolvedIps = new ArrayList<>();
    private final Lock lock = new ReentrantLock();
    private volatile boolean initialized = false;

    public DnsUrlRewriteInterceptor(String dnsServer, int dnsPort, boolean useTcp, String originalHost) {
        this.dnsServer = dnsServer;
        this.dnsPort = dnsPort;
        this.useTcp = useTcp;
        this.originalHost = originalHost;
    }

    private void ensureResolved() {
        if (initialized) {
            return;
        }
        lock.lock();
        try {
            if (initialized) {
                return;
            }
            if (dnsServer == null || dnsServer.isBlank()) {
                log.debug("No DNS server configured, skipping resolution");
                initialized = true;
                return;
            }
            try {
                resolveUsingDnsJava();
                if (resolvedIps.isEmpty()) {
                    resolvedIps.add(originalHost);
                }
                log.debug("Resolved {} via DNS {} (TCP={}) to IPs: {}", originalHost, dnsServer, useTcp, resolvedIps);
            } catch (Exception e) {
                log.warn("Failed to resolve {} via DNS {}: {}", originalHost, dnsServer, e.getMessage());
                resolvedIps.add(originalHost);
            }
            initialized = true;
        } finally {
            lock.unlock();
        }
    }

    private void resolveUsingDnsJava() throws UnknownHostException, TextParseException {
        SimpleResolver resolver = new SimpleResolver(dnsServer);
        resolver.setPort(dnsPort);
        if (useTcp) {
            resolver.setTCP(true);
        }
        Lookup lookup = new Lookup(originalHost, Type.A);
        lookup.setResolver(resolver);
        Record[] records = lookup.run();
        if (records != null) {
            for (Record record : records) {
                String ip = record.rdataToString();
                if (ip != null && !ip.isBlank()) {
                    resolvedIps.add(ip);
                }
            }
        }
    }

    private String selectRandomIp() {
        ensureResolved();
        if (resolvedIps.isEmpty()) {
            return originalHost;
        }
        if (resolvedIps.size() == 1) {
            return resolvedIps.getFirst();
        }
        return resolvedIps.get(ThreadLocalRandom.current().nextInt(resolvedIps.size()));
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        URI originalUri = request.getURI();
        String host = originalUri.getHost();

        if (host != null && host.equals(originalHost)) {
            String selectedIp = selectRandomIp();
            URI newUri = buildRewrittenUri(originalUri, selectedIp);
            HttpRequest rewrittenRequest = new RewrittenRequest(request, newUri);
            log.debug("Rewriting URL from {} to {}", originalUri, newUri);
            return execution.execute(rewrittenRequest, body);
        }

        return execution.execute(request, body);
    }

    private URI buildRewrittenUri(URI originalUri, String ip) {
        String scheme = originalUri.getScheme();
        int port = dnsPort > 0 ? dnsPort : originalUri.getPort();
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
