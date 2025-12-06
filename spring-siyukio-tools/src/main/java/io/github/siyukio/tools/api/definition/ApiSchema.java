package io.github.siyukio.tools.api.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.EnumNamingStrategies;
import com.fasterxml.jackson.databind.annotation.EnumNaming;
import lombok.Builder;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Record for representing OpenAPI Schema structure
 *
 * @author Bugee
 */
@Builder
public record ApiSchema(
        String description,
        Type type,
        List<String> required,
        @JsonProperty("default")
        Object defaultValue,
        String pattern,
        List<Example> examples,
        String example,
        Long maximum,
        Long minimum,
        Integer maxItems,
        Integer minItems,
        Integer maxLength,
        Integer minLength,
        String format,
        Boolean additionalProperties,
        Map<String, ApiSchema> properties,
        @Nullable
        ApiSchema items
) {

    @EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
    public enum Type {
        STRING,
        INTEGER,
        NUMBER,
        BOOLEAN,
        ARRAY,
        OBJECT;
    }

    @Builder
    public record Example(
            String value,
            String summary
    ) {
    }
}
