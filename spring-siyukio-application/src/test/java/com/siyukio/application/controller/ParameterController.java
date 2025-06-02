package com.siyukio.application.controller;

import com.siyukio.application.model.parameter.*;
import com.siyukio.tools.api.annotation.ApiController;
import com.siyukio.tools.api.annotation.ApiMapping;
import com.siyukio.tools.api.model.PageResponse;
import com.siyukio.tools.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Buddy
 */
@Slf4j
@ApiController(tags = "parameter")
public class ParameterController {

    @ApiMapping(path = "/testString", authorization = false)
    public void testString(StringRequest stringRequest) {
        log.info("{}", JsonUtils.toPrettyJSONString(stringRequest));
    }

    @ApiMapping(path = "/testNum", authorization = false)
    public void testNum(NumRequest numRequest) {
        log.info("{}", JsonUtils.toPrettyJSONString(numRequest));
    }

    @ApiMapping(path = "/testBool", authorization = false)
    public void testBool(BoolRequest boolRequest) {
        log.info("{}", JsonUtils.toPrettyJSONString(boolRequest));
    }

    @ApiMapping(path = "/testDate", authorization = false)
    public void testDate(DateRequest dateRequest) {
        log.info("{}", JsonUtils.toPrettyJSONString(dateRequest));
    }

    @ApiMapping(path = "/testList", authorization = false)
    public void testList(ListRequest listRequest) {
        log.info("{}", JsonUtils.toPrettyJSONString(listRequest));
    }

    @ApiMapping(path = "/testPage", authorization = false)
    public PageResponse<ItemVo> testPage(ListRequest listRequest) {
        log.info("{}", JsonUtils.toPrettyJSONString(listRequest));

        return PageResponse.<ItemVo>builder()
                .total(1).build();
    }


}
