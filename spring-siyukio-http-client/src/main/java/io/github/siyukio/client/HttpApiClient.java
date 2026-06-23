package io.github.siyukio.client;

import io.github.siyukio.tools.api.annotation.client.ApiClient;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.net.URI;
import java.util.Map;

/**
 * Common HTTP API client for generic GET/POST requests with dynamic headers, query params and body.
 *
 * @author Bugee
 */
@ApiClient
public interface HttpApiClient {

    @GetExchange
    JSONObject get(URI uri, @RequestHeader Map<String, String> headerMap, @RequestParam Map<String, String> queryMap);

    @PostExchange
    JSONObject post(URI uri, @RequestHeader Map<String, String> headerMap, @RequestBody JSONObject requestBody);
}
