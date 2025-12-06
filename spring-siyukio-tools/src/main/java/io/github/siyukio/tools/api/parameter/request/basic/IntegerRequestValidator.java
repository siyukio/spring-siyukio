package io.github.siyukio.tools.api.parameter.request.basic;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.parameter.request.AbstractRequestValidator;

import java.math.BigInteger;

/**
 * @author Buddy
 */
public class IntegerRequestValidator extends AbstractRequestValidator {

    private final Long maximum;

    private final Long minimum;

    public IntegerRequestValidator(String name, Boolean required,
                                   Long maximum, Long minimum,
                                   String parent, String error) {
        super(name, required, parent, error);
        this.maximum = maximum;
        this.minimum = minimum;
    }

    @Override
    public Object validate(Object value) {
        long num;
        if (value instanceof String) {
            String v = value.toString();
            if (v.isEmpty()) {
                return null;
            }
            try {
                num = Long.parseLong(v);
            } catch (NumberFormatException e) {
                throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_INTEGER_FORMAT);
            }
        } else if (value instanceof Number) {
            if (value instanceof BigInteger) {
                throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_INTEGER_MAX_FORMAT, Long.MAX_VALUE);
            } else if (value instanceof Double) {
                throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_INTEGER_FORMAT);
            } else {
                num = ((Number) value).longValue();
            }
        } else {
            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_INTEGER_FORMAT);
        }

        if (this.maximum != null && num > this.maximum) {
            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_INTEGER_MAX_FORMAT, this.maximum);
        }

        if (this.minimum != null && num < this.minimum) {
            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_INTEGER_MIN_FORMAT, this.minimum);
        }

        return num;
    }

}
