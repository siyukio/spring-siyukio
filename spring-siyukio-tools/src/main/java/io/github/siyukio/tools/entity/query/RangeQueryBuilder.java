package io.github.siyukio.tools.entity.query;

import lombok.Getter;
import lombok.ToString;

/**
 * @author Bugee
 */
@ToString
@Getter
public class RangeQueryBuilder implements QueryBuilder {

    private final String fieldName;

    private Object minValue = null;

    private Object maxValue = null;

    private boolean gte = false;

    private boolean gt = false;

    private boolean lte = false;

    private boolean lt = false;

    public RangeQueryBuilder(String fieldName) {
        this.fieldName = fieldName;
    }

    public RangeQueryBuilder gt(Object from) {
        this.gt = true;
        this.gte = false;
        this.minValue = from;
        return this;
    }

    public RangeQueryBuilder gte(Object from) {
        this.gt = false;
        this.gte = true;
        this.minValue = from;
        return this;
    }

    public RangeQueryBuilder lt(Object to) {
        this.lt = true;
        this.lte = false;
        this.maxValue = to;
        return this;
    }

    public RangeQueryBuilder lte(Object to) {
        this.lt = false;
        this.lte = true;
        this.maxValue = to;
        return this;
    }
}
