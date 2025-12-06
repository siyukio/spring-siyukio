package io.github.siyukio.application.dto.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import io.github.siyukio.tools.api.annotation.Example;
import lombok.Builder;
import lombok.With;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * @author Buddy
 */
@Builder
@With
public record ListRequest(

        @ApiParameter(required = false, minItems = 3, maxItems = 6, maxLength = 3,
                examples = {
                        @Example(value = """
                                ["a", "b", "c"]
                                """,
                                summary = "text array")
                })
        List<String> stringList,

        @ApiParameter(required = false, minItems = 3, maxItems = 6,
                examples = {
                        @Example(value = """
                                [1,2,3]
                                """,
                                summary = "num array")
                })
        List<Integer> numList,

        @ApiParameter(required = false, minItems = 3, maxItems = 6,
                examples = {
                        @Example(value = """
                                [{"id":""}, {"id":""}]
                                """,
                                summary = "json object array"),
                        @Example(value = """
                                [1,2,3]
                                """,
                                summary = "num array")
                })
        JSONArray objectArray,

        @ApiParameter(required = false,
                examples = {
                        @Example(value = """
                                [{"id":""}, {"id":""}]
                                """)
                })
        List<JSONObject> objectList
) {
}
