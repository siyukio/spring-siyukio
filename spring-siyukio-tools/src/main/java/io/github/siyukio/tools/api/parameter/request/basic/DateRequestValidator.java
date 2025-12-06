package io.github.siyukio.tools.api.parameter.request.basic;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.parameter.request.AbstractRequestValidator;
import io.github.siyukio.tools.util.XDataUtils;

import java.time.LocalDateTime;

/**
 * @author Buddy
 */
public class DateRequestValidator extends AbstractRequestValidator {

    public DateRequestValidator(String name, Boolean required,
                                String parent, String error) {
        super(name, required, parent, error);
    }

    @Override
    public Object validate(Object value) {
        String v = value.toString();
        if (v.isEmpty()) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return value;
        }
        LocalDateTime localDateTime;
        try {
            //
            localDateTime = XDataUtils.parse(v);
        } catch (Exception ex) {
            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_DATE_FORMAT);
        }
        if (localDateTime == null) {
            throw this.createApiException(ApiConstants.ERROR_PARAMETER_REQUIRED_DATE_FORMAT);
        }
        return localDateTime;
    }

}
