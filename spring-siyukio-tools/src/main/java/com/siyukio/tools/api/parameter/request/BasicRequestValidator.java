package com.siyukio.tools.api.parameter.request;

import com.siyukio.tools.util.JsonUtils;
import org.json.JSONObject;

/**
 * @author Buddy
 */
public final class BasicRequestValidator extends AbstractRequestValidator {

    private final Object defaultValue;

    private final RequestValidator typeRequestValidator;

    public BasicRequestValidator(RequestValidator typeRequestValidator, Object defaultValue, String parent, String error) {
        super(typeRequestValidator.getName(), typeRequestValidator.isRequired(), parent, error);
        this.defaultValue = defaultValue;
        this.typeRequestValidator = typeRequestValidator;
    }

    @Override
    public Object validate(Object value) {
        this.assertRequired(value);
        //
        if (JSONObject.NULL.equals(value) && !this.required) {
            return this.defaultValue;
        }
        Object result = value;
        if (value.toString().isEmpty() && !this.required && this.defaultValue != null) {
            return this.defaultValue;
        }
        if (value instanceof JSONObject) {
            result = JsonUtils.toJSONString(value);
        }
        //
        return this.typeRequestValidator.validate(result);
    }

}
