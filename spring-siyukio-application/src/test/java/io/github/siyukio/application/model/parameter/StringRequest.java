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
public class StringRequest {

    @ApiParameter
    public String required;

    @ApiParameter(minLength = 1, maxLength = 3, required = false)
    public String length;

    @ApiParameter(regex = "123|456", required = false)
    public String regex;

}
