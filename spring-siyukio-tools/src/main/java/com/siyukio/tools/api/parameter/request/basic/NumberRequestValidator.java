package com.siyukio.tools.api.parameter.request.basic;

import com.siyukio.tools.api.constants.ApiConstants;
import com.siyukio.tools.api.parameter.request.AbstractRequestValidator;

/**
 * @author Buddy
 */
public class NumberRequestValidator extends AbstractRequestValidator {

    private long maximum = -1;

    private long minimum = -1;

    private boolean numLimit = false;

    public NumberRequestValidator(String name, boolean required, long maximum, long minimum, String parent, String error) {
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
        double num;
        if (value instanceof String) {
            String v = value.toString();
            if (v.isEmpty()) {
                return null;
            }
            try {
                num = Double.parseDouble(v);
            } catch (NumberFormatException e) {
                throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_NUMBER_FORMAT);
            }
        } else if (value instanceof Number) {
            num = ((Number) value).doubleValue();
        } else {
            throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_NUMBER_FORMAT);
        }
        
        if (this.numLimit) {
            boolean isValid = true;
            if (this.maximum != -1 && num > this.maximum) {
                isValid = false;
            }
            if (this.minimum != -1 && num < this.minimum) {
                isValid = false;
            }
            if (!isValid) {
                throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_NUMBER_LIMIT_FORMAT, this.toNumLimitString(this.minimum), this.toNumLimitString(this.maximum));
            }
        }
        return num;
    }

}
