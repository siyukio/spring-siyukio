package io.github.siyukio.tools.api.model;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author Buddy
 */
@ToString
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PageRequest {

    @ApiParameter(description = "page number", minimum = 1, maximum = 1000, required = false, defaultValue = "1")
    public Integer page;

    @ApiParameter(description = "number of records per page.", minimum = 1, maximum = 1000, required = false, defaultValue = "20")
    public Integer pageSize;
}
