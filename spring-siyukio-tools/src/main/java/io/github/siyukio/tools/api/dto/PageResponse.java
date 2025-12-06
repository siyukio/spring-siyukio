package io.github.siyukio.tools.api.dto;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;

import java.util.List;

/**
 * @author Buddy
 */
@Builder
@With
public record PageResponse<T>(

        @ApiParameter(description = "total number of records")
        int total,

        @ApiParameter(description = "data items")
        List<T> items
) {
}
