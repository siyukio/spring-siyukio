package io.github.siyukio.postgresql.support;

import io.github.siyukio.tools.entity.ColumnType;
import io.github.siyukio.tools.entity.EntityExecutor;
import io.github.siyukio.tools.entity.definition.ColumnDefinition;
import io.github.siyukio.tools.entity.definition.EntityDefinition;
import io.github.siyukio.tools.entity.definition.KeyDefinition;
import io.github.siyukio.tools.entity.query.QueryBuilder;
import io.github.siyukio.tools.entity.sort.SortBuilder;
import io.github.siyukio.tools.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bugee
 */
public class PgEntityExecutor implements EntityExecutor {

    private final EntityDefinition entityDefinition;

    private final MultiJdbcTemplate multiJdbcTemplate;

    private final boolean generatedId;

    private final String insertSql;

    private final String updateByIdSql;

    private final String upsertSql;

    private final String deleteByIdSql;

    private final String countSql;

    private final String queryByIdSql;

    private final Map<String, String> fieldToColumnMap = new HashMap<>();

    private final Map<String, String> columnToFieldMap = new HashMap<>();

    public PgEntityExecutor(EntityDefinition entityDefinition, MultiJdbcTemplate multiJdbcTemplate) {
        this.entityDefinition = entityDefinition;
        this.multiJdbcTemplate = multiJdbcTemplate;
        KeyDefinition keyDefinition = entityDefinition.keyDefinition();
        this.generatedId = keyDefinition.generated() &&
                (keyDefinition.type() == ColumnType.BIGINT || keyDefinition.type() == ColumnType.INT);
        if (this.generatedId) {
            this.insertSql = PgSqlUtils.insertAndReturnIdSql(this.entityDefinition);
        } else {
            this.insertSql = PgSqlUtils.insertSql(this.entityDefinition);
        }
        this.updateByIdSql = PgSqlUtils.updateByIdSql(this.entityDefinition);
        this.deleteByIdSql = PgSqlUtils.deleteByIdSql(this.entityDefinition);
        this.upsertSql = PgSqlUtils.upsertSql(this.entityDefinition);
        this.queryByIdSql = PgSqlUtils.queryByIdSql(this.entityDefinition);
        this.countSql = PgSqlUtils.countSql(this.entityDefinition);

        this.fieldToColumnMap.put(keyDefinition.fieldName(), keyDefinition.columnName());
        for (ColumnDefinition columnDefinition : entityDefinition.columnDefinitions()) {
            this.fieldToColumnMap.put(columnDefinition.fieldName(), columnDefinition.columnName());
            this.columnToFieldMap.put(columnDefinition.columnName(), columnDefinition.fieldName());
        }
    }

    @Override
    public EntityDefinition getEntityDefinition() {
        return this.entityDefinition;
    }

    @Override
    public JSONObject insert(JSONObject entityJson) {
        if (this.generatedId) {
            Class<?> returnClass = Long.class;
            if (this.entityDefinition.keyDefinition().type() == ColumnType.INT) {
                returnClass = Integer.class;
            }
            List<Object> values = PgSqlUtils.insertAndReturnIdValues(this.entityDefinition, entityJson);
            Object id = this.multiJdbcTemplate.getMaster().queryForObject(this.insertSql, returnClass, values.toArray());
            entityJson.put(this.entityDefinition.keyDefinition().fieldName(), id);
        } else {
            List<Object> values = PgSqlUtils.insertValues(this.entityDefinition, entityJson);
            this.multiJdbcTemplate.getMaster().update(this.insertSql, values.toArray());
        }
        return entityJson;
    }

    @Override
    public int insertBatch(List<JSONObject> entityJsons) {
        int[][] result = this.multiJdbcTemplate.getMaster().batchUpdate(this.insertSql, entityJsons, entityJsons.size(),
                (ps, entityJson) -> {
                    List<Object> values;
                    if (this.generatedId) {
                        values = PgSqlUtils.insertAndReturnIdValues(this.entityDefinition, entityJson);
                    } else {
                        values = PgSqlUtils.insertValues(this.entityDefinition, entityJson);
                    }
                    for (int i = 0; i < values.size(); i++) {
                        ps.setObject(i + 1, values.get(i));
                    }
                });
        return result.length;
    }

