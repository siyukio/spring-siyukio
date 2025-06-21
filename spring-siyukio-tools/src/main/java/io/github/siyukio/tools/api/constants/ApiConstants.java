package io.github.siyukio.tools.api.constants;

/**
 * @author Buddy
 */
public interface ApiConstants {

    public String PROPERTY_API_JWT_PUBLIC = "API_JWT_PUBLIC_KEY";

    public String PROPERTY_API_JWT_PRIVATE = "API_JWT_PRIVATE_KEY";

    public String PROPERTY_API_JWT_ACCESS_TOKEN_DURATION = "API_JWT_ACCESS_DURATION";

    public String PROPERTY_API_JWT_REFRESH_TOKEN_DURATION = "API_JWT_REFRESH_DURATION";

    public String PROPERTY_API_SIGNATURE_SALT = "API_SIGNATURE_SALT";


    public String AUTHORIZATION = "Authorization";

    public String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

    public String ATTRIBUTE_TOKEN = "siyukio.token";

    public String ATTRIBUTE_REQUEST_BODY = "siyukio.requestBody";

    public String ATTRIBUTE_REQUEST_BODY_JSON = "siyukio.requestBodyJson";


    //ApiParameter
    public String TYPE_STRING = "string";

    public String TYPE_INTEGER = "integer";

    public String TYPE_NUMBER = "number";

    public String TYPE_DATE = "date";

    public String TYPE_BOOLEAN = "boolean";

    public String TYPE_ARRAY = "array";

    public String TYPE_OBJECT = "object";

    //error message

    public String ERROR_PARAMETER_REQUIRED_FORMAT = "The parameter '%s' is required.";

    public String ERROR_PARAMETER_PARSE_ARRAY_FORMAT = "The parameter '%s' unable to parse as an array.";

    public String ERROR_PARAMETER_REQUIRED_ARRAY_FORMAT = "The parameter '%s' must be an array.";

    public String ERROR_PARAMETER_REQUIRED_ARRAY_LIMIT_FORMAT = "The parameter '%s' must be an array[%s,%s].";

    public String ERROR_PARAMETER_PARSE_OBJECT_FORMAT = "The parameter '%s' unable to parse as an object.";

    public String ERROR_PARAMETER_REQUIRED_OBJECT_FORMAT = "The parameter '%s' must be an object.";

    public String ERROR_PARAMETER_REQUIRED_BOOLEAN_FORMAT = "The parameter '%s' must be of boolean type [true | false | 1 | 0].";

    public String ERROR_PARAMETER_REQUIRED_DATE_FORMAT = "The parameter '%s' must be a date in the format [yyyy-MM-dd[ HH:mm[:ss]] | millis].";

    public String ERROR_PARAMETER_REQUIRED_INTEGER_FORMAT = "The parameter '%s' must be an integer.";

    public String ERROR_PARAMETER_REQUIRED_INTEGER_LIMIT_FORMAT = "The parameter '%s' must be an integer[%s,%s].";

    public String ERROR_PARAMETER_REQUIRED_NUMBER_FORMAT = "The parameter '%s' must be an number.";

    public String ERROR_PARAMETER_REQUIRED_NUMBER_LIMIT_FORMAT = "The parameter '%s' must be an number[%s,%s].";

    public String ERROR_PARAMETER_REQUIRED_REGEX_FORMAT = "The parameter '%s' must match the regular:%s expression.";

    public String ERROR_PARAMETER_REQUIRED_STRING_LIMIT_FORMAT = "The parameter '%s' must be an string[%s,%s].";

}
