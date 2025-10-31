package io.github.siyukio.tools.api;

import io.github.siyukio.tools.api.constants.ApiConstants;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

/**
 * Defines an API execution exception.
 *
 * @author Buddy
 */
@ToString
@Slf4j
public final class ApiException extends RuntimeException {

    public final int error;

    public final String errorReason;

    public ApiException(String errorReason) {
        super(errorReason);
        this.errorReason = errorReason;
        this.error = HttpStatus.UNPROCESSABLE_ENTITY.value();
    }

    public ApiException(int error, String errorReason) {
        super(errorReason);
        this.errorReason = errorReason;
        this.error = error;
    }

    public ApiException(HttpStatus httpStatus) {
        super(httpStatus.getReasonPhrase());
        this.errorReason = httpStatus.getReasonPhrase();
        this.error = httpStatus.value();
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

    public static String getErrorName() {
        return "error";
    }

    public static JSONObject getErrorParameter() {
        JSONObject responseParameter = new JSONObject();
        responseParameter.put("name", "error");
        responseParameter.put("description", "error code");
        responseParameter.put("type", ApiConstants.TYPE_INTEGER);
        return responseParameter;
    }

    public static String getErrorReasonName() {
        return "errorReason";
    }

    public static JSONObject getErrorReasonParameter() {
        JSONObject responseParameter = new JSONObject();
        responseParameter.put("name", "errorReason");
        responseParameter.put("description", "error reason");
        responseParameter.put("type", ApiConstants.TYPE_STRING);
        return responseParameter;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("error", this.error);
        jsonObject.put("errorReason", this.errorReason);
        return jsonObject;
    }
}
