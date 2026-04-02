package io.github.siyukio.application.dto;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;

@Builder
public record AuthResponse(

        @ApiParameter
        String userId,

        @ApiParameter
        String nickname,

        @ApiParameter
        String accessToken,

        @ApiParameter
        String refreshToken
) {
}
