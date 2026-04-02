package io.github.siyukio.application.dto;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;

@Builder
public record LoginRequest(

        @ApiParameter
        String username,

        @ApiParameter
        String password
) {
}
