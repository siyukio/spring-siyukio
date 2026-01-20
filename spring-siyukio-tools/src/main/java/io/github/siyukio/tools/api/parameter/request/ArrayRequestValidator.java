package io.github.siyukio.tools.api.parameter.request;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.util.XDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * @author Buddy
 */
@Slf4j
public final class ArrayRequestValidator extends AbstractRequestValidator {

    private final RequestValidator requestValidator;

    private final Integer maxItems;
    private final Integer minItems;

    public ArrayRequestValidator(String name, Boolean required,
                                 RequestValidator requestValidator,
                                 Integer maxItems, Integer minItems,
                                 String parent, String error) {
        super(name, required, parent, error);
        this.requestValidator = requestValidator;
        this.maxItems = maxItems;
        this.minItems = minItems;
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
                value = XDataUtils.parseArray(str);
            } catch (RuntimeException ex) {
                log.error("parseArray error: {}", ex.getMessage());
                throw this.createApiException(ApiConstants.ERROR_PARAMETER_PARSE_ARRAY_FORMAT);
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

            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_ARRAY_FORMAT);
        }
        //
        if (this.maxItems != null && result.length() > this.maxItems) {
            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_ARRAY_MAX_FORMAT, this.maxItems);
        }

        if (this.minItems != null && result.length() < this.minItems) {
            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_ARRAY_MIN_FORMAT, this.minItems);
        }
        return result;
    }

}
