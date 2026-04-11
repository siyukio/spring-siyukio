package io.github.siyukio.tools.api.annotation.client;

import java.lang.annotation.*;

/**
 * DNS resolver configuration for API clients.
 *
 * @author Buddy
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DnsResolver {

    /**
     * DNS server address for custom DNS resolution.
     * <p>
     * Example: "8.8.8.8" for Google DNS, "1.1.1.1" for Cloudflare DNS.
     */
    String dns() default "";

    /**
     * Custom port number for the API connection.
     * <p>
     * If not specified (0), the default port will be used based on the protocol.
     */
    int port() default 53;

    /**
     * Whether to use TCP protocol, default is true.
     * <p>
     * When false, UDP will be attempted (if supported by the underlying transport).
     * Note: HTTP/2 requires TCP, so this only affects HTTP/1.1 connections.
     */
    boolean useTcp() default true;
}