    @Override
    public JSONObject update(JSONObject entityJson) {
        List<Object> values = PgSqlUtils.updateValues(this.entityDefinition, entityJson);
        this.multiJdbcTemplate.getMaster().update(this.updateByIdSql, values.toArray());
        return entityJson;
    }

    @Override
    public int updateBatch(List<JSONObject> entityJsons) {
        int[][] result = this.multiJdbcTemplate.getMaster().batchUpdate(this.updateByIdSql, entityJsons, entityJsons.size(),
                (ps, entityJson) -> {
                    List<Object> values = PgSqlUtils.updateValues(this.entityDefinition, entityJson);
                    for (int i = 0; i < values.size(); i++) {
                        ps.setObject(i + 1, values.get(i));
                    }
                });
        return result.length;
    }

    @Override
    public JSONObject upsert(JSONObject entityJson) {
        List<Object> values = PgSqlUtils.upsertValues(this.entityDefinition, entityJson);
        this.multiJdbcTemplate.getMaster().update(this.upsertSql, values.toArray());
        return entityJson;
    }

    @Override
    public int delete(Object id) {
        return this.multiJdbcTemplate.getMaster().update(this.deleteByIdSql, id);
    }

    @Override
    public void deleteBatch(List<Object> ids) {
        this.multiJdbcTemplate.getMaster().batchUpdate(this.deleteByIdSql, ids, ids.size(),
                (ps, id) -> {
                    ps.setObject(1, id);
                });
    }

    @Override
    public int deleteByQuery(QueryBuilder queryBuilder) {
        String deleteByQuerySql = PgSqlUtils.deleteByQuerySql(this.entityDefinition, queryBuilder, this.fieldToColumnMap);
        List<Object> queryValues = PgSqlUtils.toQueryValues(queryBuilder);
        return this.multiJdbcTemplate.getMaster().update(deleteByQuerySql, queryValues.toArray());
    }

    @Override
    public int count() {
        Integer count = this.multiJdbcTemplate.getRandomSlave().queryForObject(this.countSql, Integer.class);
        if (count == null) {
            count = 0;
        }
        return count;
    }

    @Override
    public int countByQuery(QueryBuilder queryBuilder) {
        String countByQuerySql = PgSqlUtils.countByQuerySql(this.entityDefinition, queryBuilder, this.fieldToColumnMap);
        List<Object> queryValues = PgSqlUtils.toQueryValues(queryBuilder);
        return this.multiJdbcTemplate.getRandomSlave().queryForObject(countByQuerySql, Integer.class, queryValues.toArray());
    }

    private JSONObject resultToEntityJson(ResultSet rs) throws SQLException {
        ResultSetMetaData resultSetMetaData = rs.getMetaData();
        int columnCount = resultSetMetaData.getColumnCount();
        JSONObject entityJson = new JSONObject();
        String columnName;
        String fieldName;
        for (int index = 1; index <= columnCount; index++) {
            columnName = resultSetMetaData.getColumnLabel(index);
            fieldName = this.columnToFieldMap.get(columnName);
            if (!StringUtils.hasText(fieldName)) {
                fieldName = EntityUtils.snakeToCamel(columnName);
            }
            entityJson.put(fieldName, rs.getObject(index));
        }
        for (ColumnDefinition columnDefinition : this.entityDefinition.columnDefinitions()) {
            if (columnDefinition.type() == ColumnType.JSON_ARRAY || columnDefinition.type() == ColumnType.JSON_OBJECT) {
                PgSqlUtils.jsonRow2FieldValue(entityJson, columnDefinition);
            }
        }
        return entityJson;
    }

    @Override
    public JSONObject queryById(Object id) {
        try {
            return this.multiJdbcTemplate.getRandomSlave().queryForObject(this.queryByIdSql, (rs, rowNum) -> this.resultToEntityJson(rs), id);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    @Override
    public List<JSONObject> query(QueryBuilder queryBuilder, SortBuilder sort, int from, int size) {
        String querySql = PgSqlUtils.querySql(this.entityDefinition, queryBuilder, sort, this.fieldToColumnMap);
        List<Object> queryValues = PgSqlUtils.toQueryValues(queryBuilder);
        List<Object> allValues = new ArrayList<>(queryValues);
        allValues.add(size);
        allValues.add(from);
        return this.multiJdbcTemplate.getRandomSlave().query(querySql, (rs, rowNum) -> this.resultToEntityJson(rs), allValues.toArray());
    }
}
