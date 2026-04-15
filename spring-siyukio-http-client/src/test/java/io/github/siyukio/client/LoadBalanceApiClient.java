package io.github.siyukio.client;

import io.github.siyukio.tools.api.annotation.client.ApiClient;
import org.json.JSONObject;
import org.springframework.web.service.annotation.GetExchange;

/**
 *
 * @author Bugee
 */
@ApiClient(url = "${spring.siyukio.api-docs.url}", headers = {
        "Authorization=${spring.siyukio.api-docs.authorization}"
}, loadBalance = true)
public interface LoadBalanceApiClient {

    /**
     * Get product information.
     *
     * @param key  the API key
     * @param asin the product ASIN
     * @return product data as JSONObject
     */
    @GetExchange("/api-docs")
    JSONObject apiDocs();
}
