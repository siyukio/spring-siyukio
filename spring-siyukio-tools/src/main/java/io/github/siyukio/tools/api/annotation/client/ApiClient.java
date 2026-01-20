package io.github.siyukio.tools.api.annotation.client;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.service.annotation.HttpExchange;

import java.lang.annotation.*;
import java.net.http.HttpClient;

/**
 * Api client.
 *
 * @author Buddy
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@HttpExchange
public @interface ApiClient {

    /**
     * Base URL for the API.
     */
    @AliasFor(annotation = HttpExchange.class)
    String url();

    @AliasFor(annotation = HttpExchange.class)
    String method() default "";

    @AliasFor(annotation = HttpExchange.class)
    String contentType() default "";

    @AliasFor(annotation = HttpExchange.class)
    String[] accept() default {};

    @AliasFor(annotation = HttpExchange.class)
    String[] headers() default {};

    /**
     * HTTP connection timeout, default is 6 seconds.
     */
    int connectTimeout() default 6;

    /**
     * HTTP read timeout, default is 60 seconds.
     */
    int readTimeout() default 60;

    /**
     * HTTP version, default is HTTP/2.
     */
    HttpClient.Version version() default HttpClient.Version.HTTP_2;
}
