package io.github.siyukio.tools.api.parameter.response;

import org.json.JSONArray;

import java.util.List;

/**
 * @author Buddy
 */
public final class ArrayResponseFilter implements ResponseFilter {

    private final String name;

    private final ResponseFilter responseFilter;

    public ArrayResponseFilter(String name, ResponseFilter responseFilter) {
        this.name = name;
        this.responseFilter = responseFilter;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void filter(Object value) {
        if (value != null) {
            if (value instanceof JSONArray valueArray) {
                for (Object subValue : valueArray) {
                    this.responseFilter.filter(subValue);
                }
            } else if (value instanceof List) {
                List<Object> valueList = (List<Object>) value;
                for (Object subValue : valueList) {
                    this.responseFilter.filter(subValue);
                }
            }
        }
    }
}
