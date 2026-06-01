package io.github.siyukio.tools.api.definition;

import lombok.Builder;
import lombok.With;

import java.util.List;

/**
 * @author Buddy
 */

@Builder
public record ApiDefinition(
        String id,
        List<String> paths,
        String summary,
        String description,
        boolean deprecated,
        List<String> tags,
        Authorization authorization,
        boolean signature,
        boolean acpAvailable,
        ApiRequestParameter requestBodyParameter,
        ApiResponseParameter responseBodyParameter,
        Class<?> returnType,
        Class<?> realReturnType
) {

    @Builder
    @With
    public record Authorization(
            String type,
            List<String> scopes,
            String actorType
    ) {

    }
}
