package io.github.siyukio.tools.entity.executor;

import io.github.siyukio.tools.entity.EntityConstants;
import io.github.siyukio.tools.entity.EntityExecutor;
import io.github.siyukio.tools.entity.definition.ColumnDefinition;
import io.github.siyukio.tools.entity.definition.EntityDefinition;
import io.github.siyukio.tools.entity.query.QueryBuilder;
import io.github.siyukio.tools.entity.sort.SortBuilder;
import io.github.siyukio.tools.util.CryptoUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bugee
 */
@Slf4j
public class CryptoEntityExecutor implements EntityExecutor {

    private final EntityExecutor delegate;

    private final List<ColumnDefinition> encryptedColumns = new ArrayList<>();

    public CryptoEntityExecutor(EntityExecutor delegate) {
        this.delegate = delegate;
        String key = delegate.getMasterKey();
        Assert.hasText(key, EntityConstants.ERROR_ENCRYPTION_KEY_MISSING);
        for (ColumnDefinition columnDefinition : this.getEntityDefinition().columnDefinitions()) {
            if (columnDefinition.encrypted()) {
                this.encryptedColumns.add(columnDefinition);
            }
        }
    }

    private void encrypt(JSONObject entityJson) {
        String salt = entityJson.optString(EntityConstants.SALT_COLUMN, "");
        if (!StringUtils.hasText(salt)) {
            salt = CryptoUtils.randomSalt();
            entityJson.put(EntityConstants.SALT_COLUMN, salt);
        }
        String masterKey = this.delegate.getMasterKey();
        String keyInfo = delegate.getEntityDefinition().keyInfo();
        byte[] keyBytes = CryptoUtils.deriveKey(masterKey, salt, keyInfo);

        String encryptedText;
        String plainText;
        for (ColumnDefinition encryptedColumn : this.encryptedColumns) {
            plainText = entityJson.optString(encryptedColumn.fieldName(), "");
            encryptedText = CryptoUtils.encrypt(keyBytes, plainText);
            entityJson.put(encryptedColumn.fieldName(), encryptedText);
        }
    }

    private void decrypt(JSONObject entityJson) {
        String salt = entityJson.optString(EntityConstants.SALT_COLUMN, "");
        if (!StringUtils.hasText(salt)) {
            String id = entityJson.optString(this.getEntityDefinition().keyDefinition().fieldName(), "");
            log.warn("{}: {} salt is empty", this.getEntityDefinition().table(), id);
            return;
        }
        String masterKey = this.delegate.getMasterKey();
        String keyInfo = delegate.getEntityDefinition().keyInfo();
        byte[] keyBytes = CryptoUtils.deriveKey(masterKey, salt, keyInfo);

        String encryptedText;
        String plainText;
        for (ColumnDefinition encryptedColumn : this.encryptedColumns) {
            encryptedText = entityJson.optString(encryptedColumn.fieldName(), "");
            plainText = CryptoUtils.decrypt(keyBytes, encryptedText);
            entityJson.put(encryptedColumn.fieldName(), plainText);
        }
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
        this.encrypt(entityJson);
        entityJson = this.delegate.insert(entityJson);
        this.decrypt(entityJson);
        return entityJson;
    }

    @Override
    public int insertBatch(List<JSONObject> entityJsons) {
        for (JSONObject entityJson : entityJsons) {
            this.encrypt(entityJson);
        }
        return this.delegate.insertBatch(entityJsons);
    }

    @Override
    public JSONObject update(JSONObject entityJson) {
        this.encrypt(entityJson);
        entityJson = this.delegate.update(entityJson);
        this.decrypt(entityJson);
        return entityJson;
    }

    @Override
    public int updateBatch(List<JSONObject> entityJsons) {
        for (JSONObject entityJson : entityJsons) {
            this.encrypt(entityJson);
        }
        return this.delegate.updateBatch(entityJsons);
    }

    @Override
    public JSONObject upsert(JSONObject entityJson) {
        this.encrypt(entityJson);
        entityJson = this.delegate.upsert(entityJson);
        this.decrypt(entityJson);
        return entityJson;
    }

    @Override
    public int delete(Object id) {
        return this.delegate.delete(id);
    }

    @Override
    public void deleteBatch(List<Object> ids) {
        this.delegate.deleteBatch(ids);
    }

    @Override
    public int deleteByQuery(QueryBuilder queryBuilder) {
        return this.delegate.deleteByQuery(queryBuilder);
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
        JSONObject entityJson = this.delegate.queryById(id);
        this.decrypt(entityJson);
        return entityJson;
    }

    @Override
    public List<JSONObject> query(QueryBuilder queryBuilder, SortBuilder sort, int from, int size) {
        List<JSONObject> items = this.delegate.query(queryBuilder, sort, from, size);
        for (JSONObject item : items) {
            this.decrypt(item);
        }
        return items;
    }
}
