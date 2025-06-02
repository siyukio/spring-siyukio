package com.siyukio.application;

import com.siyukio.tools.api.ApiError;
import com.siyukio.tools.api.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.lang.reflect.UndeclaredThrowableException;

/**
 * @author Buddy
 */
@Slf4j
@RestControllerAdvice
public class MyExceptionAdvice {

    @ExceptionHandler(value = HttpRequestMethodNotSupportedException.class)
    public JSONObject errorHandler(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ApiException apiException = new ApiException(ApiError.METHOD_NOT_ALLOWED);
        return apiException.toJson();
    }

    @ExceptionHandler(value = NoHandlerFoundException.class)
    public JSONObject errorHandler(NoHandlerFoundException ex, HttpServletRequest request) {
        ApiException apiException = new ApiException(ApiError.NOT_FOUND);
        return apiException.toJson();
    }

    @ExceptionHandler(value = ApiException.class)
    public JSONObject errorHandler(ApiException ex, HttpServletRequest request) {
        return ex.toJson();
    }

    @ExceptionHandler(value = UndeclaredThrowableException.class)
    public JSONObject errorHandler(UndeclaredThrowableException ex, HttpServletRequest request) {
        Throwable t = ex.getUndeclaredThrowable();
        ApiException apiException = ApiException.getUnknownApiException(t);
        return apiException.toJson();
    }

    @ExceptionHandler(value = Throwable.class)
    public JSONObject errorHandler(Throwable ex, HttpServletRequest request) {
        ApiException apiException = ApiException.getUnknownApiException(ex);
        return apiException.toJson();
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void ignoreAsyncRequestTimeoutException(AsyncRequestTimeoutException ex) {
        log.error("ignoreAsyncRequestTimeoutException", ex);
    }

}
