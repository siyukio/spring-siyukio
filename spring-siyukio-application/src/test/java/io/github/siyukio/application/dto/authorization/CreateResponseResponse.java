package io.github.siyukio.application.dto.authorization;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;

/**
 * @author Buddy
 */
@Builder
@With
public record CreateResponseResponse(

        @ApiParameter
        String authorization
) {
}
