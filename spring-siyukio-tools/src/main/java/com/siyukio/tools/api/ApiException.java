package com.siyukio.tools.api;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines an API execution exception.
 *
 * @author Buddy
 */
@ToString
@Slf4j
public final class ApiException extends RuntimeException {

    public final int error;

    public final String message;

    public final Map<String, Object> data = new HashMap<>();

    public ApiException(String message) {
        super(message);
        this.message = message;
        this.error = HttpStatus.UNPROCESSABLE_ENTITY.value();
    }

    private ApiException(int error, String message) {
        super(message);
        this.message = message;
        this.error = error;
    }

    public ApiException(HttpStatus httpStatus) {
        super(httpStatus.getReasonPhrase());
        this.message = httpStatus.getReasonPhrase();
        this.error = httpStatus.value();
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

    public void putData(String name, Object value) {
        this.data.put(name, value);
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("error", this.error);
        jsonObject.put("message", this.message);
        jsonObject.put("data", this.data);
        return jsonObject;
    }

}
