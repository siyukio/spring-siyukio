package io.github.siyukio.application.model.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import io.github.siyukio.tools.api.annotation.Example;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * @author Buddy
 */
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListRequest {

    @ApiParameter(required = false, minItems = 3, maxItems = 6, maxLength = 3,
            examples = {
                    @Example(value = """
                            ["a", "b", "c"]
                            """,
                            summary = "text array")
            })
    public List<String> stringList;

    @ApiParameter(required = false, minItems = 3, maxItems = 6,
            examples = {
                    @Example(value = """
                            [1,2,3]
                            """,
                            summary = "num array")
            })
    public List<Integer> numList;

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
    public JSONArray objectArray;

    @ApiParameter(required = false, minItems = 3, maxItems = 6,
            examples = {
                    @Example(value = """
                            [{"id":""}, {"id":""}]
                            """,
                            summary = "json object list")
            })
    public List<JSONObject> objectList;

}
