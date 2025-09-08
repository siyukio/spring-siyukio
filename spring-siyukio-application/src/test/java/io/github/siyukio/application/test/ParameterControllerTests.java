package io.github.siyukio.application.test;

import io.github.siyukio.application.model.parameter.StringRequest;
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
    void testStringRequired() {
        StringRequest stringRequest = StringRequest.builder()
                .required("")
                .build();
        JSONObject resultJson = this.apiMock.perform("/testString", stringRequest);
        log.info("{}", stringRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testStringLength() {
        StringRequest stringRequest = StringRequest.builder()
                .required("123456")
                .length("4444")
                .build();
        JSONObject resultJson = this.apiMock.perform("/testString", stringRequest);
        log.info("{}", stringRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testIntNumMin() {
        JSONObject numRequest = new JSONObject();
        numRequest.put("intNum", 1);
        JSONObject resultJson = this.apiMock.perform("/testNum", numRequest);
        log.info("{}", numRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testIntNumMax() {
        JSONObject numRequest = new JSONObject();
        numRequest.put("intNum", 100);
        JSONObject resultJson = this.apiMock.perform("/testNum", numRequest);
        log.info("{}", numRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testDoubleNumMin() {
        JSONObject numRequest = new JSONObject();
        numRequest.put("doubleNum", 1);
        JSONObject resultJson = this.apiMock.perform("/testNum", numRequest);
        log.info("{}", numRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testDoubleNumMax() {
        JSONObject numRequest = new JSONObject();
        numRequest.put("doubleNum", 100);
        JSONObject resultJson = this.apiMock.perform("/testNum", numRequest);
        log.info("{}", numRequest);
        log.info("{}", resultJson);
    }

    @Test
    void testBigNumMin() {
        JSONObject numDto = new JSONObject();
        numDto.put("bigNum", "100");
        JSONObject resultJson = this.apiMock.perform("/testNum", numDto);
        log.info("{}", numDto);
        log.info("{}", resultJson);
    }

    @Test
    void testBigNumMax() {
        JSONObject numDto = new JSONObject();
        numDto.put("bigNum", "999999");
        JSONObject resultJson = this.apiMock.perform("/testNum", numDto);
        log.info("{}", numDto);
        log.info("{}", resultJson);
    }

    @Test
    void testBool() {
        JSONObject boolDto = new JSONObject();
        boolDto.put("bool", "222");
        JSONObject resultJson = this.apiMock.perform("/testBool", boolDto);
        log.info("{}", boolDto);
        log.info("{}", resultJson);
    }

    @Test
    void testDate() {
        JSONObject dateDto = new JSONObject();
        dateDto.put("date", "ss222-455");
        JSONObject resultJson = this.apiMock.perform("/testDate", dateDto);
        log.info("{}", dateDto);
        log.info("{}", resultJson);
    }

    @Test
    void testStringListMin() {
        List<String> list = List.of("1", "2");
        JSONObject listDto = new JSONObject();
        listDto.put("stringList", list);
        JSONObject resultJson = this.apiMock.perform("/testList", listDto);
        log.info("{}", listDto);
        log.info("{}", resultJson);
    }

    @Test
    void testStringListMax() {
        List<String> list = List.of("1", "2", "3", "4", "5", "6", "7");
        JSONObject listDto = new JSONObject();
        listDto.put("stringList", list);
        JSONObject resultJson = this.apiMock.perform("/testList", listDto);
        log.info("{}", listDto);
        log.info("{}", resultJson);
    }

    @Test
    void testStringListItemMax() {
        List<String> list = List.of("1", "2", "3333");
        JSONObject listDto = new JSONObject();
        listDto.put("stringList", list);
        JSONObject resultJson = this.apiMock.perform("/testList", listDto);
        log.info("{}", listDto);
        log.info("{}", resultJson);
    }

    @Test
    void testStringListByNum() {
        List<Integer> list = List.of(1, 2, 3, 4, 5, 6);
        JSONObject listDto = new JSONObject();
        listDto.put("stringList", list);
        JSONObject resultJson = this.apiMock.perform("/testList", listDto);
        log.info("{}", listDto);
        log.info("{}", resultJson);
    }


    @Test
    void testStringListByJson() {
        JSONObject listDto = new JSONObject();
        listDto.put("stringList", "[\"1\", \"2\", \"3\", \"4\", \"5\", \"6\"]");
        JSONObject requestJson = this.apiMock.perform("/testList", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testStringListByJsonNum() {
        JSONObject listDto = new JSONObject();
        listDto.put("stringList", "[1, 2, 3, 4, 5, 6]");
        JSONObject requestJson = this.apiMock.perform("/testList", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testNumListByString() {
        List<String> list = List.of("1", "2", "3", "4");
        JSONObject listDto = new JSONObject();
        listDto.put("numList", list);
        JSONObject requestJson = this.apiMock.perform("/testList", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testNumListByErrorString() {
        List<String> list = List.of("1", "2w", "3", "4");
        JSONObject listDto = new JSONObject();
        listDto.put("numList", list);
        JSONObject requestJson = this.apiMock.perform("/testList", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testNumListByJsonString() {
        JSONObject listDto = new JSONObject();
        listDto.put("numList", "[\"1\", \"2\", \"3\", \"4\", \"5\", \"6\"]");
        JSONObject requestJson = this.apiMock.perform("/testList", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testObjectArray() {
        JSONObject listDto = new JSONObject();
        listDto.put("objectArray", """
                [
                    {\"name\":\"test\"},
                    {\"index\":1},
                    {\"show\":true}
                ]
                """);
        JSONObject requestJson = this.apiMock.perform("/testList", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testObjectList() {
        JSONObject listDto = new JSONObject();
        listDto.put("objectList", """
                [
                    {\"name\":\"test\"},
                    {\"index\":1},
                    {\"show\":true}
                ]
                """);
        JSONObject requestJson = this.apiMock.perform("/testList", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

    @Test
    void testPage() {
        JSONObject listDto = new JSONObject();
        listDto.put("uid", "123");
        JSONObject requestJson = this.apiMock.perform("/testPage", listDto);
        log.info("{}", listDto);
        log.info("{}", requestJson);
    }

}
