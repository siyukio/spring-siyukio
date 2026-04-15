package io.github.siyukio.client;

import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test class for KeepaClient.
 *
 * @author Bugee
 */
@SpringBootTest
@Slf4j
class CommonApiClientTest {

    @Autowired
    private CommonApiClient commonApiClient;

    @Test
    void testApiDocs() {

        JSONObject result = commonApiClient.apiDocs();

        log.info("api docs: {}", XDataUtils.toPrettyJSONString(result));
    }

    @Test
    void testHealth() {

        CommonApiClient.Health result = commonApiClient.health();

        log.info("api health: {}", XDataUtils.toPrettyJSONString(result));
    }

    @Test
    void testCreateAuthorization() {
        JSONObject request = new JSONObject();
        request.put("uid", "1234567890");
        request.put("name", "test");
//        request.put("roles", List.of("admin"));
        JSONObject result = commonApiClient.createAuthorization(request);

        log.info("api createAuthorization: {}", XDataUtils.toPrettyJSONString(result));
    }
}
