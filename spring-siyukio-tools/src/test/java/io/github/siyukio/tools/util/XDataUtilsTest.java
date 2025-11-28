package io.github.siyukio.tools.util;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

/**
 * @author Bugee
 */
@Slf4j
public class XDataUtilsTest {

    @Test
    void testParse() {
        String text = """
                {
                  "string": "Hello, world!",
                  "integer": 42,
                  "float": 3.14159,
                  "booleanTrue": true,
                  "booleanFalse": false,
                  "nullValue": null,
                  "arrayOfNumbers": [1, 2, 3, 4, 5],
                  "arrayOfStrings": ["apple", "banana", "cherry"],
                  "arrayMixed": [1, "two", true, null, {"key": "value"}],
                  "object": {
                    "nestedString": "Nested",
                    "nestedNumber": 99,
                    "nestedArray": [10, 20, 30]
                  },
                  "nestedObject": {
                    "level1": {
                      "level2": {
                        "level3": "deep value"
                      }
                    }
                  },
                  "dateString": "2025-11-27 15:00:00",
                  "emptyObject": {},
                  "emptyArray": []
                }
                """;

        JSONObject jsonObject = XDataUtils.parseObject(text);
        log.info("{}", XDataUtils.toPrettyJSONString(jsonObject));
    }

    @Test
    void testConvertToLocalDateTime() {
        String text = """
                {
                  "date1": "2025-11-27 15:15:15",
                  "date2": "2025-11-27 15:15",
                  "date3": "2025-11-27",
                  "date4": "2025-11-27T15:15:15",
                  "date5": "2025/11/27 15:15:15",
                  "date6": "2025/11/27",
                  "date7": 1764231837038,
                  "date8": "1764231837038",
                  "date9": 1764231837,
                  "date10": "1764231837"
                }
                """;

        DateRequest dateRequest = XDataUtils.parse(text, DateRequest.class);
        log.info("{}", XDataUtils.toPrettyJSONString(dateRequest));
    }

    @Test
    void testEnum() {
        String text = """
                {
                  "loginType": "USERNAME"
                }
                """;
        EnumRequest enumRequest = XDataUtils.parse(text, EnumRequest.class);
        log.info("{}", XDataUtils.toPrettyJSONString(enumRequest));
    }

    public enum LoginType {
        USERNAME,
        PHONE,
        EMAIL,
        GOOGLE,
        APPLE
    }

    public record EnumRequest(
            LoginType loginType
    ) {
    }

    public record DateRequest(

            LocalDateTime date1,

            LocalDateTime date2,

            LocalDateTime date3,

            LocalDateTime date4,

            LocalDateTime date5,

            LocalDateTime date6,

            LocalDateTime date7,

            LocalDateTime date8,

            LocalDateTime date9,

            LocalDateTime date10
    ) {
    }
}
