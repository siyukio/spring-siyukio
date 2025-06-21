package io.github.siyukio.tools.api.parameter.request;


import io.github.siyukio.tools.api.ApiException;
import io.github.siyukio.tools.api.constants.ApiConstants;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Buddy
 */
public abstract class AbstractRequestValidator implements RequestValidator {

    protected final String name;

    protected final String parent;

    protected final boolean required;

    protected final String error;

    public AbstractRequestValidator(String name, boolean required, String parent, String error) {
        this.name = name;
        this.parent = parent;
        this.required = required;
        this.error = error;
    }

    @Override
    public final String getName() {
        return this.name;
    }

    @Override
    public final boolean isRequired() {
        return required;
    }

    protected final String getFullName() {
        String fullName = this.name;
        if (StringUtils.hasText(this.parent)) {
            fullName = this.parent + "." + this.name;
        }
        return fullName;
    }

    protected String toNumLimitString(long num) {
        String result = "N";
        if (num != -1) {
            result = Long.toString(num);
        }
        return result;
    }

    protected final ApiException createApiException(Object value, String format) {
        ApiException apiException;
        String fullName = this.getFullName();
        if (StringUtils.hasText(this.error)) {
            apiException = ApiException.getInvalidApiException(this.error);
        } else {
            String errorMessage = String.format(format, this.getFullName());
            apiException = ApiException.getInvalidApiException(errorMessage);
        }
        apiException.putData(fullName, value);
        return apiException;
    }

    protected final ApiException createApiException(Object value, String format, Object... values) {
        ApiException apiException;
        String fullName = this.getFullName();
        if (StringUtils.hasText(this.error)) {
            apiException = ApiException.getInvalidApiException(this.error);
        } else {
            List<Object> valueList = new ArrayList<>(List.of(values));
            valueList.addFirst(this.getFullName());
            String errorMessage = String.format(format, valueList.toArray());
            apiException = ApiException.getInvalidApiException(errorMessage);
        }
        apiException.putData(fullName, value);
        return apiException;
    }

    protected final void assertRequired(Object value) {
        if (this.required) {
            if (JSONObject.NULL.equals(value) || value.toString().isEmpty()) {
                throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_FORMAT);
            }
        }
    }
}
