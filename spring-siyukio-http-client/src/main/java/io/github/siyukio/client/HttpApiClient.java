package io.github.siyukio.client;

import io.github.siyukio.tools.api.annotation.client.ApiClient;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.net.URI;
import java.util.Map;

/**
 * Common HTTP API client for generic GET/POST/PUT/DELETE requests with dynamic headers, query params and body.
 *
 * @author Bugee
 */
@ApiClient
public interface HttpApiClient {

    @GetExchange
    JSONObject get(URI uri, @RequestHeader Map<String, String> headerMap, @RequestParam Map<String, String> queryMap);

    @PostExchange
    JSONObject post(URI uri, @RequestHeader Map<String, String> headerMap, @RequestBody JSONObject requestBody);

    /**
     * Send a form-urlencoded POST request.
     *
     * @param uri       target URI
     * @param headerMap request headers
     * @param formMap   form data, e.g. {@code new LinkedMultiValueMap<>()}
     * @return response body as {@link JSONObject}
     */
    @PostExchange(contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    JSONObject post(URI uri, @RequestHeader Map<String, String> headerMap, @RequestBody MultiValueMap<String, String> formMap);

    @PutExchange
    JSONObject put(URI uri, @RequestHeader Map<String, String> headerMap, @RequestBody JSONObject requestBody);

    @DeleteExchange
    JSONObject delete(URI uri, @RequestHeader Map<String, String> headerMap);
}
