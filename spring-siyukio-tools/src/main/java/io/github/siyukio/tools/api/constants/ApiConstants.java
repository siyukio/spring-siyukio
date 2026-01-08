package io.github.siyukio.tools.api.constants;

/**
 * @author Buddy
 */
public interface ApiConstants {

    String ATTRIBUTE_TOKEN = "siyukio.token";

    String ATTRIBUTE_REQUEST_BODY = "siyukio.requestBody";

    String ATTRIBUTE_REQUEST_BODY_JSON = "siyukio.requestBodyJson";


    //error message

    String ERROR_PARAMETER_REQUIRED_FORMAT =
            "The parameter '%s' is required.";

    String ERROR_PARAMETER_PARSE_ARRAY_FORMAT =
            "Failed to parse the parameter '%s' as an array.";

    String ERROR_PARAMETER_REQUIRED_ARRAY_FORMAT =
            "The parameter '%s' must be an array.";

    String ERROR_PARAMETER_REQUIRED_ARRAY_MIN_FORMAT =
            "The parameter '%s' must be an array with size >= %s.";

    String ERROR_PARAMETER_REQUIRED_ARRAY_MAX_FORMAT =
            "The parameter '%s' must be an array with size <= %s.";


    String ERROR_PARAMETER_PARSE_OBJECT_FORMAT =
            "Failed to parse the parameter '%s' as an object.";

    String ERROR_PARAMETER_REQUIRED_OBJECT_FORMAT =
            "The parameter '%s' must be an object.";


    String ERROR_PARAMETER_REQUIRED_BOOLEAN_FORMAT =
            "The parameter '%s' must be a boolean value [true|false|1|0].";

    String ERROR_PARAMETER_REQUIRED_DATE_FORMAT =
            "The parameter '%s' must be a date in the format [yyyy-MM-dd[HH:mm[:ss]]|millis].";

    String ERROR_PARAMETER_REQUIRED_INTEGER_FORMAT =
            "The parameter '%s' must be an integer.";

    String ERROR_PARAMETER_REQUIRED_INTEGER_MIN_FORMAT =
            "The parameter '%s' must be an integer >= %s.";

    String ERROR_PARAMETER_REQUIRED_INTEGER_MAX_FORMAT =
            "The parameter '%s' must be an integer <= %s.";

    String ERROR_PARAMETER_REQUIRED_NUMBER_FORMAT =
            "The parameter '%s' must be a number.";

    String ERROR_PARAMETER_REQUIRED_NUMBER_MIN_FORMAT =
            "The parameter '%s' must be a number >= %s.";

    String ERROR_PARAMETER_REQUIRED_NUMBER_MAX_FORMAT =
            "The parameter '%s' must be a number <= %s.";

    String ERROR_PARAMETER_REQUIRED_REGEX_FORMAT =
            "The parameter '%s' must match the regular expression: %s.";

    String ERROR_PARAMETER_REQUIRED_STRING_MIN_FORMAT =
            "The parameter '%s' must be a string with length >= %s.";

    String ERROR_PARAMETER_REQUIRED_STRING_MAX_FORMAT =
            "The parameter '%s' must be a string with length <= %s.";


    String ERROR_DATE_UNSUPPORTED_FORMAT =
            "Unsupported type '%s' in %s. Please use LocalDateTime instead.";

}
