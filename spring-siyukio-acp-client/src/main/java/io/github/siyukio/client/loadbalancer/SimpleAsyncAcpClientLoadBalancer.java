package io.github.siyukio.client.loadbalancer;

import io.github.siyukio.client.SimpleAsyncAcpClient;

/**
 * Load balancer for SimpleAsyncAcpClient.
 *
 * @author Bugee
 */
public interface SimpleAsyncAcpClientLoadBalancer {

    /**
     * Get an available SimpleAsyncAcpClient instance.
     *
     * @return an available SimpleAsyncAcpClient
     */
    SimpleAsyncAcpClient getClient();

    void close();

}
