package com.siyukio.application.controller;

import com.siyukio.application.model.signature.SignatureRequest;
import com.siyukio.tools.api.annotation.ApiController;
import com.siyukio.tools.api.annotation.ApiMapping;
import com.siyukio.tools.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Buddy
 */
@Slf4j
@ApiController(tags = "signature")
public class SignatureController {

    @ApiMapping(path = "/testSignature", signature = true, authorization = false)
    public void testSignature(SignatureRequest signatureRequest) {
        log.info("{}", JsonUtils.toPrettyJSONString(signatureRequest));
    }

}
