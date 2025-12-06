package io.github.siyukio.tools.api.definition;

import lombok.Builder;

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
        List<String> roles,
        boolean authorization,
        boolean signature,
        boolean mcpTool,
        ApiRequestParameter requestBodyParameter,
        ApiResponseParameter responseBodyParameter,
        Class<?> returnType,
        Class<?> realReturnType
) {
}
