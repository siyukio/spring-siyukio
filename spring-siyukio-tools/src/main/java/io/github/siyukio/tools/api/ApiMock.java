package io.github.siyukio.tools.api;

import com.fasterxml.jackson.databind.JavaType;
import io.github.siyukio.tools.api.signature.SignatureProvider;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.api.token.TokenProvider;
import io.github.siyukio.tools.util.JsonUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Slf4j
public class ApiMock {

    @Setter
    private Token token;

    @Autowired
    private AipHandlerManager aipHandlerManager;

    @Autowired
    private SignatureProvider signatureProvider;

    @Autowired
    private TokenProvider tokenProvider;

    public void setAuthorization(String authorization) {
        this.token = this.tokenProvider.verifyToken(authorization);
    }

    public JSONObject perform(String path, Object request) {
        JSONObject requestJson = JsonUtils.copy(request, JSONObject.class);

        ApiHandler apiHandler = this.aipHandlerManager.getApiHandler(path);
        if (apiHandler == null) {
            throw new ApiException(HttpStatus.NOT_FOUND);
        }

        if (apiHandler.apiDefinition().signature()) {
            long timestamp = requestJson.optLong("timestamp", 0);
            String nonce = requestJson.optString("nonce");
            String signature = requestJson.optString("signature");
            this.signatureProvider.validate(timestamp, nonce, signature);
        }

        if (apiHandler.apiDefinition().authorization()) {
            if (token == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED);
            }

            if (!apiHandler.apiDefinition().roles().isEmpty()) {
                Set<String> roleSet = new HashSet<>(apiHandler.apiDefinition().roles());

                if (!CollectionUtils.isEmpty(token.roles())) {
                    roleSet.retainAll(token.roles());
                }
                
                if (roleSet.isEmpty()) {
                    throw new ApiException(HttpStatus.FORBIDDEN);
                }
            }
        }

        requestJson = apiHandler.requestValidator().validate(requestJson);

        List<Object> paramList = new ArrayList<>();
        if (token != null) {
            paramList.add(token);
        }

        Object[] params = paramList.toArray(new Object[0]);
        Object resultValue;
        try {
            resultValue = apiHandler.apiInvoker().invoke(requestJson, params);
        } catch (IllegalAccessException | IllegalArgumentException ex) {
            throw ApiException.getUnknownApiException(ex);
        } catch (InvocationTargetException ex) {
            Throwable throwable = ex.getTargetException();
            throw ApiException.getUnknownApiException(throwable);
        }

        Class<?> returnType = apiHandler.apiDefinition().realReturnType();

        if (returnType == void.class) {
            return new JSONObject();
        }

        JSONObject resultJson = JsonUtils.copy(resultValue, JSONObject.class);

        apiHandler.responseFilter().filter(resultJson);
        return resultJson;
    }

    public <T> T perform(String path, Object request, Class<T> responseClass) {
        JSONObject responseJson = this.perform(path, request);
        return JsonUtils.copy(responseJson, responseClass);
    }

    public <O, I, T> T perform(String path, Object request, Class<O> outerClass, final Class<I> innerClass) {
        JSONObject responseJson = this.perform(path, request);
        JavaType type = JsonUtils.getObjectMapper().getTypeFactory().constructParametricType(outerClass, innerClass);
        return JsonUtils.copy(responseJson, type);
    }

    public <T> T perform(String path, String subPath, Object request, Class<T> responseClass) {
        JSONObject responseJson = this.perform(path + subPath, request);
        return JsonUtils.copy(responseJson, responseClass);
    }

    public <O, I, T> T perform(String path, String subPath, Object request, Class<O> outerClass, final Class<I> innerClass) {
        JSONObject responseJson = this.perform(path + subPath, request);
        JavaType type = JsonUtils.getObjectMapper().getTypeFactory().constructParametricType(outerClass, innerClass);
        return JsonUtils.copy(responseJson, type);
    }
}
