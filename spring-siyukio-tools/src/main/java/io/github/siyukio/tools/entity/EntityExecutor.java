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

    EntityDefinition getEntityDefinition();

    JSONObject insert(JSONObject entityJson);

    int insertBatch(List<JSONObject> entityJsons);

    JSONObject update(JSONObject entityJson);

    int updateBatch(List<JSONObject> entityJsons);

    int delete(Object id);

    void deleteBatch(List<Object> ids);

    int deleteByQuery(QueryBuilder queryBuilder);

    int count();

    int countByQuery(QueryBuilder queryBuilder);

    JSONObject queryById(Object id);

    List<JSONObject> query(QueryBuilder queryBuilder, SortBuilder sort, int from, int size);
}
