package io.github.siyukio.tools.api.parameter.request.basic;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.parameter.request.AbstractRequestValidator;

/**
 * @author Buddy
 */
public class StringRequestValidator extends AbstractRequestValidator {

    private int maxLength = 255;

    private int minLength = 0;

    public StringRequestValidator(String key, boolean required, int maxLength, int minLength, String parent, String message) {
        super(key, required, parent, message);
        if (minLength < 0) {
            minLength = 0;
        }
        if (maxLength <= 0) {
            maxLength = 255;
        }
        this.maxLength = Math.max(maxLength, minLength);
        this.minLength = Math.min(maxLength, minLength);
    }

    @Override
    public Object validate(Object value) {
        String v = value.toString();
        if (v.isEmpty()) {
            return null;
        }
        //
        if (v.length() > this.maxLength || v.length() < this.minLength) {
            throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_STRING_LIMIT_FORMAT, this.toNumLimitString(this.minLength), this.toNumLimitString(this.maxLength));
        }
        return v;
    }

}
