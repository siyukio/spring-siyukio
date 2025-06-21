package io.github.siyukio.application.model.parameter;

import io.github.siyukio.tools.api.annotation.ApiParameter;
import lombok.ToString;

import java.util.Date;

/**
 * @author Buddy
 */
@ToString
@ApiParameter(description = "items")
public class ItemVo {

    @ApiParameter
    public String id;

    @ApiParameter
    public String title;

    @ApiParameter
    public Date lastUpdateDate;

}
