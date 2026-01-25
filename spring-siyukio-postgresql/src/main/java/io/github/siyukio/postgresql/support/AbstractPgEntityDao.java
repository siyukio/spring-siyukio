package io.github.siyukio.postgresql.support;

import io.github.siyukio.tools.entity.ColumnType;
import io.github.siyukio.tools.entity.EntityConstants;
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
import org.json.JSONObject;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 *
 * @author Bugee
 */
public abstract class AbstractPgEntityDao<T> implements PgEntityDao<T> {

    protected final Class<T> entityClass;

    protected final EntityExecutor entityExecutor;

    protected AbstractPgEntityDao(Class<T> entityClass, EntityExecutor entityExecutor) {
        this.entityClass = entityClass;
        this.entityExecutor = entityExecutor;
    }

    protected final void preInsert(KeyDefinition keyDefinition, List<ColumnDefinition> columnDefinitions, JSONObject entityJson) {
        // generate the primary key value
        if (keyDefinition.generated() && keyDefinition.type().equals(ColumnType.TEXT)) {
            if (!entityJson.has(keyDefinition.fieldName())) {
                entityJson.put(keyDefinition.fieldName(), IdUtils.getUniqueId());
            }
        }
        // fill in the default value when column value is null
        Object columnValue;
        for (ColumnDefinition columnDefinition : columnDefinitions) {
            columnValue = entityJson.opt(columnDefinition.fieldName());
            if (JSONObject.NULL.equals(columnValue)) {
                entityJson.put(columnDefinition.fieldName(), columnDefinition.defaultValue());
            }
        }
        //
        long createdAtTs = System.currentTimeMillis();
        String createdAtFormat = XDataUtils.formatMs(createdAtTs);
        entityJson.put(EntityConstants.CREATED_AT_TS_FIELD, createdAtTs);
        entityJson.put(EntityConstants.CREATED_AT_FIELD, createdAtFormat);
        entityJson.put(EntityConstants.UPDATED_AT_TS_FIELD, createdAtTs);
        entityJson.put(EntityConstants.UPDATED_AT_FIELD, createdAtFormat);
    }

    protected final void preUpdate(JSONObject entityJson) {
        long updatedAtTs = System.currentTimeMillis();
        String updatedAtFormat = XDataUtils.formatMs(updatedAtTs);
        entityJson.put(EntityConstants.UPDATED_AT_TS_FIELD, updatedAtTs);
        entityJson.put(EntityConstants.UPDATED_AT_FIELD, updatedAtFormat);
    }

    protected final void preUpsert(JSONObject entityJson) {
        long createdAtTs = System.currentTimeMillis();
        String createdAtFormat = XDataUtils.formatMs(createdAtTs);
        entityJson.put(EntityConstants.CREATED_AT_TS_FIELD, createdAtTs);
        entityJson.put(EntityConstants.CREATED_AT_FIELD, createdAtFormat);
        entityJson.put(EntityConstants.UPDATED_AT_TS_FIELD, createdAtTs);
        entityJson.put(EntityConstants.UPDATED_AT_FIELD, createdAtFormat);
    }

    @Override
    public final String toString() {
        EntityDefinition entityDefinition = this.entityExecutor.getEntityDefinition();
        return entityDefinition.schema() + "." + entityDefinition.table();
    }

    @Override
    public final T insert(T t) {
        JSONObject entityJson = XDataUtils.copy(t, JSONObject.class);
        EntityDefinition entityDefinition = this.entityExecutor.getEntityDefinition();
        this.preInsert(entityDefinition.keyDefinition(), entityDefinition.columnDefinitions(), entityJson);
        entityJson = this.entityExecutor.insert(entityJson);
        return XDataUtils.copy(entityJson, this.entityClass);
    }

    @Override
    public final int insertBatch(Collection<T> tList) {
        if (CollectionUtils.isEmpty(tList)) {
            return 0;
        }
        EntityDefinition entityDefinition = this.entityExecutor.getEntityDefinition();
        List<JSONObject> entityJsonList = XDataUtils.copy(tList, List.class, JSONObject.class);
        for (JSONObject entityJson : entityJsonList) {
            this.preInsert(entityDefinition.keyDefinition(), entityDefinition.columnDefinitions(), entityJson);
        }
        return this.entityExecutor.insertBatch(entityJsonList);
    }

    @Override
    public final T update(T t) {
        JSONObject entityJson = XDataUtils.copy(t, JSONObject.class);
        this.preUpdate(entityJson);
        entityJson = this.entityExecutor.update(entityJson);
        return XDataUtils.copy(entityJson, this.entityClass);
    }

    @Override
    public final int updateBatch(Collection<T> tList) {
        if (CollectionUtils.isEmpty(tList)) {
            return 0;
        }
        List<JSONObject> entityJsonList = XDataUtils.copy(tList, List.class, JSONObject.class);
        for (JSONObject entityJson : entityJsonList) {
            this.preUpdate(entityJson);
        }
        return this.entityExecutor.updateBatch(entityJsonList);
    }

    @Override
    public final int deleteById(Object id) {
        return this.entityExecutor.delete(id);
    }

    @Override
    public final int delete(T t) {
        JSONObject entityJson = XDataUtils.copy(t, JSONObject.class);
        String key = this.entityExecutor.getEntityDefinition().keyDefinition().fieldName();
        Object id = entityJson.opt(key);
        return this.deleteById(id);
    }

    @Override
    public final int deleteByQuery(QueryBuilder queryBuilder) {
        return this.entityExecutor.deleteByQuery(queryBuilder);
    }

    @Override
    public final boolean existById(Object id) {
        JSONObject entityJson = this.entityExecutor.queryById(id);
        return entityJson != null;
    }

    @Override
    public final T queryById(Object id) {
        JSONObject entityJson = this.entityExecutor.queryById(id);
        if (entityJson == null) {
            return null;
        }
        return XDataUtils.copy(entityJson, this.entityClass);
    }

    @Override
    public final int queryCount() {
        return this.entityExecutor.count();
    }

    @Override
    public final int queryCount(QueryBuilder queryBuilder) {
        return this.entityExecutor.countByQuery(queryBuilder);
    }

    public final T queryOne(QueryBuilder queryBuilder) {
        List<T> list = this.queryList(queryBuilder, 0, 1);
        if (list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    @Override
    public final List<T> queryList(QueryBuilder queryBuilder) {
        return this.queryList(queryBuilder, null, 0, 100);
    }

    @Override
    public final List<T> queryList(QueryBuilder queryBuilder, SortBuilder sort) {
        return this.queryList(queryBuilder, sort, 0, 100);
    }

    @Override
    public final List<T> queryList(QueryBuilder queryBuilder, int from, int size) {
        return this.queryList(queryBuilder, null, from, size);
    }

    @Override
    public final List<T> queryList(SortBuilder sort) {
        return this.queryList(null, sort);
    }

    @Override
    public final List<T> queryList(SortBuilder sort, int from, int size) {
        return this.queryList(null, sort, from, size);
    }

    @Override
    public final List<T> queryList(int from, int size) {
        return this.queryList(null, null, from, size);
    }

    public abstract List<T> queryList(QueryBuilder queryBuilder, SortBuilder sort, int from, int size);

    public abstract Page<T> queryPage(QueryBuilder queryBuilder, SortBuilder sort, int page, int size);
}
