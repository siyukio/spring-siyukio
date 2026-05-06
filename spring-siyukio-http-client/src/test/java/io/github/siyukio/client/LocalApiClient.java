package io.github.siyukio.client;

import io.github.siyukio.tools.api.annotation.client.ApiClient;
import org.json.JSONObject;
import org.springframework.web.service.annotation.PostExchange;

/**
 *
 * @author Bugee
 */
@ApiClient(url = "${spring.siyukio.api-docs.url}", headers = {
        "Authorization=${spring.siyukio.api-docs.authorization}"
})
public interface LocalApiClient {

    @PostExchange("/token/get")
    JSONObject getToken();

}
