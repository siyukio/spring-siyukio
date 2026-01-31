package io.github.siyukio.tools.util;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * HttpClient utility class for managing singleton instances based on version.
 *
 * @author Bugee
 */
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
                        .connectTimeout(Duration.ofSeconds(6))
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
}
