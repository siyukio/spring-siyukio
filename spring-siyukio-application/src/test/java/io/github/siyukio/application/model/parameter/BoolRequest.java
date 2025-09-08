package io.github.siyukio.application.model.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import io.github.siyukio.tools.api.annotation.Example;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author Buddy
 */
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoolRequest {

    @ApiParameter(required = false,
            examples = {
                    @Example(value = "true", summary = "true"),
                    @Example(value = "false", summary = "false")
            })
    public boolean bool;

}
