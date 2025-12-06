package io.github.siyukio.tools.api.definition;

import lombok.Builder;

import java.util.List;

/**
 * @author Bugee
 */
@Builder
public record ApiResponseParameter(

        String name,

        ApiSchema schema,

        List<ApiResponseParameter> properties,

        ApiResponseParameter items
) {
}
