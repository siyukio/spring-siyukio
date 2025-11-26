package io.github.siyukio.application.model.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;


@ApiParameter(description = "items")
@Builder
@With
public record PageItem(

        @ApiParameter
        String id,

        @ApiParameter
        String title,

        @ApiParameter
        Date lastUpdateDate,

        @ApiParameter
        JSONObject metadata,

        @ApiParameter
        List<JSONObject> children
) {

}
