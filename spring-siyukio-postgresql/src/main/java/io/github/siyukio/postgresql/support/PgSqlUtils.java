package io.github.siyukio.postgresql.support;

import io.github.siyukio.tools.entity.ColumnType;
import io.github.siyukio.tools.entity.definition.ColumnDefinition;
import io.github.siyukio.tools.entity.definition.EntityDefinition;
import io.github.siyukio.tools.entity.definition.IndexDefinition;
import io.github.siyukio.tools.entity.definition.KeyDefinition;
import io.github.siyukio.tools.entity.query.*;
import io.github.siyukio.tools.entity.sort.FieldSortBuilder;
import io.github.siyukio.tools.entity.sort.ListSortBuilder;
import io.github.siyukio.tools.entity.sort.SortBuilder;
import io.github.siyukio.tools.util.XDataUtils;
import org.json.JSONObject;
import org.postgresql.util.PGobject;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.*;

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
                ordinal_position ;
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

    private final static String ADD_COLUMN_TEMPLATE = " ALTER TABLE %s.%s ADD COLUMN %s ;";

    private final static String ALTER_COLUMN_DEFAULT_TEMPLATE = " ALTER TABLE %s.%s ALTER COLUMN %s SET DEFAULT %s ;";

    private final static String ALTER_COLUMN_TYPE_TEMPLATE = " ALTER TABLE %s.%s ALTER COLUMN %s TYPE %s ;";

    private final static String CREATE_TABLE_TEMPLATE = """
            CREATE TABLE IF NOT EXISTS %s.%s (
                %s
            );
            """;

    private final static String CREATE_INDEX_TEMPLATE = "CREATE INDEX %s ON %s.%s ( %s ) ;";

    private final static String INSERT_TEMPLATE = """
            INSERT INTO %s.%s ( %s )
            VALUES ( %s );
            """;

    private final static String INSERT_AND_RETURN_ID_TEMPLATE = """
            INSERT INTO %s.%s ( %s )
            VALUES ( %s )
            RETURNING %s;
            """;

    private final static String UPDATE_TEMPLATE = """
            UPDATE %s.%s
            SET %s
            WHERE %s;
            """;

    private final static String UPSERT_TEMPLATE = """
            INSERT INTO %s.%s ( %s )
            VALUES ( %s )
            ON CONFLICT ( %s )
            DO UPDATE SET %s ;
            """;

    private final static String DELETE_BY_QUERY_TEMPLATE = """
            DELETE FROM %s.%s
            WHERE %s;
            """;

    private final static String COUNT_TEMPLATE = """
            SELECT COUNT(*)
            FROM %s.%s;
            """;

    private final static String COUNT_BY_QUERY_TEMPLATE = """
            SELECT COUNT(*)
            FROM %s.%s
            WHERE %s;
            """;

    private final static String SORT_TEMPLATE = """
            ORDER BY %s
            """;

    private final static String QUERY_TEMPLATE = """
            SELECT * FROM %s.%s
            WHERE %s
            %s
            LIMIT ? OFFSET ?;
            """;

    private final static String QUERY_BY_ID_TEMPLATE = """
            SELECT * FROM %s.%s
            WHERE %s;
            """;

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
        if (columnDefinition.type() != ColumnType.JSON_OBJECT) {
            partList.add("DEFAULT");
            String defaultValue = getSqlDefault(columnDefinition);
            partList.add(defaultValue);
        }
        return String.join(" ", partList);
    }

    private static String getSchema(EntityDefinition entityDefinition) {
        String schema = entityDefinition.schema();
        if (!StringUtils.hasText(schema)) {
            schema = DEFAULT_SCHEMA;
        }
        return schema;
    }

    public static List<String> addColumnAndCommentSql(EntityDefinition entityDefinition, ColumnDefinition columnDefinition) {
        List<String> sqlList = new ArrayList<>();
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();
        String columnDefinitionSql = getColumnDefinitionSql(columnDefinition);

        String alterSql = String.format(ADD_COLUMN_TEMPLATE, schema, table, columnDefinitionSql);

        sqlList.add(alterSql);

        String commentSql = String.format(COLUMN_COMMENT_TEMPLATE,
                schema + "." + table + "." + columnDefinition.columnName(),
                columnDefinition.comment());
        sqlList.add(commentSql);
        return sqlList;
    }

    public static String alterColumnDefaultSql(EntityDefinition entityDefinition, ColumnDefinition columnDefinition) {
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();
        String columnName = columnDefinition.columnName();
        String defaultValue = getSqlDefault(columnDefinition);
        return String.format(ALTER_COLUMN_DEFAULT_TEMPLATE, schema, table, columnName, defaultValue);
    }

    public static String alterColumnTypeSql(EntityDefinition entityDefinition, ColumnDefinition columnDefinition) {
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();
        String columnName = columnDefinition.columnName();
        String sqlType = getSqlType(columnDefinition);
        return String.format(ALTER_COLUMN_TYPE_TEMPLATE, schema, table, columnName, sqlType);
    }

    public static List<String> createTableAndCommentSql(EntityDefinition entityDefinition) {
        List<String> sqlList = new ArrayList<>();

        //create table
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();
        List<String> columnDefinitionSqlList = new ArrayList<>();
        columnDefinitionSqlList.add(getKeyDefinitionSql(entityDefinition.keyDefinition()));
        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            columnDefinitionSqlList.add(getColumnDefinitionSql(columnDefinition));
        }
        String columnDefinitions = String.join("," + System.lineSeparator(), columnDefinitionSqlList);
        String createTableSql = String.format(CREATE_TABLE_TEMPLATE, schema, table, columnDefinitions);
        sqlList.add(createTableSql);

        //comment
        sqlList.add(String.format(TABLE_COMMENT_TEMPLATE, schema + "." + table, entityDefinition.comment()));

        KeyDefinition keyDefinition = entityDefinition.keyDefinition();
        if (StringUtils.hasText(keyDefinition.comment())) {
            sqlList.add(String.format(COLUMN_COMMENT_TEMPLATE,
                    schema + "." + table + "." + keyDefinition.columnName(),
                    keyDefinition.comment()));
        }

        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            if (StringUtils.hasText(columnDefinition.comment())) {
                sqlList.add(String.format(COLUMN_COMMENT_TEMPLATE,
                        schema + "." + table + "." + columnDefinition.columnName(),
                        columnDefinition.comment()));
            }
        }
        return sqlList;
    }

    public static String createIndexSql(EntityDefinition entityDefinition, IndexDefinition indexDefinition) {
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();
        String indexName = indexDefinition.indexName();
        List<String> columnNameList = Arrays.asList(indexDefinition.columns());
        String columns = String.join(", ", columnNameList);
        return String.format(CREATE_INDEX_TEMPLATE, indexName, schema, table, columns);
    }

    public static String insertAndReturnIdSql(EntityDefinition entityDefinition) {
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();

        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            columns.add(columnDefinition.columnName());
            values.add("?");
        }

        return String.format(INSERT_AND_RETURN_ID_TEMPLATE, schema, table,
                String.join(",", columns), String.join(",", values),
                entityDefinition.keyDefinition().columnName());
    }

    public static List<Object> insertAndReturnIdValues(EntityDefinition entityDefinition, JSONObject entityJson) {
        List<Object> values = new ArrayList<>();

        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            values.add(field2RowValue(entityJson, columnDefinition));
        }

        return values;
    }

    public static String insertSql(EntityDefinition entityDefinition) {
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();

        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();

        KeyDefinition keyDefinition = entityDefinition.keyDefinition();
        columns.add(keyDefinition.columnName());
        values.add("?");
        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            columns.add(columnDefinition.columnName());
            values.add("?");
        }

        return String.format(INSERT_TEMPLATE, schema, table, String.join(",", columns), String.join(",", values));
    }

    public static List<Object> insertValues(EntityDefinition entityDefinition, JSONObject entityJson) {
        List<Object> values = new ArrayList<>();

        KeyDefinition keyDefinition = entityDefinition.keyDefinition();
        values.add(entityJson.opt(keyDefinition.fieldName()));
        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            values.add(field2RowValue(entityJson, columnDefinition));
        }
        return values;
    }

    public static String updateByIdSql(EntityDefinition entityDefinition) {
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();

        List<String> columns = new ArrayList<>();
        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            columns.add(columnDefinition.columnName() + " = ?");
        }
        KeyDefinition keyDefinition = entityDefinition.keyDefinition();
        String where = keyDefinition.columnName() + " = ?";

        return String.format(UPDATE_TEMPLATE, schema, table, String.join(", ", columns), where);
    }

    public static List<Object> updateValues(EntityDefinition entityDefinition, JSONObject entityJson) {
        List<Object> values = new ArrayList<>();

        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            values.add(field2RowValue(entityJson, columnDefinition));
        }

        KeyDefinition keyDefinition = entityDefinition.keyDefinition();
        values.add(entityJson.opt(keyDefinition.fieldName()));

        return values;
    }

    public static String upsertSql(EntityDefinition entityDefinition) {
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();

        List<String> insertColumns = new ArrayList<>();
        List<String> insertValues = new ArrayList<>();

        List<String> updateColumnValues = new ArrayList<>();

        KeyDefinition keyDefinition = entityDefinition.keyDefinition();
        insertColumns.add(keyDefinition.columnName());
        insertValues.add("?");
        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            insertColumns.add(columnDefinition.columnName());
            insertValues.add("?");

            if (columnDefinition.fieldName().equals("createAt") || columnDefinition.fieldName().equals("createTime")) {
                continue;
            }
            updateColumnValues.add(columnDefinition.columnName() + " = ?");
        }

        return String.format(UPSERT_TEMPLATE, schema, table, String.join(",", insertColumns), String.join(",", insertValues),
                keyDefinition.columnName(), String.join(", ", updateColumnValues));
    }

    public static List<Object> upsertValues(EntityDefinition entityDefinition, JSONObject entityJson) {
        List<Object> values = new ArrayList<>();
        //Construct insert values.
        KeyDefinition keyDefinition = entityDefinition.keyDefinition();
        values.add(entityJson.opt(keyDefinition.fieldName()));
        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            values.add(field2RowValue(entityJson, columnDefinition));
        }
        //Construct update values, excluding thi fields createAt and createTime.
        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            if (columnDefinition.fieldName().equals("createAt") || columnDefinition.fieldName().equals("createTime")) {
                continue;
            }
            values.add(field2RowValue(entityJson, columnDefinition));
        }
        return values;
    }

    public static String deleteByIdSql(EntityDefinition entityDefinition) {
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();

        KeyDefinition keyDefinition = entityDefinition.keyDefinition();
        String where = keyDefinition.columnName() + " = ?";

        return String.format(DELETE_BY_QUERY_TEMPLATE, schema, table, where);
    }

    public static String queryByIdSql(EntityDefinition entityDefinition) {
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();

        KeyDefinition keyDefinition = entityDefinition.keyDefinition();
        String where = keyDefinition.columnName() + " = ?";

        return String.format(QUERY_BY_ID_TEMPLATE, schema, table, where);
    }

    private static String escape(String keyword) {
        if (keyword == null) {
            return null;
        }
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    public static String toConditionSql(MatchQueryBuilder matchQueryBuilder, Map<String, String> dictionaryMap) {
        String columnName = dictionaryMap.get(matchQueryBuilder.getFieldName());
        if (!StringUtils.hasText(columnName)) {
            columnName = matchQueryBuilder.getFieldName();
        }
        return columnName + " LIKE '%' || ? || '%' ESCAPE '\\'";
    }

    public static List<Object> toConditionValue(MatchQueryBuilder matchQueryBuilder) {
        return List.of(escape(matchQueryBuilder.getText()));
    }

    public static String toConditionSql(WildcardQueryBuilder wildcardQueryBuilder, Map<String, String> dictionaryMap) {
        String columnName = dictionaryMap.get(wildcardQueryBuilder.getFieldName());
        if (!StringUtils.hasText(columnName)) {
            columnName = wildcardQueryBuilder.getFieldName();
        }
        String value = "?";
        if (wildcardQueryBuilder.isPrefix()) {
            value = value + " || '%";
        }
        if (wildcardQueryBuilder.isSuffix()) {
            value = "'%' || " + value;
        }
        return columnName + " LIKE " + value + " ESCAPE '\\'";
    }

    public static List<Object> toConditionValue(WildcardQueryBuilder wildcardQueryBuilder) {
        return List.of(escape(wildcardQueryBuilder.getText()));
    }

    public static String toConditionSql(RangeQueryBuilder rangeQueryBuilder, Map<String, String> dictionaryMap) {
        String columnName = dictionaryMap.get(rangeQueryBuilder.getFieldName());
        if (!StringUtils.hasText(columnName)) {
            columnName = rangeQueryBuilder.getFieldName();
        }
        List<String> conditions = new ArrayList<>();

        if (rangeQueryBuilder.getMinValue() != null) {
            if (rangeQueryBuilder.isGt()) {
                conditions.add(columnName + " > ?");
            } else if (rangeQueryBuilder.isGte()) {
                conditions.add(columnName + " >= ?");
            }
        }
        if (rangeQueryBuilder.getMaxValue() != null) {
            if (rangeQueryBuilder.isLt()) {
                conditions.add(columnName + " < ?");
            } else if (rangeQueryBuilder.isLte()) {
                conditions.add(columnName + " <= ?");
            }
        }

        if (conditions.isEmpty()) {
            return "";
        }
        return "(" + String.join(" AND ", conditions) + ")";
    }

    public static List<Object> toConditionValue(RangeQueryBuilder rangeQueryBuilder) {
        List<Object> conditionValues = new ArrayList<>();
        if (rangeQueryBuilder.getMinValue() != null) {
            if (rangeQueryBuilder.isGt()) {
                conditionValues.add(rangeQueryBuilder.getMinValue());
            } else if (rangeQueryBuilder.isGte()) {
                conditionValues.add(rangeQueryBuilder.getMinValue());
            }
        }
        if (rangeQueryBuilder.getMaxValue() != null) {
            if (rangeQueryBuilder.isLt()) {
                conditionValues.add(rangeQueryBuilder.getMaxValue());
            } else if (rangeQueryBuilder.isLte()) {
                conditionValues.add(rangeQueryBuilder.getMaxValue());
            }
        }
        return conditionValues;
    }

    public static String toConditionSql(TermsQueryBuilder termsQueryBuilder, Map<String, String> dictionaryMap) {
        String columnName = dictionaryMap.get(termsQueryBuilder.getFieldName());
        if (!StringUtils.hasText(columnName)) {
            columnName = termsQueryBuilder.getFieldName();
        }
        return columnName + " = ANY(?)";
    }

    public static List<Object> toConditionValue(TermsQueryBuilder termsQueryBuilder) {
        Set<Object> vlaueSet = termsQueryBuilder.getValueSet();
        return List.of(vlaueSet.toArray(new Object[0]));
    }

    public static String toConditionSql(TermQueryBuilder termQueryBuilder, Map<String, String> dictionaryMap) {
        String columnName = dictionaryMap.get(termQueryBuilder.getFieldName());
        if (!StringUtils.hasText(columnName)) {
            columnName = termQueryBuilder.getFieldName();
        }
        return columnName + " = ?";
    }

    public static List<Object> toConditionValue(TermQueryBuilder termQueryBuilder) {
        return List.of(termQueryBuilder.getValue());
    }

    public static String toConditionSql(BoolQueryBuilder boolQueryBuilder, Map<String, String> dictionaryMap) {
        List<String> boolConditions = new ArrayList<>();
        if (!boolQueryBuilder.getMustNotClauses().isEmpty()) {
            List<String> mustNotConditions = new ArrayList<>();
            String conditionSql;
            for (QueryBuilder mustNotClause : boolQueryBuilder.getMustNotClauses()) {
                conditionSql = toQuerySql(mustNotClause, dictionaryMap);
                if (StringUtils.hasText(conditionSql)) {
                    mustNotConditions.add(conditionSql);
                }
            }
            boolConditions.add("NOT (" + String.join(" AND ", mustNotConditions) + ")");
        }
        if (!boolQueryBuilder.getShouldClauses().isEmpty()) {
            List<String> shouldConditions = new ArrayList<>();
            String conditionSql;
            for (QueryBuilder shouldClause : boolQueryBuilder.getShouldClauses()) {
                conditionSql = toQuerySql(shouldClause, dictionaryMap);
                if (StringUtils.hasText(conditionSql)) {
                    shouldConditions.add(conditionSql);
                }
            }
            boolConditions.add("(" + String.join(" OR ", shouldConditions) + ")");
        }

        if (!boolQueryBuilder.getMustClauses().isEmpty()) {
            List<String> mustConditions = new ArrayList<>();
            String conditionSql;
            for (QueryBuilder mustClause : boolQueryBuilder.getMustClauses()) {
                conditionSql = toQuerySql(mustClause, dictionaryMap);
                if (StringUtils.hasText(conditionSql)) {
                    mustConditions.add(conditionSql);
                }
            }
            boolConditions.add("(" + String.join(" AND ", mustConditions) + ")");
        }
        if (boolConditions.isEmpty()) {
            return "";
        } else {
            return "(" + String.join(" AND ", boolConditions) + ")";
        }
    }

    public static List<Object> toConditionValue(BoolQueryBuilder boolQueryBuilder) {
        List<Object> boolConditionValues = new ArrayList<>();
        if (!boolQueryBuilder.getMustNotClauses().isEmpty()) {
            List<Object> conditionValues;
            for (QueryBuilder mustNotClause : boolQueryBuilder.getMustNotClauses()) {
                conditionValues = toQueryValues(mustNotClause);
                boolConditionValues.addAll(conditionValues);
            }
        }
        if (!boolQueryBuilder.getShouldClauses().isEmpty()) {
            List<Object> conditionValues;
            for (QueryBuilder shouldClause : boolQueryBuilder.getShouldClauses()) {
                conditionValues = toQueryValues(shouldClause);
                boolConditionValues.addAll(conditionValues);
            }
        }
        if (!boolQueryBuilder.getMustClauses().isEmpty()) {
            List<Object> conditionValues;
            for (QueryBuilder mustClauses : boolQueryBuilder.getMustClauses()) {
                conditionValues = toQueryValues(mustClauses);
                boolConditionValues.addAll(conditionValues);
            }
        }
        return boolConditionValues;
    }

    public static String toQuerySql(QueryBuilder queryBuilder, Map<String, String> dictionaryMap) {
        if (queryBuilder == null) {
            return "1 = 1";
        }
        String result = switch (queryBuilder) {
            case TermQueryBuilder termQueryBuilder -> toConditionSql(termQueryBuilder, dictionaryMap);
            case TermsQueryBuilder termsQueryBuilder -> toConditionSql(termsQueryBuilder, dictionaryMap);
            case RangeQueryBuilder rangeQueryBuilder -> toConditionSql(rangeQueryBuilder, dictionaryMap);
            case MatchQueryBuilder matchQueryBuilder -> toConditionSql(matchQueryBuilder, dictionaryMap);
            case WildcardQueryBuilder wildcardQueryBuilder -> toConditionSql(wildcardQueryBuilder, dictionaryMap);
            case BoolQueryBuilder boolQueryBuilder -> toConditionSql(boolQueryBuilder, dictionaryMap);
            default -> "1 = 1";
        };
        if (!StringUtils.hasText(result)) {
            result = "1 = 1";
        }
        return result;
    }

    public static List<Object> toQueryValues(QueryBuilder queryBuilder) {
        if (queryBuilder == null) {
            return List.of();
        }
        return switch (queryBuilder) {
            case TermQueryBuilder termQueryBuilder -> toConditionValue(termQueryBuilder);
            case TermsQueryBuilder termsQueryBuilder -> toConditionValue(termsQueryBuilder);
            case RangeQueryBuilder rangeQueryBuilder -> toConditionValue(rangeQueryBuilder);
            case MatchQueryBuilder matchQueryBuilder -> toConditionValue(matchQueryBuilder);
            case WildcardQueryBuilder wildcardQueryBuilder -> toConditionValue(wildcardQueryBuilder);
            case BoolQueryBuilder boolQueryBuilder -> toConditionValue(boolQueryBuilder);
            default -> List.of();
        };
    }

    public static String deleteByQuerySql(EntityDefinition entityDefinition, QueryBuilder queryBuilder, Map<String, String> dictionaryMap) {
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();
        String conditionSql = toQuerySql(queryBuilder, dictionaryMap);
        return String.format(DELETE_BY_QUERY_TEMPLATE, schema, table, conditionSql);
    }

    public static String countSql(EntityDefinition entityDefinition) {
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();
        return String.format(COUNT_TEMPLATE, schema, table);
    }

    public static String countByQuerySql(EntityDefinition entityDefinition, QueryBuilder queryBuilder, Map<String, String> dictionaryMap) {
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();
        String conditionSql = toQuerySql(queryBuilder, dictionaryMap);
        return String.format(COUNT_BY_QUERY_TEMPLATE, schema, table, conditionSql);
    }

    public static String toSortSql(FieldSortBuilder sortBuilder, Map<String, String> dictionaryMap) {
        String columnName = dictionaryMap.get(sortBuilder.getFieldName());
        if (!StringUtils.hasText(columnName)) {
            columnName = sortBuilder.getFieldName();
        }
        return columnName + " " + sortBuilder.getOrder().toString();
    }

    public static String toSortSql(SortBuilder sortBuilder, Map<String, String> dictionaryMap) {
        if (sortBuilder == null) {
            return "";
        }
        List<String> sortList = new ArrayList<>();
        if (sortBuilder instanceof FieldSortBuilder fieldSortBuilder) {
            sortList.add(toSortSql(fieldSortBuilder, dictionaryMap));
        } else if (sortBuilder instanceof ListSortBuilder listSortBuilder) {
            for (FieldSortBuilder fieldSortBuilder : listSortBuilder.getSortBuilderList()) {
                sortList.add(toSortSql(fieldSortBuilder, dictionaryMap));
            }
        }
        return String.format(SORT_TEMPLATE, String.join(",", sortList));
    }

    public static String querySql(EntityDefinition entityDefinition, QueryBuilder queryBuilder, SortBuilder sortBuilder, Map<String, String> dictionaryMap) {
        String schema = getSchema(entityDefinition);

        String table = entityDefinition.table();
        String conditionSql = toQuerySql(queryBuilder, dictionaryMap);
        String sortSql = toSortSql(sortBuilder, dictionaryMap);
        return String.format(QUERY_TEMPLATE, schema, table, conditionSql, sortSql);
    }

    private static Object field2RowValue(JSONObject entityJson, ColumnDefinition columnDefinition) {
        Object value = entityJson.opt(columnDefinition.fieldName());
        if (columnDefinition.type() == ColumnType.JSON_ARRAY || columnDefinition.type() == ColumnType.JSON_OBJECT) {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            try {
                if (value == null) {
                    jsonObject.setValue(null);
                } else {
                    jsonObject.setValue(XDataUtils.toJSONString(value));
                }
            } catch (SQLException ignored) {
            }
            value = jsonObject;
        }
        return value;
    }

    public static void jsonRow2FieldValue(JSONObject entityJson, ColumnDefinition columnDefinition) {
        String text = entityJson.optString(columnDefinition.fieldName());
        if (StringUtils.hasText(text)) {
            if (columnDefinition.type() == ColumnType.JSON_ARRAY) {
                entityJson.put(columnDefinition.fieldName(), XDataUtils.parseArray(text));
            } else {
                entityJson.put(columnDefinition.fieldName(), XDataUtils.parseObject(text));
            }
        }
    }
}
