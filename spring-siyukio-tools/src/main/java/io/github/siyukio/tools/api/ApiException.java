package io.github.siyukio.tools.api;

import io.github.siyukio.tools.api.constants.ApiConstants;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

/**
 * Defines an API execution exception.
 *
 * @author Buddy
 */
@ToString
@Slf4j
@Getter
public final class ApiException extends RuntimeException {

    private final int code;

    private final String message;

    public ApiException(String message) {
        super(message);
        this.message = message;
        this.code = HttpStatus.UNPROCESSABLE_ENTITY.value();
    }

    public ApiException(int code, String message) {
        super(message);
        this.message = message;
        this.code = code;
    }

    public ApiException(HttpStatus httpStatus) {
        super(httpStatus.getReasonPhrase());
        this.message = httpStatus.getReasonPhrase();
        this.code = httpStatus.value();
    }

    public static ApiException getApiException(HttpStatus httpStatus, String errorReason) {
        return new ApiException(httpStatus.value(), errorReason);
    }

    public static ApiException getInvalidApiException(String error) {
        return new ApiException(HttpStatus.BAD_REQUEST.value(), error);
    }

    public static ApiException getUnknownApiException(Throwable ex) {
        if (ex instanceof ApiException apiException) {
            return apiException;
        }

        log.error("unknown exception", ex);

        String message = ex.getClass().getName();
        int index = message.lastIndexOf(".");
        if (index > 0) {
            message = message.substring(index + 1);
        }
        message = message + ":" + ex.getMessage();
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    public static JSONObject getErrorSchema() {
        JSONObject codeParameter = new JSONObject();
        codeParameter.put("name", "code");
        codeParameter.put("description", "The error type that occurred");
        codeParameter.put("type", ApiConstants.TYPE_INTEGER);

        JSONObject messageParameter = new JSONObject();
        messageParameter.put("name", "message");
        messageParameter.put("description", "A short description of the error. The message SHOULD be limited to a concise single sentence");
        messageParameter.put("type", ApiConstants.TYPE_STRING);

        JSONArray childArray = new JSONArray();
        childArray.put(codeParameter);
        childArray.put(messageParameter);

        JSONObject errorParameter = new JSONObject();
        errorParameter.put("name", "error");
        errorParameter.put("description", "Error information if the request failed");
        errorParameter.put("type", ApiConstants.TYPE_OBJECT);
        errorParameter.put("additionalProperties", false);
        errorParameter.put("childArray", childArray);
        return errorParameter;
    }

    public JSONObject toJson() {
        JSONObject errorJson = new JSONObject();
        errorJson.put("code", this.code);
        errorJson.put("message", this.message);
        JSONObject responseObject = new JSONObject();
        responseObject.put("error", errorJson);
        return responseObject;
    }
}
