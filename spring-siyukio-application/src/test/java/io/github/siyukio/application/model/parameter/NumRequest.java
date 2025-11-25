package io.github.siyukio.application.model.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.Builder;
import lombok.With;

import java.math.BigDecimal;

/**
 * @author Buddy
 */
@Builder
@With
public record NumRequest(

        @ApiParameter(required = false, minimum = 6, maximum = 10)
        int intNum,

        @ApiParameter(required = false, minimum = 6, maximum = 10)
        double doubleNum,

        @ApiParameter(required = false, minimum = 1000, maximum = 99999)
        BigDecimal bigNum
) {

}
