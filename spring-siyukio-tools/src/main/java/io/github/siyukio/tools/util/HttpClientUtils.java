package io.github.siyukio.tools.util;

import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * HttpClient utility class for managing singleton instances based on version.
 *
 * @author Bugee
 */
@Slf4j
public abstract class HttpClientUtils {

    private static final ConcurrentMap<HttpClient.Version, HttpClient> HTTP_CLIENT_CACHE = new ConcurrentHashMap<>();

    /**
     * Get or create a singleton HttpClient instance for the specified version.
     *
     * @param version the HTTP version
     * @return a singleton HttpClient instance
     */
    public static HttpClient getHttpClient(HttpClient.Version version) {
        return HTTP_CLIENT_CACHE.computeIfAbsent(version, v ->
                HttpClient.newBuilder()
                        .version(v)
                        .connectTimeout(Duration.ofSeconds(12))
                        .executor(AsyncUtils.VIRTUAL_EXECUTOR_SERVICE)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build()
        );
    }

    /**
     * Get HttpClient with HTTP_1_1 version.
     *
     * @return a singleton HttpClient instance with HTTP_1_1 version
     */
    public static HttpClient getHttpClient() {
        return getHttpClient(HttpClient.Version.HTTP_1_1);
    }

    /**
     * Clear all cached HttpClient instances.
     */
    public static void clearCache() {
        HTTP_CLIENT_CACHE.clear();
    }

    /**
     * Resolve domain name to list of IP addresses.
     *
     * @param domain the domain name to resolve
     * @return an unmodifiable list of IP addresses, or empty list if resolution fails
     */
    public static List<String> resolveDomain(String domain) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(domain);
            return Arrays.stream(addresses)
                    .filter(addr -> addr instanceof Inet4Address)
                    .filter(addr -> !addr.isLoopbackAddress())
                    .map(InetAddress::getHostAddress).toList();
        } catch (Exception e) {
            log.error("Resolve domain {} error: {}", domain, e.getMessage());
            return Collections.emptyList();
        }
    }

}
