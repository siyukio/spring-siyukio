package io.github.siyukio.application.dto.signature;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;

/**
 * @author Buddy
 */
@Builder
@With
public record SignatureRequest(

        @ApiParameter
        int num,

        @ApiParameter
        String text,

        @ApiParameter
        boolean bool
) {
}
