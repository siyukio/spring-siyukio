package io.github.siyukio.client.loadbalancer;

import io.github.siyukio.client.SimpleAsyncAcpClient;
import io.github.siyukio.tools.api.ApiException;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A simple load balancer that returns a single ACP client instance.
 *
 * @author Bugee
 */
@Slf4j
public class DirectAcpClientLoadBalancer implements SimpleAsyncAcpClientLoadBalancer {

    private final URI uri;

    private final SimpleAsyncAcpClient.Builder simpleAsyncAcpClientBuilder;
    private final Lock lock = new ReentrantLock();
    private volatile SimpleAsyncAcpClient client = null;
    private volatile long lastResolvedTime = 0;

    public DirectAcpClientLoadBalancer(URI uri,
                                       SimpleAsyncAcpClient.Builder simpleAsyncAcpClientBuilder) {
        this.uri = uri;
        this.simpleAsyncAcpClientBuilder = simpleAsyncAcpClientBuilder;
    }

    @Override
    public SimpleAsyncAcpClient getClient() {
        this.lock.lock();
        try {
            if (System.currentTimeMillis() - this.lastResolvedTime >= 15000) {
                if (this.client == null || this.client.isClosed()) {
                    this.client = this.simpleAsyncAcpClientBuilder.build(this.uri);
                    this.lastResolvedTime = System.currentTimeMillis();
                    log.info("Resolved ACP client: {}", this.uri);
                }
            }
        } finally {
            this.lock.lock();
        }
        if (this.client == null || this.client.isClosed()) {
            throw new ApiException("No ACP clients available:" + this.uri.toString());
        }
        return this.client;
    }

    @Override
    public void close() {
        if (this.client != null) {
            try {
                this.client.close();
            } catch (Exception ignored) {
            }
        }
    }

}
