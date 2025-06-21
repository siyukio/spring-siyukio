package io.github.siyukio.tools.api.parameter.response;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.definition.ApiDefinition;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Buddy
 */
public interface ResponseFilter {

    private static ResponseFilter createObjectResponseFilter(String paramName, boolean dynamic, JSONArray responseParamArray) {
        Map<String, ResponseFilter> subResponseFilterMap = new HashMap<>();
        //
        ResponseFilter responseFilter;
        JSONObject responseParam;
        String type;
        String name;
        for (int index = 0; index < responseParamArray.length(); index++) {
            responseParam = responseParamArray.getJSONObject(index);
            type = responseParam.getString("type");
            switch (type) {
                case ApiConstants.TYPE_ARRAY:
                    responseFilter = createCollectionResponseFilter(responseParam);
                    break;
                case ApiConstants.TYPE_OBJECT:
                    name = responseParam.getString("name");
                    JSONArray childArray = responseParam.getJSONArray("childArray");
                    responseFilter = createObjectResponseFilter(name, responseParam.optBoolean("dynamic", false), childArray);
                    break;
                default:
                    name = responseParam.getString("name");
                    responseFilter = new BasicResponseFilter(name);
                    break;
            }
            subResponseFilterMap.put(responseFilter.getName(), responseFilter);
        }
        return new ObjectResponseFilter(paramName, dynamic, subResponseFilterMap);
    }

    private static ResponseFilter createCollectionResponseFilter(JSONObject responseParam) {
        ResponseFilter responseFilter;
        String name = responseParam.getString("name");
        JSONObject items = responseParam.getJSONObject("items");
        String itemType = items.getString("type");
        if (itemType.equals(ApiConstants.TYPE_OBJECT)) {
            JSONArray childArray = items.getJSONArray("childArray");
            responseFilter = createObjectResponseFilter(name, responseParam.optBoolean("dynamic", false), childArray);
        } else {
            responseFilter = new BasicResponseFilter(name + ".item");
        }
        return new ArrayResponseFilter(name, responseFilter);
    }

    public static ResponseFilter createResponseFilter(ApiDefinition apiDefinition) {
        JSONArray responseParameters = apiDefinition.responseParameters();
        boolean dynamic = apiDefinition.realReturnType().isAssignableFrom(JSONObject.class);
        return createObjectResponseFilter("ResponseBody", dynamic, responseParameters);
    }

    public String getName();

    public void filter(Object value);

}
