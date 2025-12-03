package io.github.siyukio.tools.api.constants;

/**
 * @author Buddy
 */
public interface ApiConstants {

    String PROPERTY_API_JWT_PUBLIC = "API_JWT_PUBLIC_KEY";

    String PROPERTY_API_JWT_PRIVATE = "API_JWT_PRIVATE_KEY";

    String PROPERTY_API_JWT_PASSWORD = "API_JWT_PASSWORD";

    String PROPERTY_API_JWT_ACCESS_TOKEN_DURATION = "API_JWT_ACCESS_DURATION";

    String PROPERTY_API_JWT_REFRESH_TOKEN_DURATION = "API_JWT_REFRESH_DURATION";

    String PROPERTY_API_SIGNATURE_SALT = "API_SIGNATURE_SALT";

    String PROPERTY_API_PROFILES_DOCS = "API_PROFILES_DOCS";

    String PROPERTY_API_PROFILES_ACTIVE = "API_PROFILES_ACTIVE";

    String ATTRIBUTE_TOKEN = "siyukio.token";

    String ATTRIBUTE_REQUEST_BODY = "siyukio.requestBody";

    String ATTRIBUTE_REQUEST_BODY_JSON = "siyukio.requestBodyJson";


    //ApiParameter
    String TYPE_STRING = "string";

    String TYPE_INTEGER = "integer";

    String TYPE_NUMBER = "number";

    String TYPE_DATE = "date";

    String TYPE_BOOLEAN = "boolean";

    String TYPE_ARRAY = "array";

    String TYPE_OBJECT = "object";

    //error message

    String ERROR_PARAMETER_REQUIRED_FORMAT = "The parameter '%s' is required.";

    String ERROR_PARAMETER_PARSE_ARRAY_FORMAT = "The parameter '%s' unable to parse as an array.";

    String ERROR_PARAMETER_REQUIRED_ARRAY_FORMAT = "The parameter '%s' must be an array.";

    String ERROR_PARAMETER_REQUIRED_ARRAY_LIMIT_FORMAT = "The parameter '%s' must be an array[%s,%s].";

    String ERROR_PARAMETER_PARSE_OBJECT_FORMAT = "The parameter '%s' unable to parse as an object.";

    String ERROR_PARAMETER_REQUIRED_OBJECT_FORMAT = "The parameter '%s' must be an object.";

    String ERROR_PARAMETER_REQUIRED_BOOLEAN_FORMAT = "The parameter '%s' must be of boolean type [true | false | 1 | 0].";

    String ERROR_PARAMETER_REQUIRED_DATE_FORMAT = "The parameter '%s' must be a date in the format [yyyy-MM-dd[ HH:mm[:ss]] | millis].";

    String ERROR_PARAMETER_REQUIRED_INTEGER_FORMAT = "The parameter '%s' must be an integer.";

    String ERROR_PARAMETER_REQUIRED_INTEGER_LIMIT_FORMAT = "The parameter '%s' must be an integer[%s,%s].";

    String ERROR_PARAMETER_REQUIRED_NUMBER_FORMAT = "The parameter '%s' must be an number.";

    String ERROR_PARAMETER_REQUIRED_NUMBER_LIMIT_FORMAT = "The parameter '%s' must be an number[%s,%s].";

    String ERROR_PARAMETER_REQUIRED_REGEX_FORMAT = "The parameter '%s' must match the regular:%s expression.";

    String ERROR_PARAMETER_REQUIRED_STRING_LIMIT_FORMAT = "The parameter '%s' must be an string[%s,%s].";

    String ERROR_DATE_UNSUPPORTED_FORMAT = "Unsupported type: '%s' in %s. Please use LocalDateTime instead.";

}
