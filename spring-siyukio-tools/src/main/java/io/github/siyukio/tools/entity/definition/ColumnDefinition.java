package io.github.siyukio.tools.entity.definition;

import io.github.siyukio.tools.entity.ColumnType;

/**
 * @author Bugee
 */
public record ColumnDefinition(
        String fieldName,
        String columnName,
        ColumnType type,
        Object defaultValue,
        boolean encrypted,
        String comment
) {
}
