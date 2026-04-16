package io.github.siyukio.client.loadbalancer;

import io.github.siyukio.client.SimpleAsyncAcpClient;
import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.util.HttpClientUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A load balancer that resolves the host DNS and randomly selects an available client.
 *
 * @author Bugee
 */
@Slf4j
public class DnsRandomAcpClientLoadBalancer implements SimpleAsyncAcpClientLoadBalancer {

    private final URI uri;
    private final SimpleAsyncAcpClient.Builder builder;
    private final Lock lock = new ReentrantLock();
    private volatile List<SimpleAsyncAcpClient> clients = List.of();
    private volatile long lastResolvedTime = 0;

    public DnsRandomAcpClientLoadBalancer(URI uri, SimpleAsyncAcpClient.Builder builder) {
        this.uri = uri;
        this.builder = builder;
    }

    private URI buildUri(URI originalUri, String ip) {
        String scheme = originalUri.getScheme();
        int port = originalUri.getPort();
        String path = originalUri.getPath();

        StringBuilder sb = new StringBuilder();
        sb.append(scheme).append("://").append(ip);
        if (port > 0) {
            sb.append(":").append(port);
        }
        if (path != null && !path.isEmpty()) {
            sb.append(path);
        }
        return URI.create(sb.toString());
    }

    private List<SimpleAsyncAcpClient> ensureResolved() {
        String host = this.uri.getHost();
        List<String> resolvedIps = HttpClientUtils.resolveDomain(host);
        log.debug("Resolved {} ips: {}", host, resolvedIps);
        if (CollectionUtils.isEmpty(resolvedIps)) {
            throw new ApiException("No resolved ips: " + host);
        }

        Set<String> newIps = new HashSet<>(resolvedIps);
        List<SimpleAsyncAcpClient> newClients = new ArrayList<>();
        this.clients.forEach(simpleAsyncAcpClient -> {
            String ip = simpleAsyncAcpClient.getUri().getHost();
            if (newIps.contains(ip)) {
                newClients.add(simpleAsyncAcpClient);
                newIps.remove(ip);
            }
        });
        if (!CollectionUtils.isEmpty(newIps)) {
            newIps.forEach(ip -> {
                URI newUri;
                if ("127.0.0.1".equals(ip)) {
                    newUri = this.uri;
                } else {
                    newUri = this.buildUri(this.uri, ip);
                }
                SimpleAsyncAcpClient simpleAsyncAcpClient = this.builder.build(newUri);
                newClients.add(simpleAsyncAcpClient);
            });
        }
        return newClients;
    }

    @Override
    public SimpleAsyncAcpClient getClient() {
        this.lock.lock();
        try {
            if (System.currentTimeMillis() - this.lastResolvedTime >= 12000) {
                List<SimpleAsyncAcpClient> newClients = this.ensureResolved();
                if (CollectionUtils.isEmpty(this.clients)) {
                    log.debug("Resolved ACP client: {}, {}", this.uri, newClients.size());
                }
                this.clients = newClients;
                this.lastResolvedTime = System.currentTimeMillis();
            }
        } finally {
            this.lock.lock();
        }
        if (CollectionUtils.isEmpty(this.clients)) {
            throw new ApiException("No ACP clients available:" + this.uri.toString());
        }

        int index = ThreadLocalRandom.current().nextInt(this.clients.size());
        SimpleAsyncAcpClient simpleAsyncAcpClient = this.clients.get(index);
        log.debug("Selected simpleAsyncAcpClient: {}, {}, {}", this.uri, simpleAsyncAcpClient.getUri(), index);
        return simpleAsyncAcpClient;
    }

    @Override
    public void close() {
        this.clients.forEach(simpleAsyncAcpClient -> {
            try {
                simpleAsyncAcpClient.close();
            } catch (Exception ignored) {
            }
        });
    }

}
