package io.github.siyukio.tools.api.definition;

import lombok.Builder;

import java.util.List;

/**
 * @author Bugee
 */
@Builder
public record ApiRequestParameter(

        String name,

        Boolean required,

        String error,

        ApiSchema schema,

        List<ApiRequestParameter> properties,

        ApiRequestParameter items
) {
}
