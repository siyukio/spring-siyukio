package io.github.siyukio.tools.api;

import io.github.siyukio.tools.api.definition.ApiDefinition;
import io.github.siyukio.tools.api.parameter.request.RequestValidator;
import io.github.siyukio.tools.api.parameter.response.ResponseFilter;
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
