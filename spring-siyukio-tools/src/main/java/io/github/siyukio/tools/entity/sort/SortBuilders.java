package io.github.siyukio.tools.entity.sort;

/**
 * @author Bugee
 */
public class SortBuilders {

    public static FieldSortBuilder fieldSort(String field) {
        return new FieldSortBuilder(field);
    }

    public static ListSortBuilder fieldSort(FieldSortBuilder... sortBuilders) {
        return new ListSortBuilder(sortBuilders);
    }
}
