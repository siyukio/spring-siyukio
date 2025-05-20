package com.siyukio.tools.api;

import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines an API execution exception.
 *
 * @author Buddy
 */
@ToString
@Getter
public final class ApiException extends RuntimeException {

    private final int error;

    private final String message;

    private final Map<String, Object> data = new HashMap<>();

    public ApiException(String message) {
        super(message);
        this.message = message;
        this.error = ApiError.PROCESS_ERROR.error;
    }

    public ApiException(int error, String message) {
        super(message);
        this.message = message;
        this.error = error;
    }

    public ApiException(ApiError apiError) {
        super(apiError.message);
        this.message = apiError.message;
        this.error = apiError.error;
    }

    public void putData(String name, Object value) {
        this.data.put(name, value);
    }

}
