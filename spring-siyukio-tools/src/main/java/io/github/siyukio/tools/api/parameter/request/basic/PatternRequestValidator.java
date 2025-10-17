package io.github.siyukio.tools.api.parameter.request.basic;

import io.github.siyukio.tools.api.constants.ApiConstants;
import io.github.siyukio.tools.api.parameter.request.AbstractRequestValidator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Buddy
 */
public class PatternRequestValidator extends AbstractRequestValidator {

    private final String regex;

    private Pattern pattern = null;

    public PatternRequestValidator(String key, boolean required, String regex, String parent, String message) {
        super(key, required, parent, message);
        this.regex = regex;
    }

    @Override
    public Object validate(Object value) {
        String v = value.toString();
        if (v.isEmpty()) {
            return null;
        }
        //
        if (this.pattern == null) {
            pattern = Pattern.compile(this.regex);
        }
        Matcher matcher = this.pattern.matcher(v);
        if (!matcher.matches()) {
            throw this.createApiException(value, ApiConstants.ERROR_PARAMETER_REQUIRED_REGEX_FORMAT, this.regex);
        }
        return value;
    }

}
