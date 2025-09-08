package io.github.siyukio.tools.entity.sort;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Bugee
 */
@ToString
@Getter
public class ListSortBuilder implements SortBuilder {

    private final List<FieldSortBuilder> sortBuilderList = new ArrayList<>();

    public ListSortBuilder(FieldSortBuilder... sortBuilders) {
        sortBuilderList.addAll(Arrays.asList(sortBuilders));
    }
}
