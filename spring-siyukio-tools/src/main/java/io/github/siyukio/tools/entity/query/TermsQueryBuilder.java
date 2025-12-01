package io.github.siyukio.tools.entity.query;

import lombok.Getter;
import lombok.ToString;

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
        for (Object value : values) {
            if (value instanceof Enum<?> enumValue) {
                value = enumValue.name();
            }
            this.valueSet.add(value);
        }
    }

    public TermsQueryBuilder add(Object value) {
        if (value instanceof Enum<?> enumValue) {
            value = enumValue.name();
        }
        this.valueSet.add(value);
        return this;
    }

    public TermsQueryBuilder addAll(Collection<?> values) {
        for (Object value : values) {
            if (value instanceof Enum<?> enumValue) {
                value = enumValue.name();
            }
            this.valueSet.add(value);
        }
        return this;
    }
}
