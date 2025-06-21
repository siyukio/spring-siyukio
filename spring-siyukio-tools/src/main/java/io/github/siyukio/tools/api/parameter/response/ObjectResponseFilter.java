package io.github.siyukio.tools.api.parameter.response;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Buddy
 */
public class ObjectResponseFilter implements ResponseFilter {

    private final String name;
    private final boolean dynamic;
    private final Map<String, ResponseFilter> responseFilterMap = new HashMap<>();

    public ObjectResponseFilter(String name, boolean dynamic, Map<String, ResponseFilter> responseFilterMap) {
        this.name = name;
        this.dynamic = dynamic;
        this.responseFilterMap.putAll(responseFilterMap);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void filter(Object value) {
        if (value != null) {
            if (value instanceof JSONObject data) {
                ResponseFilter responseFilter;
                Set<String> keySet = new HashSet<>(data.keySet());
                Object childValue;
                for (String param : keySet) {
                    responseFilter = this.responseFilterMap.get(param);
                    if (responseFilter == null) {
                        if (!this.dynamic) {
                            data.remove(param);
                        }
                    } else {
                        childValue = data.get(param);
                        responseFilter.filter(childValue);
                    }
                }
            }
        }
    }
}
