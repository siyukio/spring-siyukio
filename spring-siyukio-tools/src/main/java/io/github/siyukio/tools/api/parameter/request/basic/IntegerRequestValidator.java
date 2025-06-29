package io.github.siyukio.tools.api.parameter.request.basic;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.parameter.request.AbstractRequestValidator;

import java.math.BigInteger;

/**
 * @author Buddy
 */
public class IntegerRequestValidator extends AbstractRequestValidator {

    private long maximum = -1;

    private long minimum = -1;

    private boolean numLimit = false;

    public IntegerRequestValidator(String name, boolean required, long maximum, long minimum, String parent, String error) {
        super(name, required, parent, error);
        this.maximum = maximum;
        this.minimum = minimum;
        if (minimum != -1) {
            this.numLimit = true;
        }
        if (maximum != -1) {
            this.numLimit = true;
        }
        if (minimum != -1 && maximum != -1) {
            this.maximum = Math.max(maximum, minimum);
            this.minimum = Math.min(maximum, minimum);
        }
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
                throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_INTEGER_FORMAT);
            }
        } else if (value instanceof Number) {
            if (value instanceof BigInteger) {
                throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_INTEGER_LIMIT_FORMAT, this.toNumLimitString(this.minimum), Integer.MAX_VALUE);
            } else if (value instanceof Double) {
                throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_INTEGER_FORMAT);
            } else {
                num = ((Number) value).longValue();
            }
        } else {
            throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_INTEGER_FORMAT);
        }

        if (this.numLimit) {
            boolean isValid = this.maximum == -1 || num <= this.maximum;
            if (isValid) {
                if (this.minimum != -1 && num < this.minimum) {
                    isValid = false;
                }
            }
            if (!isValid) {
                throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_INTEGER_LIMIT_FORMAT, this.toNumLimitString(this.minimum), this.toNumLimitString(this.maximum));
            }
        }
        //
        return num;
    }

}
