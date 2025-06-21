package io.github.siyukio.tools.api.definition;

import lombok.Builder;
import org.json.JSONArray;

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
        boolean sampling,
        JSONArray requestParameters,
        JSONArray responseParameters,
        Class<?> returnType,
        Class<?> realReturnType
) {
}
