package io.github.siyukio.tools.entity.query;

import io.github.siyukio.tools.util.XDataUtils;
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
        if (value instanceof Enum<?> enumValue) {
            value = XDataUtils.getEnumJsonValue(enumValue);
        }
        this.value = value;
    }

}
