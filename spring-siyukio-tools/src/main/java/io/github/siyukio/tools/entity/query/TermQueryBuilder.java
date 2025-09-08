package io.github.siyukio.tools.entity.query;

import lombok.Getter;
import lombok.ToString;

/**
 * @author Bugee
 */
@ToString
@Getter
public class TermQueryBuilder implements QueryBuilder {

    private final String fieldName;

    private final Object value;

    public TermQueryBuilder(String fieldName, Object value) {
        this.fieldName = fieldName;
        this.value = value;
    }

}
