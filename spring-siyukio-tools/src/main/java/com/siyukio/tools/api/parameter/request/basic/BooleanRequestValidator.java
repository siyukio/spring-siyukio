package com.siyukio.tools.api.parameter.request.basic;

import com.siyukio.tools.api.constants.ApiConstants;
import com.siyukio.tools.api.parameter.request.AbstractRequestValidator;

/**
 * @author Buddy
 */
public class BooleanRequestValidator extends AbstractRequestValidator {

    public BooleanRequestValidator(String name, boolean required, String parent, String error) {
        super(name, required, parent, error);
    }

    @Override
    public Object validate(Object value) {
        String v = value.toString().toLowerCase();
        return switch (v) {
            case "true", "1" -> true;
            case "false", "", "0" -> false;
            default -> throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_BOOLEAN_FORMAT);
        };
    }

}
