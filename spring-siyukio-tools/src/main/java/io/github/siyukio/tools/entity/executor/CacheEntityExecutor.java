package io.github.siyukio.tools.entity.executor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.siyukio.tools.entity.EntityExecutor;
import io.github.siyukio.tools.entity.definition.CacheDefinition;
import io.github.siyukio.tools.entity.definition.EntityDefinition;
import io.github.siyukio.tools.entity.query.QueryBuilder;
import io.github.siyukio.tools.entity.sort.SortBuilder;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Bugee
 */
@Slf4j
public class CacheEntityExecutor implements EntityExecutor {

    private final EntityExecutor delegate;
    private final Cache<String, JSONObject> cache;

    public CacheEntityExecutor(EntityExecutor delegate, CacheDefinition cacheDefinition) {
        this.delegate = delegate;
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        builder.maximumSize(cacheDefinition.maximumSize());
        if (cacheDefinition.softValues()) {
            builder.softValues();
        }
        TimeUnit expireUnit = cacheDefinition.expireUnit();
        if (cacheDefinition.expireAfterAccess() > 0) {
            builder.expireAfterAccess(cacheDefinition.expireAfterAccess(), expireUnit);
        }
        if (cacheDefinition.expireAfterWrite() > 0) {
            builder.expireAfterWrite(cacheDefinition.expireAfterWrite(), expireUnit);
        }
        this.cache = builder.build();
    }

    private String buildCacheKey(JSONObject entityJson) {
        EntityDefinition entityDefinition = this.delegate.getEntityDefinition();
        return entityJson.optString(entityDefinition.keyDefinition().fieldName());
    }

    @Override
    public String getMasterKey() {
        return this.delegate.getMasterKey();
    }

    @Override
    public EntityDefinition getEntityDefinition() {
        return this.delegate.getEntityDefinition();
    }

    @Override
    public JSONObject insert(JSONObject entityJson) {
        return this.delegate.insert(entityJson);
    }

    @Override
    public int insertBatch(List<JSONObject> entityJsons) {
        return this.delegate.insertBatch(entityJsons);
    }

    @Override
    public JSONObject update(JSONObject entityJson) {
        entityJson = this.delegate.update(entityJson);
        this.cache.invalidate(this.buildCacheKey(entityJson));
        return entityJson;
    }

    @Override
    public int updateBatch(List<JSONObject> entityJsons) {
        int num = this.delegate.updateBatch(entityJsons);
        for (JSONObject entityJson : entityJsons) {
            this.cache.invalidate(this.buildCacheKey(entityJson));
        }
        return num;
    }

    @Override
    public JSONObject upsert(JSONObject entityJson) {
        entityJson = this.delegate.upsert(entityJson);
        this.cache.invalidate(this.buildCacheKey(entityJson));
        return entityJson;
    }

    @Override
    public int delete(Object id) {
        int num = this.delegate.delete(id);
        this.cache.invalidate(String.valueOf(id));
        return num;
    }

    @Override
    public void deleteBatch(List<Object> ids) {
        this.delegate.deleteBatch(ids);
        for (Object id : ids) {
            this.cache.invalidate(String.valueOf(id));
        }
    }

    @Override
    public int deleteByQuery(QueryBuilder queryBuilder) {
        int num = this.delegate.deleteByQuery(queryBuilder);
        if (num > 0) {
            this.cache.invalidateAll();
        }
        return num;
    }

    @Override
    public int count() {
        return this.delegate.count();
    }

    @Override
    public int countByQuery(QueryBuilder queryBuilder) {
        return this.delegate.countByQuery(queryBuilder);
    }

    @Override
    public JSONObject queryById(Object id) {
        String cacheKey = String.valueOf(id);
        JSONObject cached = this.cache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Cache hit: {}", cacheKey);
            return cached;
        }
        JSONObject entityJson = this.delegate.queryById(id);
        if (entityJson != null) {
            this.cache.put(cacheKey, entityJson);
        }
        return entityJson;
    }

    @Override
    public List<JSONObject> query(QueryBuilder queryBuilder, SortBuilder sort, int from, int size) {
        return this.delegate.query(queryBuilder, sort, from, size);
    }
}
