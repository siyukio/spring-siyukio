package io.github.siyukio.application.dto;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RegisterResponse(

        @ApiParameter
        String userId,

        @ApiParameter
        String nickname,

        @ApiParameter
        LocalDateTime createdAt
) {
}
