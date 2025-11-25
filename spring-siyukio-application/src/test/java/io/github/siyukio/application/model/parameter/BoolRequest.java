package io.github.siyukio.application.model.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import io.github.siyukio.tools.api.annotation.Example;
import lombok.Builder;
import lombok.With;

/**
 * @author Buddy
 */
@Builder
@With
public record BoolRequest(

        @ApiParameter(required = false,
                examples = {
                        @Example(value = "true", summary = "true"),
                        @Example(value = "false", summary = "false")
                })
        boolean bool
) {
}
