package com.siyukio.tools.api.annotation.client;

import java.lang.annotation.*;

/**
 * Api client.
 *
 * @author Buddy
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiClient {

    /**
     * Base URLs should always end in /
     * Base URL: http://example.com/api/ Endpoint: foo/bar/ Target: http://example.com/api/foo/bar/
     * Base URL: http://example.com/api/ Endpoint: /foo/bar/ Target: http://example.com/foo/bar/
     */
    String baseUrl() default "";

    ApiClientHeader[] headers() default {};

    /**
     * HTTP connection timeout, default is 10 seconds.
     */
    int connectTimeout() default 6;

    /**
     * HTTP read timeout, default is 10 seconds.
     */
    int readTimeout() default 10;

    /**
     * HTTP write timeout, default is 10 seconds.
     */
    int writeTimeout() default 10;

    /**
     * Maximum number of idle connections in the HTTP connection pool, default is 60.
     */
    int maxIdleConnections() default 60;

    /**
     * Idle connection eviction time in the HTTP connection pool, default is 15 minutes.
     */
    int keepAliveDuration() default 15;

    /**
     * Maximum number of concurrent requests, default is 60.
     */
    int maxRequests() default 60;

    /**
     * Maximum number of concurrent requests per host, default is 45.
     */
    int maxRequestsPerHost() default 45;

    /**
     * Whether to use the h2c protocol (HTTP/2 cleartext), default is false.
     */
    boolean h2c() default false;

    /**
     * Whether to force the use of HTTP/1, default is false.
     */
    boolean http1Only() default false;

    /**
     * Whether to automatically generate an app role token during the request.
     */
    boolean autoAppToken() default false;
}
