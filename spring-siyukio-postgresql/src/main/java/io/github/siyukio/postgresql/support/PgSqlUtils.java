package io.github.siyukio.postgresql.support;

import io.github.siyukio.tools.entity.ColumnType;
import io.github.siyukio.tools.entity.definition.ColumnDefinition;
import io.github.siyukio.tools.entity.definition.EntityDefinition;
import io.github.siyukio.tools.entity.definition.IndexDefinition;
import io.github.siyukio.tools.entity.definition.KeyDefinition;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bugee
 */
public abstract class PgSqlUtils {

    public final static String DEFAULT_SCHEMA = "public";

    public final static String QUERY_COLUMNS_SQL = """
            SELECT
                column_name,
                data_type,
                udt_name,
                column_default
            FROM
                information_schema.columns
            WHERE
                table_schema = ?
                AND table_name = ?
            ORDER BY
                ordinal_position;
            """;

    public final static String QUERY_INDEXES_SQL = """
            SELECT
                indexname, 
                indexdef
            FROM
                pg_indexes
            WHERE
                schemaname = ?
                AND tablename = ? ;
            """;

    private final static String TABLE_COMMENT_TEMPLATE = "COMMENT ON TABLE %s IS '%s' ;";

    private final static String COLUMN_COMMENT_TEMPLATE = "COMMENT ON COLUMN %s IS '%s' ;";

    public static String createSchemaIfNotExistsSql(String schemaName) {
        return "CREATE SCHEMA IF NOT EXISTS " + schemaName;
    }

    private static String getKeyDefinitionSql(KeyDefinition keyDefinition) {
        List<String> partList = new ArrayList<>();
        String name = keyDefinition.columnName();
        partList.add(name);
        if (keyDefinition.type() == ColumnType.BIGINT || keyDefinition.type() == ColumnType.INT) {
            if (keyDefinition.type() == ColumnType.INT) {
                partList.add("INT");
            } else {
                partList.add("BIGINT");
            }
            if (keyDefinition.generated()) {
                partList.add("SERIAL");
            }
        } else {
            partList.add("TEXT");
        }
        partList.add("PRIMARY KEY");
        return String.join(" ", partList);
    }

    private static String getSqlType(ColumnDefinition columnDefinition) {
        return switch (columnDefinition.type()) {
            case ColumnType.INT -> "INT";
            case ColumnType.BIGINT -> "BIGINT";
            case ColumnType.DOUBLE -> "DOUBLE PRECISION";
            case ColumnType.BOOLEAN -> "BOOLEAN";
            case ColumnType.DATETIME -> "TIMESTAMP";
            case ColumnType.JSON_OBJECT, ColumnType.JSON_ARRAY -> "JSON";
            default -> "TEXT";
        };
    }

    private static String getSqlDefault(ColumnDefinition columnDefinition) {
        if (StringUtils.hasText(columnDefinition.defaultValue())) {
            return switch (columnDefinition.type()) {
                case ColumnType.INT, ColumnType.BIGINT, ColumnType.DOUBLE, ColumnType.BOOLEAN ->
                        columnDefinition.defaultValue();
                default -> "'" + columnDefinition.defaultValue() + "'";
            };
        }
        return "''";
    }

    private static String getColumnDefinitionSql(ColumnDefinition columnDefinition) {
        List<String> partList = new ArrayList<>();
        String name = columnDefinition.columnName();
        partList.add(name);
        String sqlType = getSqlType(columnDefinition);
        partList.add(sqlType);
        partList.add("NOT NULL");
        partList.add("DEFAULT");
        String defaultValue = getSqlDefault(columnDefinition);
        partList.add(defaultValue);
        return String.join(" ", partList);
    }

