package io.github.siyukio.application.test;

import io.github.siyukio.application.dto.parameter.EnumRequest;
import io.github.siyukio.application.dto.parameter.LoginType;
import io.github.siyukio.application.dto.parameter.StringRequest;
import io.github.siyukio.tools.api.ApiMock;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
class ParameterControllerTests {

    @Autowired
    private ApiMock apiMock;

    @Test
    void testStringRequiredError() {
        StringRequest stringRequest = StringRequest.builder()
                .required("")
                .build();
        JSONObject resultJson = this.apiMock.perform("/string/test", stringRequest);
        log.info("{}", stringRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testStringLengthError() {
        StringRequest stringRequest = StringRequest.builder()
                .required("123456")
                .length("4444")
                .build();
        JSONObject resultJson = this.apiMock.perform("/string/test", stringRequest);
        log.info("{}", stringRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testStringPatternError() {
        StringRequest stringRequest = StringRequest.builder()
                .required("123456")
                .pattern("1234")
                .build();
        JSONObject resultJson = this.apiMock.perform("/string/test", stringRequest);
        log.info("{}", stringRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testEnumSuccess() {
        EnumRequest stringRequest = EnumRequest.builder()
                .loginType(LoginType.USERNAME)
                .build();
        JSONObject resultJson = this.apiMock.perform("/enum/test", stringRequest);
        log.info("{}", stringRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testEnumError() {
        JSONObject requestJson = new JSONObject();
        requestJson.put("loginType", "USERNAME");
        JSONObject resultJson = this.apiMock.perform("/enum/test", requestJson);
        log.info("{}", requestJson);
        log.info("{}", resultJson);
    }

    @Test
    void testIntNumMinError() {
        JSONObject numRequest = new JSONObject();
        numRequest.put("intNum", 1);
        JSONObject resultJson = this.apiMock.perform("/num/test", numRequest);
        log.info("{}", numRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testIntNumMaxError() {
        JSONObject numRequest = new JSONObject();
        numRequest.put("intNum", 100);
        JSONObject resultJson = this.apiMock.perform("/num/test", numRequest);
        log.info("{}", numRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testDoubleNumMinError() {
        JSONObject numRequest = new JSONObject();
        numRequest.put("doubleNum", 1);
        JSONObject resultJson = this.apiMock.perform("/num/test", numRequest);
        log.info("{}", numRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testDoubleNumMaxError() {
        JSONObject numRequest = new JSONObject();
        numRequest.put("doubleNum", 100);
        JSONObject resultJson = this.apiMock.perform("/num/test", numRequest);
        log.info("{}", numRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testBigNumMinError() {
        JSONObject numDto = new JSONObject();
        numDto.put("bigNum", "100");
        JSONObject resultJson = this.apiMock.perform("/num/test", numDto);
        log.info("{}", numDto);
        log.info("{}", resultJson);
    }

    @Test
    void testBigNumMaxError() {
        JSONObject numDto = new JSONObject();
        numDto.put("bigNum", "999999");
        JSONObject resultJson = this.apiMock.perform("/num/test", numDto);
        log.info("{}", numDto);
        log.info("{}", resultJson);
    }

    @Test
    void testBoolError() {
        JSONObject boolDto = new JSONObject();
        boolDto.put("bool", "222");
        JSONObject resultJson = this.apiMock.perform("/bool/test", boolDto);
        log.info("{}", boolDto);
        log.info("{}", resultJson);
    }

    @Test
    void testDateError() {
        JSONObject dateDto = new JSONObject();
        dateDto.put("date", "ss222-455");
        JSONObject resultJson = this.apiMock.perform("/date/test", dateDto);
        log.info("{}", dateDto);
        log.info("{}", resultJson);
    }

    @Test
    void testStringListMinError() {
        List<String> list = List.of("1", "2");
        JSONObject listDto = new JSONObject();
        listDto.put("stringList", list);
        JSONObject resultJson = this.apiMock.perform("/list/test", listDto);
        log.info("{}", listDto);
        log.info("{}", resultJson);
    }

    @Test
    void testStringListMaxError() {
        List<String> list = List.of("1", "2", "3", "4", "5", "6", "7");
        JSONObject listDto = new JSONObject();
        listDto.put("stringList", list);
        JSONObject resultJson = this.apiMock.perform("/list/test", listDto);
        log.info("{}", listDto);
        log.info("{}", resultJson);
    }

    @Test
    void testStringListItemMaxError() {
        List<String> list = List.of("1", "2", "3333");
        JSONObject listDto = new JSONObject();
        listDto.put("stringList", list);
        JSONObject resultJson = this.apiMock.perform("/list/test", listDto);
        log.info("{}", listDto);
        log.info("{}", resultJson);
    }

    @Test
    void testStringListByNumSuccess() {
        List<Integer> list = List.of(1, 2, 3, 4, 5, 6);
        JSONObject listDto = new JSONObject();
        listDto.put("stringList", list);
        JSONObject resultJson = this.apiMock.perform("/list/test", listDto);
        log.info("{}", listDto);
        log.info("{}", resultJson);
    }


    @Test
    void testStringListByJsonSuccess() {
        JSONObject listDto = new JSONObject();
        listDto.put("stringList", "[\"1\", \"2\", \"3\", \"4\", \"5\", \"6\"]");
        JSONObject requestJson = this.apiMock.perform("/list/test", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testStringListByJsonNumSuccess() {
        JSONObject listDto = new JSONObject();
        listDto.put("stringList", "[1, 2, 3, 4, 5, 6]");
        JSONObject requestJson = this.apiMock.perform("/list/test", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testNumListByStringSuccess() {
        List<String> list = List.of("1", "2", "3", "4");
        JSONObject listDto = new JSONObject();
        listDto.put("numList", list);
        JSONObject requestJson = this.apiMock.perform("/list/test", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testNumListByStringError() {
        List<String> list = List.of("1", "2w", "3", "4");
        JSONObject listDto = new JSONObject();
        listDto.put("numList", list);
        JSONObject requestJson = this.apiMock.perform("/list/test", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testNumListByJsonStringSuccess() {
        JSONObject listDto = new JSONObject();
        listDto.put("numList", "[\"1\", \"2\", \"3\", \"4\", \"5\", \"6\"]");
        JSONObject requestJson = this.apiMock.perform("/list/test", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testObjectArraySuccess() {
        JSONObject listDto = new JSONObject();
        listDto.put("objectArray", """
                [
                    {"name":"test"},
                    {"index":1},
                    {"show":true}
                ]
                """);
        JSONObject requestJson = this.apiMock.perform("/list/test", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testObjectListSuccess() {
        JSONObject listDto = new JSONObject();
        listDto.put("objectList", """
                [
                    {"name":"test"},
                    {"index":1},
                    {"show":true}
                ]
                """);
        JSONObject requestJson = this.apiMock.perform("/list/test", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testPageSuccess() {
        JSONObject myPageRequest = new JSONObject();

        JSONObject filterJson = new JSONObject();
        filterJson.put("uid", "admin");
        myPageRequest.put("filter", filterJson);

        JSONObject requestJson = this.apiMock.perform("/page/test", myPageRequest);
        log.info("{}", myPageRequest);
        log.info("{}", requestJson);
    }

}
