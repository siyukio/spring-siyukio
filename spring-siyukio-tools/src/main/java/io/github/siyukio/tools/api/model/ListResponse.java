package io.github.siyukio.tools.api.model;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;

import java.util.List;

/**
 * @author Buddy
 */
@Builder
@With
public record ListResponse<T>(
        
        @ApiParameter(description = "data items")
        List<T> items
) {
}
