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
class LoadBalanceApiClientTest {

    @Autowired
    private LoadBalanceApiClient loadBalanceApiClient;

    @Test
    void testApiDocs() {

        JSONObject result = loadBalanceApiClient.apiDocs();

        log.info("api docs: {}", XDataUtils.toPrettyJSONString(result));
    }
}
