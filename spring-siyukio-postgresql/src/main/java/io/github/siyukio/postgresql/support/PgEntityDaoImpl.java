package io.github.siyukio.postgresql.support;

import io.github.siyukio.tools.entity.ColumnType;
import io.github.siyukio.tools.entity.EntityExecutor;
import io.github.siyukio.tools.entity.definition.ColumnDefinition;
import io.github.siyukio.tools.entity.definition.EntityDefinition;
import io.github.siyukio.tools.entity.definition.KeyDefinition;
import io.github.siyukio.tools.entity.page.Page;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import io.github.siyukio.tools.entity.query.QueryBuilder;
import io.github.siyukio.tools.entity.sort.SortBuilder;
import io.github.siyukio.tools.util.IdUtils;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * @author Bugee
 */
@Slf4j
public class PgEntityDaoImpl<T> implements PgEntityDao<T> {

    private final Class<T> entityClass;

    private final EntityExecutor entityExecutor;

    public PgEntityDaoImpl(Class<T> entityClass, EntityExecutor entityExecutor) {
        this.entityClass = entityClass;
        this.entityExecutor = entityExecutor;
    }

    @Override
    public String toString() {
        EntityDefinition entityDefinition = this.entityExecutor.getEntityDefinition();
        return entityDefinition.schema() + "." + entityDefinition.table();
    }

    private void preInsert(JSONObject entityJson) {
        // generate the primary key value
        KeyDefinition keyDefinition = this.entityExecutor.getEntityDefinition().keyDefinition();
        if (keyDefinition.generated() && keyDefinition.type().equals(ColumnType.TEXT)) {
            if (!entityJson.has(keyDefinition.fieldName())) {
                entityJson.put(keyDefinition.fieldName(), IdUtils.getUniqueId());
            }
        }
        // fill in the default value when column value is null
        Object columnValue;
        for (ColumnDefinition columnDefinition : this.entityExecutor.getEntityDefinition().columnDefinitions()) {
            columnValue = entityJson.opt(columnDefinition.fieldName());
            if (JSONObject.NULL.equals(columnValue)) {
                entityJson.put(columnDefinition.fieldName(), columnDefinition.defaultValue());
            }
        }
        //
        long createdAtTs = System.currentTimeMillis();
        String createdAtFormat = XDataUtils.formatMs(createdAtTs);
        entityJson.put("createdAtTs", createdAtTs);
        entityJson.put("createdAt", createdAtFormat);
        entityJson.put("updatedAtTs", createdAtTs);
        entityJson.put("updatedAt", createdAtFormat);
    }

    @Override
    public T insert(T t) {
        JSONObject entityJson = XDataUtils.copy(t, JSONObject.class);
        this.preInsert(entityJson);
        entityJson = this.entityExecutor.insert(entityJson);
        return XDataUtils.copy(entityJson, this.entityClass);
    }

    @Override
    public int insertBatch(Collection<T> tList) {
        if (CollectionUtils.isEmpty(tList)) {
            return 0;
        }
        List<JSONObject> entityJsonList = XDataUtils.copy(tList, List.class, JSONObject.class);
        for (JSONObject entityJson : entityJsonList) {
            this.preInsert(entityJson);
        }
        return this.entityExecutor.insertBatch(entityJsonList);
    }

    private void preUpdate(JSONObject entityJson) {
        long updatedAtTs = System.currentTimeMillis();
        String updatedAtFormat = XDataUtils.formatMs(updatedAtTs);
        entityJson.put("updatedAtTs", updatedAtTs);
        entityJson.put("updatedAt", updatedAtFormat);
    }

    @Override
    public T update(T t) {
        JSONObject entityJson = XDataUtils.copy(t, JSONObject.class);
        this.preUpdate(entityJson);
        entityJson = this.entityExecutor.update(entityJson);
        return XDataUtils.copy(entityJson, this.entityClass);
    }

    @Override
    public int updateBatch(Collection<T> tList) {
        if (CollectionUtils.isEmpty(tList)) {
            return 0;
        }
        List<JSONObject> entityJsonList = XDataUtils.copy(tList, List.class, JSONObject.class);
        for (JSONObject entityJson : entityJsonList) {
            this.preUpdate(entityJson);
        }
        return this.entityExecutor.updateBatch(entityJsonList);
    }

    private void preUpsert(JSONObject entityJson) {
        long createdAtTs = System.currentTimeMillis();
        String createdAtFormat = XDataUtils.formatMs(createdAtTs);
        entityJson.put("createdAtTs", createdAtTs);
        entityJson.put("createdAt", createdAtFormat);
        entityJson.put("updatedAtTs", createdAtTs);
        entityJson.put("updatedAt", createdAtFormat);
    }

    @Override
    public T upsert(T t) {
        JSONObject entityJson = XDataUtils.copy(t, JSONObject.class);
        this.preUpsert(entityJson);
        entityJson = this.entityExecutor.upsert(entityJson);
        return XDataUtils.copy(entityJson, this.entityClass);
    }

    @Override
    public int deleteById(Object id) {
        return this.entityExecutor.delete(id);
    }

    @Override
    public int delete(T t) {
        JSONObject entityJson = XDataUtils.copy(t, JSONObject.class);
        String key = this.entityExecutor.getEntityDefinition().keyDefinition().fieldName();
        Object id = entityJson.opt(key);
        return this.deleteById(id);
    }

    @Override
    public int deleteByQuery(QueryBuilder queryBuilder) {
        return this.entityExecutor.deleteByQuery(queryBuilder);
    }

    @Override
    public boolean existById(Object id) {
        JSONObject entityJson = this.entityExecutor.queryById(id);
        return entityJson != null;
    }

    @Override
    public T queryById(Object id) {
        JSONObject entityJson = this.entityExecutor.queryById(id);
        if (entityJson == null) {
            return null;
        }
        return XDataUtils.copy(entityJson, this.entityClass);
    }

    public T queryOne(QueryBuilder queryBuilder) {
        List<T> list = this.query(queryBuilder, 0, 1);
        if (list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    @Override
    public List<T> query(QueryBuilder queryBuilder, SortBuilder sort, int from, int size) {
        if (from < 0) {
            from = 0;
        }
        if (size <= 0) {
            size = 100;
        }
        List<JSONObject> entityJsonList = this.entityExecutor.query(queryBuilder, sort, from, size);
        return XDataUtils.copy(entityJsonList, List.class, this.entityClass);
    }

    @Override
    public List<T> query(QueryBuilder queryBuilder, int from, int size) {
        return this.query(queryBuilder, null, from, size);
    }

    @Override
    public List<T> query(int from, int size) {
        return this.query(null, null, from, size);
    }

    @Override
    public int count() {
        return this.entityExecutor.count();
    }

    @Override
    public int countByQuery(QueryBuilder queryBuilder) {
        return this.entityExecutor.countByQuery(queryBuilder);
    }

    @Override
    public Page<T> queryPage(QueryBuilder queryBuilder, SortBuilder sort, int page, int size) {
        int total = this.countByQuery(queryBuilder);
        int from = (page - 1) * size;
        List<T> items = this.query(queryBuilder, sort, from, size);
        return new Page<>(total, items);
    }
}
