package io.github.siyukio.application.model.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
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

    @ApiParameter(required = false)
    public boolean bool;

}
