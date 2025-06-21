package io.github.siyukio.tools.api.parameter.request;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.util.JsonUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * @author Buddy
 */
public final class ArrayRequestValidator extends AbstractRequestValidator {

    private final RequestValidator requestValidator;

    private int maxItems = 255;
    private int minItems = 0;

    public ArrayRequestValidator(String name, boolean required, RequestValidator requestValidator, int maxItems, int minItems, String parent, String error) {
        super(name, required, parent, error);
        this.requestValidator = requestValidator;
        if (minItems < 0) {
            minItems = 0;
        }
        if (maxItems <= 0) {
            maxItems = 255;
        }
        this.maxItems = Math.max(maxItems, minItems);
        this.minItems = Math.min(maxItems, minItems);
    }

    @Override
    public Object validate(Object value) {
        this.assertRequired(value);
        //
        if (JSONObject.NULL.equals(value)) {
            return value;
        }
        if (value instanceof String) {
            String str = value.toString();
            if (str.isEmpty()) {
                str = "[]";
            }
            try {
                value = JsonUtils.parseArray(str);
            } catch (RuntimeException ex) {
                throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_PARSE_ARRAY_FORMAT);
            }
        }
        //
        Object itemValue;
        JSONArray result = new JSONArray();
        if (value instanceof JSONArray valueArray) {
            //trim
            Object obj;
            for (int index = 0; index < valueArray.length(); index++) {
                obj = valueArray.opt(index);
                if (!JSONObject.NULL.equals(obj)) {
                    if (obj instanceof String text) {
                        valueArray.put(index, text.strip());
                    } else {
                        break;
                    }
                }
            }
            for (Object subValue : valueArray) {
                itemValue = this.requestValidator.validate(subValue);
                if (!JSONObject.NULL.equals(itemValue)) {
                    result.put(itemValue);
                }
            }
        } else if (value instanceof List) {
            List<Object> valueList = (List<Object>) value;
            //trim
            Object obj;
            for (int index = 0; index < valueList.size(); index++) {
                obj = valueList.get(index);
                if (!JSONObject.NULL.equals(obj)) {
                    if (obj instanceof String text) {
                        valueList.set(index, text.strip());
                    } else {
                        break;
                    }
                }
            }
            for (Object subValue : valueList) {
                itemValue = this.requestValidator.validate(subValue);
                if (!JSONObject.NULL.equals(itemValue)) {
                    result.put(itemValue);
                }
            }
        } else {

            throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_ARRAY_FORMAT);
        }
        //
        if (result.length() > this.maxItems || result.length() < this.minItems) {
            throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_ARRAY_LIMIT_FORMAT, this.minItems, this.maxItems);
        }
        return result;
    }

}
