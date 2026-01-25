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
        boolean encrypted,
        String keyInfo,
        Partition partition,
        KeyDefinition keyDefinition,
        List<ColumnDefinition> columnDefinitions,
        List<IndexDefinition> indexDefinitions
) {
    /**
     * Partitioning strategy for table partitioning.
     */
    public enum Partition {
        /**
         * No partitioning (default).
         */
        NONE,

        /**
         * Partition by year.
         */
        YEAR,

        /**
         * Partition by month.
         */
        MONTH,

        /**
         * Partition by day.
         */
        DAY,

        /**
         * Partition by hour.
         */
        HOUR
    }
}
