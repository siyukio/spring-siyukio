package io.github.siyukio.tools.entity;

import io.github.siyukio.tools.entity.definition.EntityDefinition;
import io.github.siyukio.tools.entity.query.QueryBuilder;
import io.github.siyukio.tools.entity.sort.SortBuilder;
import org.json.JSONObject;

import java.util.List;

/**
 * @author Bugee
 */
public interface EntityExecutor {

    String getMasterKey();

    EntityDefinition getEntityDefinition();

    JSONObject insert(JSONObject entityJson);

    int insertBatch(List<JSONObject> entityJsons);

    JSONObject update(JSONObject entityJson);

    int updateBatch(List<JSONObject> entityJsons);

    int update(String sql, List<Object> values);

    JSONObject upsert(JSONObject entityJson);

    int delete(Object id);

    void deleteBatch(List<Object> ids);

    int deleteByQuery(QueryBuilder queryBuilder);

    int count();

    int countByQuery(QueryBuilder queryBuilder);

    JSONObject queryById(Object id);

    List<JSONObject> query(QueryBuilder queryBuilder, SortBuilder sort, int from, int size);

    <E> List<E> queryForList(String querySql, Class<E> elementType, Object... args);

    <E> E queryForObject(String querySql, Class<E> elementType, Object... args);
}
