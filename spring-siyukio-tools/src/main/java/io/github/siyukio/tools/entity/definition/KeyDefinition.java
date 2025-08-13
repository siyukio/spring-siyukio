package io.github.siyukio.tools.entity.definition;

import io.github.siyukio.tools.entity.ColumnType;

/**
 * @author Bugee
 */

public record KeyDefinition(
        String fieldName,
        String columnName,
        ColumnType type,
        String comment
) {
}
