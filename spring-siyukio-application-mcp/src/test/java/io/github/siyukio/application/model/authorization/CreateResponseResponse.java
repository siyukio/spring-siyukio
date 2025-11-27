package io.github.siyukio.application.model.authorization;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;

import java.time.LocalDateTime;

/**
 * @author Buddy
 */
@Builder
@With
public record CreateResponseResponse(

        @ApiParameter
        String authorization,

        @ApiParameter
        LocalDateTime createdAt
) {
}
