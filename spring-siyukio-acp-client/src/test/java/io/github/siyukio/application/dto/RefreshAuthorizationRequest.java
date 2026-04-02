package io.github.siyukio.application.dto;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;

/**
 * @author Bugee
 */
@Builder
@With
public record RefreshAuthorizationRequest(

        @ApiParameter
        String refreshToken
) {

}
