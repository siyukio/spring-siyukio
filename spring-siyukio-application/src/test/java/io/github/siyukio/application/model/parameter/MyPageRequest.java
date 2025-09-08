package io.github.siyukio.application.model.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import io.github.siyukio.tools.api.annotation.Example;
import io.github.siyukio.tools.api.model.PageRequest;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.json.JSONObject;

/**
 * @author Buddy
 */
@ToString
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class MyPageRequest extends PageRequest {

    @ApiParameter(description = "user id")
    public String uid;

    @ApiParameter(required = false, description = "json",
            examples = {
                    @Example(value = """
                            {"name":"", "value":""}
                            """,
                            summary = "json object")
            })
    public JSONObject json;
}
