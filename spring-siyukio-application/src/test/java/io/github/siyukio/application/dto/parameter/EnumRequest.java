package io.github.siyukio.application.dto.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;

/**
 * @author Buddy
 */
@Builder
@With
public record EnumRequest(

        @ApiParameter
        LoginType loginType
) {
}
