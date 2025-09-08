package io.github.siyukio.tools.entity.query;

import lombok.Getter;
import lombok.ToString;

/**
 * @author Bugee
 */
@ToString
@Getter
public class MatchQueryBuilder implements QueryBuilder {
    
    private final String fieldName;

    private final String text;

    public MatchQueryBuilder(String fieldName, String text) {
        this.fieldName = fieldName;
        this.text = text;
    }

}
