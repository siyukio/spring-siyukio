package com.siyukio.tools.api;

import lombok.ToString;

/**
 * @author Buddy
 */
@ToString
public enum ApiError {

    REQUEST_INVALID(400, "bad request"),

    AUTHORIZED_ERROR(401, "unauthorized"),

    FORBIDDEN_ROLE(403, "forbidden"),

    NOT_FOUND(404, "not found"),

    METHOD_NOT_ALLOWED(405, "method not allowed"),

    PROCESS_ERROR(422, "process error"),

    EXCEPTION(500, "exception");

    public final int error;
    public final String message;

    ApiError(int error, String message) {
        this.error = error;
        this.message = message;
    }
}
