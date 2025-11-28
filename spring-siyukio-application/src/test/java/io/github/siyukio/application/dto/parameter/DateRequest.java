package io.github.siyukio.application.dto.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;

import java.time.LocalDateTime;

/**
 * @author Buddy
 */
@Builder
@With
public record DateRequest(

        @ApiParameter(required = false)
        LocalDateTime date
) {
}
