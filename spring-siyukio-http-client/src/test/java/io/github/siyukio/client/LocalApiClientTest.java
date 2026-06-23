package io.github.siyukio.client;

import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class LocalApiClientTest {

    @Value("${spring.siyukio.api-docs.url}")
    private String url;

    @Value("${spring.siyukio.api-docs.authorization}")
    private String authorization;

    @Autowired
    private HttpApiClient httpApiClient;

    @Test
    void testGetToken() {

        URI uri = URI.create(this.url + "/token/get");

        JSONObject result = this.httpApiClient.post(uri, Map.of("Authorization", this.authorization), new JSONObject());

        log.info("Local api getToken: {}", XDataUtils.toPrettyJSONString(result));
    }
}
