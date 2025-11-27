package io.github.siyukio.application.controller;

import io.github.siyukio.application.model.signature.SignatureRequest;
import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Buddy
 */
@Slf4j
@ApiController(tags = "signature")
public class SignatureController {

    @ApiMapping(path = "/testSignature", signature = true, authorization = false)
    public void testSignature(SignatureRequest signatureRequest) {
        log.info("{}", XDataUtils.toPrettyJSONString(signatureRequest));
    }

}
