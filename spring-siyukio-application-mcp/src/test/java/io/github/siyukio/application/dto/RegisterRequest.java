package io.github.siyukio.application.dto;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;

@Builder
public record RegisterRequest(

        @ApiParameter
        String username,

        @ApiParameter(minLength = 6)
        String password,

        @ApiParameter(required = false)
        String nickname
) {
}
