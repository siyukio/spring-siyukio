package io.github.siyukio.application.model.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author Buddy
 */
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NumRequest {

    @ApiParameter(required = false, minimum = 6, maximum = 10)
    public int intNum;

    @ApiParameter(required = false, minimum = 6, maximum = 10)
    public double doubleNum;

    @ApiParameter(required = false, minimum = 1000, maximum = 99999)
    public BigDecimal bigNum;

}
