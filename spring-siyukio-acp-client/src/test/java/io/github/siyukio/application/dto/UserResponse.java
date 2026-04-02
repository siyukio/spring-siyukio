package io.github.siyukio.application.dto;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record UserResponse(

        @ApiParameter
        String id,

        @ApiParameter
        String nickname,

        @ApiParameter
        List<String> roles,

        @ApiParameter
        LocalDateTime createdAt
) {
}
