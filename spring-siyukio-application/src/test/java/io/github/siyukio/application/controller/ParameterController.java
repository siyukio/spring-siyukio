package io.github.siyukio.application.controller;

import io.github.siyukio.application.dto.parameter.*;
import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.model.PageRequest;
import io.github.siyukio.tools.api.model.PageResponse;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Buddy
 */
@Slf4j
@ApiController(tags = "parameter")
public class ParameterController {

    @ApiMapping(path = "/string/test", authorization = false)
    public void testString(StringRequest stringRequest) {
        log.info("{}", XDataUtils.toPrettyJSONString(stringRequest));
    }

    @ApiMapping(path = "/enum/test", authorization = false)
    public void testEnum(EnumRequest enumRequest) {
        log.info("{}", XDataUtils.toPrettyJSONString(enumRequest));
    }

    @ApiMapping(path = "/num/test", authorization = false)
    public void testNum(NumRequest numRequest) {
        log.info("{}", XDataUtils.toPrettyJSONString(numRequest));
    }

    @ApiMapping(path = "/bool/test", authorization = false)
    public void testBool(BoolRequest boolRequest) {
        log.info("{}", XDataUtils.toPrettyJSONString(boolRequest));
    }

    @ApiMapping(path = "/date/test", authorization = false)
    public void testDate(DateRequest dateRequest) {
        log.info("{}", XDataUtils.toPrettyJSONString(dateRequest));
    }

    @ApiMapping(path = "/list/test", authorization = false)
    public void testList(ListRequest listRequest) {
        log.info("{}", XDataUtils.toPrettyJSONString(listRequest));
    }

    @ApiMapping(path = "/page/test", authorization = false)
    public PageResponse<PageItem> testPage(PageRequest<FilterParam> pageRequest) {
        log.info("{}", XDataUtils.toPrettyJSONString(pageRequest));

        return PageResponse.<PageItem>builder()
                .items(List.of(PageItem.builder()
                        .loginType(LoginType.APPLE)
                        .updatedAt(LocalDateTime.now()).build()))
                .total(1).build();
    }


}
