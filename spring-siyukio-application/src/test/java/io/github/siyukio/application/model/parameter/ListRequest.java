package io.github.siyukio.application.model.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
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

    @ApiParameter(required = false, minItems = 3, maxItems = 6, maxLength = 3)
    public List<String> stringList;

    @ApiParameter(required = false, minItems = 3, maxItems = 6)
    public List<Integer> numList;

    @ApiParameter(required = false, minItems = 3, maxItems = 6)
    public JSONArray objectArray;

    @ApiParameter(required = false, minItems = 3, maxItems = 6)
    public List<JSONObject> objectList;

}
