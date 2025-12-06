package io.github.siyukio.tools.api.parameter.request.basic;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.parameter.request.AbstractRequestValidator;

/**
 * @author Buddy
 */
public class NumberRequestValidator extends AbstractRequestValidator {

    private final Long maximum;

    private final Long minimum;

    public NumberRequestValidator(String name, Boolean required,
                                  Long maximum, Long minimum,
                                  String parent, String error) {
        super(name, required, parent, error);
        this.maximum = maximum;
        this.minimum = minimum;
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
                throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_NUMBER_FORMAT);
            }
        } else if (value instanceof Number) {
            num = ((Number) value).doubleValue();
        } else {
            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_NUMBER_FORMAT);
        }

        if (this.maximum != null && num > this.maximum) {
            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_NUMBER_MAX_FORMAT, this.maximum);
        }

        if (this.minimum != null && num < this.minimum) {
            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_NUMBER_MIN_FORMAT, this.minimum);
        }
        return num;
    }

}
