package io.github.siyukio.client;

import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
class LocalApiClientTest {

    @Autowired
    private LocalApiClient localApiClient;

    @Test
    void testGetToken() {
        JSONObject result = localApiClient.getToken();

        log.info("Local api getToken: {}", XDataUtils.toPrettyJSONString(result));
    }
}
