package io.github.siyukio.application.model.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import io.github.siyukio.tools.api.annotation.Example;
import lombok.Builder;
import lombok.With;
import org.json.JSONObject;

/**
 * @author Buddy
 */
@ApiParameter(description = "item")
@Builder
@With
public record FilterParam(

        @ApiParameter(description = "user id")
        String uid,

        @ApiParameter(required = false, description = "json",
                examples = {
                        @Example(value = """
                                {"name":"", "value":""}
                                """,
                                summary = "json object")
                })
        JSONObject json
) {
}
