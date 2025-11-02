package io.github.siyukio.tools.entity.definition;

import java.util.List;

/**
 * @author Bugee
 */
public record EntityDefinition(
        String dbName,
        String schema,
        String table,
        String comment,
        boolean createTableAuto,
        boolean addColumnAuto,
        boolean createIndexAuto,
        KeyDefinition keyDefinition,
        List<ColumnDefinition> columnDefinitions,
        List<IndexDefinition> indexDefinitions
) {
}
