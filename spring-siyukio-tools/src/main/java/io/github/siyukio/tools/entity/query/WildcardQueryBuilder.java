package io.github.siyukio.tools.entity.query;

import lombok.Getter;
import lombok.ToString;

/**
 * @author Bugee
 */
@ToString
@Getter
public class WildcardQueryBuilder implements QueryBuilder {

    private final String fieldName;

    private final String text;

    private final boolean prefix;

    private final boolean suffix;

    public WildcardQueryBuilder(String fieldName, String text) {
        this.fieldName = fieldName;
        this.text = text;
        this.prefix = true;
        this.suffix = true;
    }

    public WildcardQueryBuilder(String fieldName, String text, boolean prefix, boolean suffix) {
        this.fieldName = fieldName;
        this.text = text;
        this.prefix = prefix;
        this.suffix = suffix;
    }
}
