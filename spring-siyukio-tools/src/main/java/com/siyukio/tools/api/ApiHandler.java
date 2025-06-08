package com.siyukio.tools.api;

import com.siyukio.tools.api.definition.ApiDefinition;
import com.siyukio.tools.api.parameter.request.RequestValidator;
import com.siyukio.tools.api.parameter.response.ResponseFilter;
import lombok.Builder;

/**
 * @author Buddy
 */

@Builder
public record ApiHandler(
        ApiDefinition apiDefinition,
        RequestValidator requestValidator,
        ResponseFilter responseFilter,
        ApiInvoker apiInvoker
) {
}
