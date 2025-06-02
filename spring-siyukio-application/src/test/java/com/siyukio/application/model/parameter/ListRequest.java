package com.siyukio.application.model.parameter;

import com.siyukio.tools.api.annotation.ApiParameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * @author Buddy
 */
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListRequest {

    @ApiParameter(required = false, minItems = 3, maxItems = 6)
    public List<String> stringList;

    @ApiParameter(required = false, minItems = 3, maxItems = 6)
    public List<Integer> numList;

}
