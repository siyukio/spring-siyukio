package io.github.siyukio.application.dto.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;
import org.json.JSONObject;

import java.time.LocalDateTime;
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
        LoginType loginType,

        @ApiParameter
        LocalDateTime updatedAt,

        @ApiParameter
        JSONObject metadata,

        @ApiParameter
        List<JSONObject> children
) {

}
