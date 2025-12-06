package io.github.siyukio.tools.api;

import io.github.siyukio.tools.api.definition.ApiResponseParameter;
import io.github.siyukio.tools.api.definition.ApiSchema;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public static ApiResponseParameter getErrorResponseParameter() {
        List<ApiResponseParameter> subResponseParameters = new ArrayList<>();
        subResponseParameters.add(ApiResponseParameter.builder()
                .name("code")
                .schema(ApiSchema.builder()
                        .type(ApiSchema.Type.INTEGER)
                        .description("The error type that occurred")
                        .build())
                .build());

        subResponseParameters.add(ApiResponseParameter.builder()
                .name("message")
                .schema(ApiSchema.builder()
                        .type(ApiSchema.Type.STRING)
                        .description("A short description of the error.")
                        .build())
                .build());

        Map<String, ApiSchema> map = new LinkedHashMap<>();
        for (ApiResponseParameter apiResponseParameter : subResponseParameters) {
            map.put(apiResponseParameter.name(), apiResponseParameter.schema());
        }

        return ApiResponseParameter.builder()
                .name("error")
                .schema(ApiSchema.builder()
                        .type(ApiSchema.Type.OBJECT)
                        .properties(map)
                        .build())
                .properties(subResponseParameters)
                .build();
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
