package io.github.siyukio.tools.entity.query;

import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author Bugee
 */
@ToString
@Getter
public class TermsQueryBuilder implements QueryBuilder {

    private final String fieldName;

    private final LinkedHashSet<Object> valueSet;

    public TermsQueryBuilder(String fieldName) {
        this.fieldName = fieldName;
        this.valueSet = new LinkedHashSet<>();
    }

    public TermsQueryBuilder(String fieldName, Object... values) {
        this.fieldName = fieldName;
        this.valueSet = new LinkedHashSet<>(values.length);
        this.valueSet.addAll(Arrays.asList(values));
    }

    public TermsQueryBuilder add(Object value) {
        this.valueSet.add(value);
        return this;
    }

    public TermsQueryBuilder addAll(Collection<?> values) {
        this.valueSet.addAll(values);
        return this;
    }
}
