package io.github.siyukio.application.test;

import io.github.siyukio.application.model.signature.SignatureRequest;
import io.github.siyukio.tools.api.ApiMock;
import io.github.siyukio.tools.api.signature.SignatureProvider;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@Slf4j
@SpringBootTest
class SignatureControllerTests {

    @Autowired
    private ApiMock apiMock;

    @Autowired
    private SignatureProvider signatureProvider;

    @Test
    void testSignature() {
        SignatureRequest signatureRequest = SignatureRequest.builder()
                .bool(true)
                .num(6)
                .text("hello")
                .build();
        JSONObject requestJson = XDataUtils.copy(signatureRequest, JSONObject.class);
        long timestamp = System.currentTimeMillis();
        requestJson.put("timestamp", timestamp);
        String nonce = UUID.randomUUID().toString();
        requestJson.put("nonce", nonce);
        String signature = this.signatureProvider.createSignature(timestamp, nonce);
        requestJson.put("signature", signature);
        JSONObject resultJson = this.apiMock.perform("/testSignature", requestJson);
        log.info("{}", requestJson);
        log.info("{}", resultJson);
    }

}
