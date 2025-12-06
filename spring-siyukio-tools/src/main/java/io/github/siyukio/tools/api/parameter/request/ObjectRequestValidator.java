package io.github.siyukio.tools.api.parameter.request;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.util.XDataUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Buddy
 */
public class ObjectRequestValidator extends AbstractRequestValidator {

    private final boolean additionalProperties;
    private final Map<String, RequestValidator> requestValidatorMap = new HashMap<>();

    public ObjectRequestValidator(String name, Boolean required,
                                  Map<String, RequestValidator> requestValidatorMap,
                                  Boolean additionalProperties, String parentPath, String error) {
        super(name, required, parentPath, error);
        this.additionalProperties = additionalProperties != null && additionalProperties;
        this.requestValidatorMap.putAll(requestValidatorMap);
    }

    @Override
    public Object validate(Object value) {
        this.assertRequired(value);
        //
        if (JSONObject.NULL.equals(value)) {
            return value;
        }
        //
        Object result;
        if (value instanceof String) {
            String str = value.toString();
            if (str.isEmpty()) {
                str = "{}";
            }
            try {
                value = XDataUtils.parseObject(str);
            } catch (RuntimeException ex) {
                throw this.createApiException(ApiConstants.ERROR_PARAMETER_PARSE_OBJECT_FORMAT);
            }
        }
        if (value instanceof JSONObject data) {
            if (this.additionalProperties) {
                return data;
            }

            Object childValue;
            RequestValidator requestValidator;
            Set<String> nameSet = new HashSet<>(data.keySet());

            for (String param : nameSet) {
                requestValidator = this.requestValidatorMap.get(param);
                if (requestValidator == null) {
                    data.remove(param);
                } else {
                    childValue = data.opt(param);
                    if (JSONObject.NULL.equals(childValue)) {
                        //Discard null values.
                        data.remove(param);
                    }
                }
            }

            for (Entry<String, RequestValidator> entry : this.requestValidatorMap.entrySet()) {
                childValue = data.opt(entry.getKey());
                if (childValue instanceof String str) {
                    childValue = str.strip();
                }
                childValue = entry.getValue().validate(childValue);
                if (!JSONObject.NULL.equals(childValue)) {
                    data.put(entry.getKey(), childValue);
                }
            }
            result = data;
        } else {
            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_OBJECT_FORMAT);
        }
        return result;
    }

}
