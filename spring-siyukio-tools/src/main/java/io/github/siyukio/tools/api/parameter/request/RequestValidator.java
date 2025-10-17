package io.github.siyukio.tools.api.parameter.request;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.definition.ApiDefinition;
import io.github.siyukio.tools.api.parameter.request.basic.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Validates whether the request parameters are valid.
 *
 * @author Buddy
 */

public interface RequestValidator {

    private static RequestValidator createObjectRequestValidator(String paramName, boolean required, JSONArray requestParamArray, boolean dynamic, String parent, String error) {
        Map<String, RequestValidator> subRequestValidatorMap = new HashMap<>();
        //
        RequestValidator requestValidator;
        JSONObject requestParam;
        String type;
        String name;
        String currentPath = StringUtils.hasText(parent) ? parent + "." + paramName : paramName;
        for (int index = 0; index < requestParamArray.length(); index++) {
            requestParam = requestParamArray.getJSONObject(index);
            type = requestParam.getString("type");
            switch (type) {
                case ApiConstants.TYPE_ARRAY:
                    requestValidator = createCollectionRequestValidator(requestParam, currentPath);
                    break;
                case ApiConstants.TYPE_OBJECT:
                    JSONArray childArray = requestParam.getJSONArray("childArray");
                    name = requestParam.getString("name");
                    requestValidator = createObjectRequestValidator(name, requestParam.optBoolean("required"), childArray,
                            requestParam.optBoolean("dynamic"), currentPath, requestParam.optString("error", ""));
                    break;
                default:
                    Object defaultValue = requestParam.opt("default");
                    RequestValidator typeRequestValidator = createTypeRequestValidator(requestParam, currentPath);
                    requestValidator = new BasicRequestValidator(typeRequestValidator, defaultValue, currentPath, requestParam.optString("error", ""));
                    break;
            }
            subRequestValidatorMap.put(requestValidator.getName(), requestValidator);
        }
        return new ObjectRequestValidator(paramName, required, subRequestValidatorMap, dynamic, parent, error);
    }

    private static RequestValidator createCollectionRequestValidator(JSONObject requestParam, String parent) {
        RequestValidator requestValidator;
        String name = requestParam.getString("name");
        JSONObject items = requestParam.getJSONObject("items");
        String itemType = items.getString("type");
        boolean dynamic = items.optBoolean("dynamic", false);
        int maxItems = requestParam.optInt("maxItems");
        int minItems = requestParam.optInt("minItems");
        String error = requestParam.optString("error", "");
        if (itemType.equals(ApiConstants.TYPE_OBJECT)) {
            JSONArray childArray = items.getJSONArray("childArray");
            requestValidator = createObjectRequestValidator(name, false, childArray, dynamic, parent, error);
        } else {
            RequestValidator typeRequestValidator = createArrayItemRequestValidator(name, items, requestParam, parent);
            requestValidator = new BasicRequestValidator(typeRequestValidator, null, parent, error);
        }
        return new ArrayRequestValidator(name, requestParam.optBoolean("required"), requestValidator, maxItems, minItems, parent, error);
    }

    private static RequestValidator createArrayItemRequestValidator(String name, JSONObject items, JSONObject requestParam, String parent) {
        String itemName = name + ".item";
        boolean required = false;
        String itemType = items.getString("type");
        long maximum = items.optLong("maximum", -1);
        long minimum = items.optLong("minimum", -1);
        int maxLength = items.optInt("maxLength", -1);
        int minLength = items.optInt("minLength", -1);
        String pattern = items.optString("pattern", "");
        String error = requestParam.optString("error", "");
        RequestValidator requestValidator;
        switch (itemType) {
            case ApiConstants.TYPE_BOOLEAN:
                requestValidator = new BooleanRequestValidator(itemName, required, parent, error);
                break;
            case ApiConstants.TYPE_INTEGER:
                requestValidator = new IntegerRequestValidator(itemName, required, maximum, minimum, parent, error);
                break;
            case ApiConstants.TYPE_NUMBER:
                requestValidator = new NumberRequestValidator(itemName, required, maximum, minimum, parent, error);
                break;
            case ApiConstants.TYPE_DATE:
                requestValidator = new DateRequestValidator(itemName, required, parent, error);
                break;
            default:
                if (pattern.isEmpty()) {
                    requestValidator = new StringRequestValidator(itemName, required, maxLength, minLength, parent, error);
                } else {
                    requestValidator = new PatternRequestValidator(itemName, required, pattern, parent, error);
                }
                break;
        }
        return requestValidator;
    }

    private static RequestValidator createTypeRequestValidator(JSONObject requestParam, String parent) {
        String type = requestParam.optString("type");
        String name = requestParam.optString("name");
        boolean required = requestParam.optBoolean("required");
        long max = requestParam.optLong("maximum", -1);
        long min = requestParam.optLong("minimum", -1);
        int maxLength = requestParam.optInt("maxLength", -1);
        int minLength = requestParam.optInt("minLength", -1);
        String pattern = requestParam.optString("pattern", "");
        String error = requestParam.optString("error", "");
        RequestValidator requestValidator;
        switch (type) {
            case ApiConstants.TYPE_BOOLEAN:
                requestValidator = new BooleanRequestValidator(name, required, parent, error);
                break;
            case ApiConstants.TYPE_INTEGER:
                requestValidator = new IntegerRequestValidator(name, required, max, min, parent, error);
                break;
            case ApiConstants.TYPE_NUMBER:
                requestValidator = new NumberRequestValidator(name, required, max, min, parent, error);
                break;
            case ApiConstants.TYPE_DATE:
                requestValidator = new DateRequestValidator(name, required, parent, error);
                break;
            default:
                if (pattern.isEmpty()) {
                    requestValidator = new StringRequestValidator(name, required, maxLength, minLength, parent, error);
                } else {
                    requestValidator = new PatternRequestValidator(name, required, pattern, parent, error);
                }
                break;
        }
        return requestValidator;
    }

    public static RequestValidator createRequestValidator(ApiDefinition apiDefinition) {
        JSONArray requestParameters = apiDefinition.requestParameters();
        return createObjectRequestValidator("", true, requestParameters, false, "", "");
    }

    public String getName();

    public boolean isRequired();

    public <T> T validate(T value);

}
