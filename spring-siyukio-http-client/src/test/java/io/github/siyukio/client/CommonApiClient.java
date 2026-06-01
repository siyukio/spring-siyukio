package io.github.siyukio.client;

import io.github.siyukio.tools.api.annotation.client.ApiClient;
import io.github.siyukio.tools.cache.annotation.CacheConfig;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Bugee
 */
@ApiClient(url = "${spring.siyukio.api-docs.url}", headers = {
        "Authorization=${spring.siyukio.api-docs.authorization}"
}, cacheConfig = @CacheConfig(maximumSize = 100, expireUnit = TimeUnit.SECONDS, expireAfterWrite = 30, expireAfterAccess = 0))
public interface CommonApiClient {

    /**
     * Get product information.
     *
     * @param key  the API key
     * @param asin the product ASIN
     * @return product data as JSONObject
     */
    @GetExchange("/api-docs")
    JSONObject apiDocs();

    @GetExchange("/actuator/health")
    Health health();

    @PostExchange("/authorization/create")
    JSONObject createAuthorization(@RequestBody JSONObject request);

    @PostExchange("/token/get")
    JSONObject getToken();

    record Health(String status) {
    }
}
