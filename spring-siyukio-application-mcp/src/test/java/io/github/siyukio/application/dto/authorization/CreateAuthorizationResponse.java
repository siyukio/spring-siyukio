package io.github.siyukio.application.dto.authorization;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;

import java.time.LocalDateTime;

/**
 * @author Buddy
 */
@Builder
@With
public record CreateAuthorizationResponse(

        @ApiParameter
        String accessToken,

        @ApiParameter
        String refreshToken,

        @ApiParameter
        LocalDateTime createdAt
) {
}
