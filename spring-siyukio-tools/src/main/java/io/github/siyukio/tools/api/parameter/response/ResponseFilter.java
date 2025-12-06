package io.github.siyukio.tools.api.parameter.response;

import io.github.siyukio.tools.api.definition.ApiDefinition;
import io.github.siyukio.tools.api.definition.ApiResponseParameter;
import io.github.siyukio.tools.api.definition.ApiSchema;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Buddy
 */
public interface ResponseFilter {

    private static ResponseFilter createObjectResponseFilter(ApiResponseParameter apiResponseParameter) {
        Map<String, ResponseFilter> subResponseFilterMap = new HashMap<>();
        //
        if (!CollectionUtils.isEmpty(apiResponseParameter.properties())) {
            ResponseFilter responseFilter;
            for (ApiResponseParameter childApiResponseParameter : apiResponseParameter.properties()) {
                switch (childApiResponseParameter.schema().type()) {
                    case ARRAY:
                        responseFilter = createCollectionResponseFilter(childApiResponseParameter);
                        break;
                    case OBJECT:
                        responseFilter = createObjectResponseFilter(childApiResponseParameter);
                        break;
                    default:
                        responseFilter = new BasicResponseFilter(childApiResponseParameter.name());
                        break;
                }
                subResponseFilterMap.put(responseFilter.getName(), responseFilter);
            }
        }
        return new ObjectResponseFilter(apiResponseParameter.name(),
                apiResponseParameter.schema().additionalProperties(),
                subResponseFilterMap);
    }

    private static ResponseFilter createCollectionResponseFilter(ApiResponseParameter apiResponseParameter) {
        ResponseFilter responseFilter;
        ApiResponseParameter items = apiResponseParameter.items();
        assert items != null;
        ApiSchema.Type itemsType = items.schema().type();
        String itemsName = apiResponseParameter.name() + "." + items.name();
        if (itemsType == ApiSchema.Type.OBJECT) {
            responseFilter = createObjectResponseFilter(apiResponseParameter.items());
        } else {
            responseFilter = new BasicResponseFilter(itemsName);
        }
        return new ArrayResponseFilter(apiResponseParameter.name(), responseFilter);
    }

    static ResponseFilter createResponseFilter(ApiDefinition apiDefinition) {
        return createObjectResponseFilter(apiDefinition.responseBodyParameter());
    }

    String getName();

    void filter(Object value);

}
