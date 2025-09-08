package io.github.siyukio.tools.entity.postgresql;

import io.github.siyukio.tools.entity.page.Page;
import io.github.siyukio.tools.entity.query.QueryBuilder;
import io.github.siyukio.tools.entity.sort.SortBuilder;

import java.util.Collection;
import java.util.List;

/**
 * @author Bugee
 */
public interface PgEntityDao<T> {

    T insert(T t);

    int insertBatch(Collection<T> tList);

    T update(T t);

    int updateBatch(Collection<T> tList);

    int deleteById(Object id);

    int delete(T t);

    int deleteByQuery(QueryBuilder queryBuilder);

    T queryById(Object id);

    List<T> query(QueryBuilder queryBuilder, SortBuilder sort, int from, int size);

    List<T> query(QueryBuilder queryBuilder, int from, int size);

    List<T> query(int from, int size);

    int count();

    int countByQuery(QueryBuilder queryBuilder);

    Page<T> queryPage(QueryBuilder queryBuilder, SortBuilder sort, int page, int pageSize);
}
