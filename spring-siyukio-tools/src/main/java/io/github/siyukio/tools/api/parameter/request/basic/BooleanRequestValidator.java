package io.github.siyukio.tools.api.parameter.request.basic;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.parameter.request.AbstractRequestValidator;

/**
 * @author Buddy
 */
public class BooleanRequestValidator extends AbstractRequestValidator {

    public BooleanRequestValidator(String name, Boolean required,
                                   String parent, String error) {
        super(name, required, parent, error);
    }

    @Override
    public Object validate(Object value) {
        String v = value.toString().toLowerCase();
        return switch (v) {
            case "true", "1" -> true;
            case "false", "", "0" -> false;
            default -> throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_BOOLEAN_FORMAT);
        };
    }

}