    public static List<String> addColumnAndCommentSql(EntityDefinition entityDefinition, ColumnDefinition columnDefinition) {
        List<String> sqlList = new ArrayList<>();
        String addColumnSqlTemplate = " ALTER TABLE %s.%s ADD COLUMN %s ;";
        String schema = entityDefinition.schema();
        if (!StringUtils.hasText(schema)) {
            schema = DEFAULT_SCHEMA;

        }

        String table = entityDefinition.table();
        String columnDefinitionSql = getColumnDefinitionSql(columnDefinition);

        String alterSql = String.format(addColumnSqlTemplate, schema, table, columnDefinitionSql);

        sqlList.add(alterSql);

        String commentSql = String.format(COLUMN_COMMENT_TEMPLATE,
                schema + "." + table + "." + columnDefinition.columnName(),
                columnDefinition.comment());
        sqlList.add(commentSql);
        return sqlList;
    }

    public static String alterColumnDefaultSql(EntityDefinition entityDefinition, ColumnDefinition columnDefinition) {
        String alterColumnDefaultSqlTemplate = " ALTER TABLE %s.%s ALTER COLUMN %s SET DEFAULT %s ;";
        String schema = entityDefinition.schema();
        if (!StringUtils.hasText(schema)) {
            schema = DEFAULT_SCHEMA;
        }

        String table = entityDefinition.table();
        String columnName = columnDefinition.columnName();
        String defaultValue = getSqlDefault(columnDefinition);
        return String.format(alterColumnDefaultSqlTemplate, schema, table, columnName, defaultValue);
    }

    public static String alterColumnTypeSql(EntityDefinition entityDefinition, ColumnDefinition columnDefinition) {
        String alterColumnTypeSqlTemplate = " ALTER TABLE %s.%s ALTER COLUMN %s TYPE %s ;";
        String schema = entityDefinition.schema();
        if (!StringUtils.hasText(schema)) {
            schema = DEFAULT_SCHEMA;
        }

        String table = entityDefinition.table();
        String columnName = columnDefinition.columnName();
        String sqlType = getSqlType(columnDefinition);
        return String.format(alterColumnTypeSqlTemplate, schema, table, columnName, sqlType);
    }

    public static List<String> createTableAndCommentSql(EntityDefinition entityDefinition) {
        List<String> sqlList = new ArrayList<>();

        //create table
        String createTableSqlTemplate = """
                CREATE TABLE IF NOT EXISTS %s.%s (
                    %s
                );
                """;
        String schema = entityDefinition.schema();
        if (!StringUtils.hasText(schema)) {
            schema = DEFAULT_SCHEMA;

        }

        String table = entityDefinition.table();
        List<String> columnDefinitionSqlList = new ArrayList<>();
        columnDefinitionSqlList.add(getKeyDefinitionSql(entityDefinition.keyDefinition()));
        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            columnDefinitionSqlList.add(getColumnDefinitionSql(columnDefinition));
        }
        String columnDefinitions = String.join("," + System.lineSeparator(), columnDefinitionSqlList);
        String createTableSql = String.format(createTableSqlTemplate, schema, table, columnDefinitions);
        sqlList.add(createTableSql);

        //comment
        sqlList.add(String.format(TABLE_COMMENT_TEMPLATE, schema + "." + table, entityDefinition.comment()));

        sqlList.add(String.format(COLUMN_COMMENT_TEMPLATE,
                schema + "." + table + "." + entityDefinition.keyDefinition().columnName(),
                entityDefinition.keyDefinition().comment()));

        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            sqlList.add(String.format(COLUMN_COMMENT_TEMPLATE,
                    schema + "." + table + "." + columnDefinition.columnName(),
                    columnDefinition.comment()));
        }
        return sqlList;
    }

    public static String createIndexSql(EntityDefinition entityDefinition, IndexDefinition indexDefinition) {
        String createIndexSqlTemplate = "CREATE INDEX %s ON %s.%s ( %s ) ;";
        String schema = entityDefinition.schema();
        if (!StringUtils.hasText(schema)) {
            schema = DEFAULT_SCHEMA;
        }

        String table = entityDefinition.table();
        String indexName = indexDefinition.indexName();
        List<String> columnNameList = new ArrayList<>();
        for (String column : indexDefinition.columns()) {
            columnNameList.add(column);
        }
        String columns = String.join(", ", columnNameList);
        return String.format(createIndexSqlTemplate, indexName, schema, table, columns);
    }
}
