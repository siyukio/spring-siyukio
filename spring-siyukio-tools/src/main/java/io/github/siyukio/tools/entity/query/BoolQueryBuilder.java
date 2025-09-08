package io.github.siyukio.tools.entity.query;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Bugee
 */
@ToString
@Getter
public class BoolQueryBuilder implements QueryBuilder {
    
    private final List<QueryBuilder> mustClauses = new ArrayList<>();

    private final List<QueryBuilder> shouldClauses = new ArrayList<>();

    private final List<QueryBuilder> mustNotClauses = new ArrayList<>();

    public BoolQueryBuilder must(QueryBuilder queryBuilder) {
        mustClauses.add(queryBuilder);
        return this;
    }

    public BoolQueryBuilder should(QueryBuilder queryBuilder) {
        shouldClauses.add(queryBuilder);
        return this;
    }

    public BoolQueryBuilder mustNot(QueryBuilder queryBuilder) {
        mustNotClauses.add(queryBuilder);
        return this;
    }

}
