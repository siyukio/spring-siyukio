package io.github.siyukio.tools.api.parameter.request.basic;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.parameter.request.AbstractRequestValidator;

/**
 * @author Buddy
 */
public class StringRequestValidator extends AbstractRequestValidator {

    private final Integer maxLength;

    private final Integer minLength;

    public StringRequestValidator(String name, Boolean required,
                                  Integer maxLength, Integer minLength,
                                  String parent, String error) {
        super(name, required, parent, error);
        this.maxLength = maxLength;
        this.minLength = minLength;
    }

    @Override
    public Object validate(Object value) {
        String v = value.toString();
        if (v.isEmpty()) {
            return null;
        }

        if (this.maxLength != null && v.length() > this.maxLength) {
            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_STRING_MAX_FORMAT, this.maxLength);
        }

        if (this.minLength != null && v.length() < this.minLength) {
            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_STRING_MIN_FORMAT, this.minLength);
        }
        return v;
    }

}
