package io.github.siyukio.tools.entity.postgresql;

import io.github.siyukio.tools.entity.page.Page;
import io.github.siyukio.tools.entity.query.QueryBuilder;
import io.github.siyukio.tools.entity.sort.SortBuilder;

import java.util.Collection;
import java.util.List;

/**
 * Generic Data Access Object (DAO) interface for PostgreSQL-backed entities.
 *
 * <p>This interface defines common CRUD and query operations for entities of
 * type {@code T}. Implementations are responsible for translating the
 * {@link QueryBuilder} and {@link SortBuilder} into SQL, executing the
 * statements, and mapping result sets to entity instances.</p>
 *
 * @param <T> the entity type handled by this DAO
 * @author Bugee
 */
public interface PgEntityDao<T> {

    /**
     * Insert the given entity into the database.
     *
     * @param t the entity to insert
     * @return the inserted entity (may include generated fields such as an id)
     */
    T insert(T t);

    /**
     * Insert a batch of entities into the database.
     *
     * @param tList the collection of entities to insert
     * @return the number of rows successfully inserted
     */
    int insertBatch(Collection<T> tList);

    /**
     * Update the given entity in the database.
     *
     * @param t the entity containing updated values (must include identity)
     * @return the updated entity (may be the same instance or a refreshed copy)
     */
    T update(T t);

    /**
     * Update a batch of entities.
     *
     * @param tList the collection of entities to update
     * @return the number of rows successfully updated
     */
    int updateBatch(Collection<T> tList);

    /**
     * Insert the entity if it does not exist; otherwise update the existing
     * entity (upsert behavior).
     *
     * @param t the entity to upsert
     * @return the resulting entity after the upsert operation
     */
    T upsert(T t);

    /**
     * Delete a record by its primary key or identifier.
     *
     * @param id the primary key or unique identifier of the record to delete
     * @return the number of rows affected (0 if not found, 1 if deleted)
     */
    int deleteById(Object id);

    /**
     * Delete records that match the provided entity (usually by identity
     * or by comparing key fields).
     *
     * @param t an entity whose identity (or fields) determine which records to delete
     * @return the number of rows affected
     */
    int delete(T t);

    /**
     * Delete records that match the provided query.
     *
     * @param queryBuilder a query describing which records should be deleted
     * @return the number of rows affected
     */
    int deleteByQuery(QueryBuilder queryBuilder);

    /**
     * Check whether a record with the given id exists.
     *
     * @param id the primary key or identifier to check
     * @return {@code true} if a matching record exists, {@code false} otherwise
     */
    boolean existById(Object id);

    /**
     * Query a single entity by its primary key or identifier.
     *
     * @param id the primary key or unique identifier
     * @return the matched entity, or {@code null} if none found
     */
    T queryById(Object id);

    /**
     * Query a single entity that matches the provided query criteria.
     * If multiple records match, the implementation may return the first
     * match according to its ordering rules.
     *
     * @param queryBuilder the query to select the entity
     * @return the matched entity, or {@code null} if none found
     */
    T queryOne(QueryBuilder queryBuilder);

    /**
     * Query a list of entities that match the provided query criteria.
     *
     * @param queryBuilder query criteria
     * @return a list of matching entities (empty list if none)
     */
    List<T> queryList(QueryBuilder queryBuilder);

    /**
     * Query a list of entities that match the provided query criteria with sorting.
     *
     * @param queryBuilder query criteria
     * @param sort         sorting specification (maybe {@code null})
     * @return a list of matching entities (empty list if none)
     */
    List<T> queryList(QueryBuilder queryBuilder, SortBuilder sort);

    /**
     * Query a list of entities that match the provided criteria with sorting
     * and offset/limit pagination.
     *
     * @param queryBuilder query criteria
     * @param sort         sorting specification (maybe {@code null})
     * @param from         zero-based offset of the first result to return
     * @param size         maximum number of results to return
     * @return a list of matching entities (empty list if none)
     */
    List<T> queryList(QueryBuilder queryBuilder, SortBuilder sort, int from, int size);

    /**
     * Query a list of entities that match the provided criteria with
     * offset/limit pagination and no explicit sort.
     *
     * @param queryBuilder query criteria
     * @param from         zero-based offset of the first result to return
     * @param size         maximum number of results to return
     * @return a list of matching entities (empty list if none)
     */
    List<T> queryList(QueryBuilder queryBuilder, int from, int size);

    /**
     * Query all entities with sorting.
     *
     * @param sort sorting specification (may be {@code null})
     * @return a list of matching entities (empty list if none)
     */
    List<T> queryList(SortBuilder sort);

    /**
     * Query entities with sorting and pagination but without any filtering criteria.
     *
     * @param sort sorting specification (may be {@code null})
     * @param from zero-based offset of the first result to return
     * @param size maximum number of results to return
     * @return a list of entities from the requested page (empty list if none)
     */
    List<T> queryList(SortBuilder sort, int from, int size);

    /**
     * Query entities with pagination but without any filtering criteria.
     *
     * @param from zero-based offset of the first result to return
     * @param size maximum number of results to return
     * @return a list of entities from the requested page (empty list if none)
     */
    List<T> queryList(int from, int size);

    /**
     * Count all records in the underlying table for this entity type.
     *
     * @return the total number of records
     */
    int queryCount();

    /**
     * Count the number of records that match the provided query criteria.
     *
     * @param queryBuilder query criteria
     * @return the number of matching records
     */
    int queryCount(QueryBuilder queryBuilder);

    /**
     * Query a paged result set that contains both the list of entities and
     * paging metadata.
     *
     * @param queryBuilder query criteria (may be {@code null} for no filter)
     * @param sort         sorting specification (may be {@code null})
     * @param page         one-based page index (implementation may use zero-based internally)
     * @param size         number of items per page
     * @return a {@link Page} containing the requested page of entities and metadata
     */
    Page<T> queryPage(QueryBuilder queryBuilder, SortBuilder sort, int page, int size);
}
