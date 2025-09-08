package io.github.siyukio.tools.entity.sort;

import lombok.Getter;
import lombok.ToString;

/**
 * @author Bugee
 */
@ToString
@Getter
public class FieldSortBuilder implements SortBuilder {

    private final String fieldName;

    private SortOrder order = SortOrder.ASC;

    public FieldSortBuilder(String fieldName) {
        this.fieldName = fieldName;
    }

    public FieldSortBuilder order(SortOrder order) {
        this.order = order;
        return this;
    }
}
