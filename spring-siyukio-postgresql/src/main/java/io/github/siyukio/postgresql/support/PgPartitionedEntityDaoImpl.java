package io.github.siyukio.postgresql.support;

import io.github.siyukio.tools.entity.EntityConstants;
import io.github.siyukio.tools.entity.EntityExecutor;
import io.github.siyukio.tools.entity.page.Page;
import io.github.siyukio.tools.entity.postgresql.PgEntityDao;
import io.github.siyukio.tools.entity.query.QueryBuilder;
import io.github.siyukio.tools.entity.sort.SortBuilder;
import io.github.siyukio.tools.entity.sort.SortBuilders;
import io.github.siyukio.tools.entity.sort.SortOrder;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.util.List;

/**
 * @author Bugee
 */
@Slf4j
public class PgPartitionedEntityDaoImpl<T> extends AbstractPgEntityDao<T> implements PgEntityDao<T> {

    public PgPartitionedEntityDaoImpl(Class<T> entityClass, EntityExecutor entityExecutor) {
        super(entityClass, entityExecutor);
    }

    @Override
    public T upsert(T t) {
        throw new UnsupportedOperationException("upsert not supported");
    }

    @Override
    public List<T> queryList(QueryBuilder queryBuilder, SortBuilder sort, int from, int size) {
        if (from < 0) {
            from = 0;
        }
        if (size <= 0) {
            size = 100;
        }
        if (sort == null) {
            sort = SortBuilders.fieldSort(EntityConstants.CREATED_AT_TS_FIELD).order(SortOrder.ASC);
        }
        List<JSONObject> entityJsonList = this.entityExecutor.query(queryBuilder, sort, from, size);
        return XDataUtils.copy(entityJsonList, List.class, this.entityClass);
    }

    @Override
    public Page<T> queryPage(QueryBuilder queryBuilder, SortBuilder sort, int page, int size) {
        int total = this.queryCount(queryBuilder);
        int from = (page - 1) * size;
        if (sort == null) {
            sort = SortBuilders.fieldSort(EntityConstants.CREATED_AT_TS_FIELD).order(SortOrder.ASC);
        }
        List<T> items = this.queryList(queryBuilder, sort, from, size);
        return new Page<>(total, items);
    }
}
