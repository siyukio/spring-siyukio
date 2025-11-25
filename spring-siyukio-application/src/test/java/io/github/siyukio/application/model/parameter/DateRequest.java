package io.github.siyukio.application.model.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;

import java.util.Date;

/**
 * @author Buddy
 */
@Builder
@With
public record DateRequest(

        @ApiParameter(required = false)
        Date date
) {
}
