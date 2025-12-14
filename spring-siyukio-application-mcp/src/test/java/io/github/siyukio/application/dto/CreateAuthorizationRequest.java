package io.github.siyukio.application.dto;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;

import java.util.List;

/**
 * @author Buddy
 */
@Builder
@With
public record CreateAuthorizationRequest(

        @ApiParameter
        String uid,

        @ApiParameter(required = false)
        String name,

        @ApiParameter(required = false)
        List<String> roles
) {

}
