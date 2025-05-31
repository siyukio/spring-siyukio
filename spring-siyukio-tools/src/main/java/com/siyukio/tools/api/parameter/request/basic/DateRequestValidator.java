package com.siyukio.tools.api.parameter.request.basic;

import com.siyukio.tools.api.constants.ApiConstants;
import com.siyukio.tools.api.parameter.request.AbstractRequestValidator;
import com.siyukio.tools.util.DateUtils;

import java.util.Date;

/**
 * @author Buddy
 */
public class DateRequestValidator extends AbstractRequestValidator {

    public DateRequestValidator(String name, boolean required, String parent, String error) {
        super(name, required, parent, error);
    }

    @Override
    public Object validate(Object value) {
        String v = value.toString();
        if (v.isEmpty()) {
            return null;
        }
        if (value instanceof Date) {
            return value;
        }
        Date date;
        try {
            //
            date = DateUtils.parse(v);
        } catch (Exception ex) {
            throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_DATE_FORMAT);
        }
        if (date == null) {
            throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_DATE_FORMAT);
        }
        return date;
    }

}
