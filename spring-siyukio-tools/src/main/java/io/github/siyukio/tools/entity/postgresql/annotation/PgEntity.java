package io.github.siyukio.tools.entity.postgresql.annotation;

import io.github.siyukio.tools.entity.definition.EntityDefinition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Bugee
 */

@Target(value = {ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface PgEntity {

    /**
     * Automatically create table if it does not exist.
     *
     * @return true if auto-create is enabled, false otherwise
     */
    boolean createTableAuto() default true;

    /**
     * Automatically add columns if they do not exist.
     *
     * @return true if auto-add column is enabled, false otherwise
     */
    boolean addColumnAuto() default true;

    /**
     * Automatically create indexes if they do not exist.
     *
     * @return true if auto-create index is enabled, false otherwise
     */
    boolean createIndexAuto() default true;

    /**
     * Database name for multi-database support.
     * <p>
     * Empty string uses the default database.
     *
     * @return database name
     */
    String dbName() default "";

    /**
     * Schema name for multi-schema support.
     * <p>
     * Empty string uses the default schema (typically 'public').
     *
     * @return schema name
     */
    String schema() default "";

    /**
     * Table name.
     * <p>
     * Empty string uses the record class name in snake_case.
     *
     * @return table name
     */
    String table() default "";

    /**
     * Table partitioning strategy.
     * <p>
     * Supports: NONE, YEAR, MONTH, DAY, HOUR.
     * <p>
     * NONE means no partitioning (default).
     *
     * @return partitioning strategy
     */
    EntityDefinition.Partition partition() default EntityDefinition.Partition.NONE;

    /**
     * Table comment/description.
     *
     * @return table comment
     */
    String comment();

    /**
     * Index definitions for the table.
     *
     * @return array of index configurations
     */
    PgIndex[] indexes() default {};

    /**
     * Contextual information for key derivation.
     * <p>
     * Used as the 'info' parameter in HMAC-SHA256 key derivation for
     * encrypting sensitive fields. This allows different entities to use
     * different derived keys from the same master key and salt, providing
     * key isolation between entity types.
     * <p>
     * Empty string will use the entity class simple name as the default context.
     *
     * @return contextual information for key derivation
     */
    String keyInfo() default "";
}
