package io.github.siyukio.tools.api.model;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;
import reactor.util.annotation.Nullable;

@Builder
@With
public record PageRequest<T>(

        @ApiParameter(description = "page number", minimum = 1, maximum = 1000, required = false, defaultValue = "1")
        Integer page,

        @ApiParameter(description = "number of records per page.", minimum = 1, maximum = 1000, required = false, defaultValue = "20")
        Integer pageSize,

        @ApiParameter(description = "data filter", required = false)
        @Nullable
        T filter
) {
}
