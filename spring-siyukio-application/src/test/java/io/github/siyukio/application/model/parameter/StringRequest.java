package io.github.siyukio.application.model.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;

/**
 * @author Buddy
 */
@Builder
@With
public record StringRequest(

        @ApiParameter
        String required,

        @ApiParameter(minLength = 1, maxLength = 3, required = false)
        String length,

        @ApiParameter(pattern = "123|456", required = false)
        String pattern
) {

}
