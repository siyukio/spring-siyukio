package io.github.siyukio.postgresql.support;

import org.springframework.util.StringUtils;

/**
 * Sql utilities used to migrate data of range partitioned tables.
 *
 * @author Bugee
 */
public abstract class PgPartitionMigrationUtils {

    /**
     * Template of the sql which queries the child partitions and their range bounds of a range partitioned table.
     * <p>
     * The placeholders are: the partitioned table name, the range from and the range to.
     * Only the partitions which overlap with the range [from, to) are returned.
     */
    public final static String QUERY_PARTITIONS_TEMPLATE = """
            SELECT
                partition_name,
                partition_from,
                partition_to
            FROM (
                SELECT
                    inhrelid::regclass::text AS partition_name,
                    (regexp_match(pg_get_expr(pg_class.relpartbound, inhrelid), 'FROM \\(''([0-9]+)''\\)'))[1]::bigint AS partition_from,
                    (regexp_match(pg_get_expr(pg_class.relpartbound, inhrelid), 'TO \\(''([0-9]+)''\\)'))[1]::bigint AS partition_to
                FROM pg_inherits
                JOIN pg_class ON pg_inherits.inhrelid = pg_class.oid
                WHERE inhparent = '%s'::regclass
                    AND pg_get_expr(pg_class.relpartbound, inhrelid) ~ 'FOR VALUES FROM \\(''([0-9]+)''\\) TO \\(''([0-9]+)''\\)'
            ) AS partitions
            WHERE partition_to >= %d
                AND partition_from < %d
            ORDER BY partition_from ;
            """;

    /**
     * Builds the sql which queries the child partitions of the partitioned table.
     *
     * @param schema the schema of the partitioned table, 'public' is used when it is empty
     * @param table  the partitioned table name
     * @param from   the lower bound of the range, inclusive
     * @param to     the upper bound of the range, exclusive
     * @return the sql which queries the child partitions
     */
    public static String queryPartitionsSql(String schema, String table, long from, long to) {
        if (!StringUtils.hasText(schema)) {
            schema = PgSqlUtils.DEFAULT_SCHEMA;
        }
        return String.format(QUERY_PARTITIONS_TEMPLATE, schema + "." + table, from, to);
    }

    /**
     * Template of the sql which detaches a child partition from its partitioned table.
     * <p>
     * The placeholders are: the partitioned table name, the child partition name.
     */
    public final static String DETACH_PARTITION_TEMPLATE = "ALTER TABLE %s DETACH PARTITION %s ;";

    /**
     * Builds the sql which detaches the child partition from the partitioned table.
     *
     * @param schema    the schema of the partitioned table, 'public' is used when it is empty
     * @param table     the partitioned table name
     * @param partition the child partition name, it may already contain the schema prefix
     * @return the sql which detaches the child partition
     */
    public static String detachPartitionSql(String schema, String table, String partition) {
        if (!StringUtils.hasText(schema)) {
            schema = PgSqlUtils.DEFAULT_SCHEMA;
        }
        if (!StringUtils.hasText(partition)) {
            throw new IllegalArgumentException("The partition name must not be empty.");
        }
        // The partition name queried by 'regclass::text' may already contain the schema prefix
        if (!partition.contains(".")) {
            partition = schema + "." + partition;
        }
        return String.format(DETACH_PARTITION_TEMPLATE, schema + "." + table, partition);
    }

    /**
     * Template of the sql which migrates the data of a partition into another partition.
     * <p>
     * The placeholders are: the target partition name, the source partition name.
     */
    public final static String MIGRATE_PARTITION_TEMPLATE = "INSERT INTO %s SELECT * FROM %s ;";

    /**
     * Builds the sql which migrates the data of the source partition into the target partition.
     *
     * @param schema          the schema of the partitions, 'public' is used when it is empty
     * @param targetPartition the partition which the data is inserted into, it may already contain the schema prefix
     * @param sourcePartition the partition which the data is selected from, it may already contain the schema prefix
     * @return the sql which migrates the data of the source partition
     */
    public static String migratePartitionSql(String schema, String targetPartition, String sourcePartition) {
        if (!StringUtils.hasText(schema)) {
            schema = PgSqlUtils.DEFAULT_SCHEMA;
        }
        if (!StringUtils.hasText(targetPartition)) {
            throw new IllegalArgumentException("The target partition name must not be empty.");
        }
        if (!StringUtils.hasText(sourcePartition)) {
            throw new IllegalArgumentException("The source partition name must not be empty.");
        }
        // The partition names queried by 'regclass::text' may already contain the schema prefix
        if (!targetPartition.contains(".")) {
            targetPartition = schema + "." + targetPartition;
        }
        if (!sourcePartition.contains(".")) {
            sourcePartition = schema + "." + sourcePartition;
        }
        return String.format(MIGRATE_PARTITION_TEMPLATE, targetPartition, sourcePartition);
    }
}
