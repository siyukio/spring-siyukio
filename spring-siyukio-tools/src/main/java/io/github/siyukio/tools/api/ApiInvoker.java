package io.github.siyukio.tools.api;

import io.github.siyukio.tools.util.JsonUtils;
import lombok.ToString;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @author Buddy
 */
@ToString
public class ApiInvoker {

    private final Object bean;

    private final Method method;

    public ApiInvoker(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
    }

    private int supportsParameter(Parameter parameter, Object... objects) {
        Object obj;
        for (int i = 0; i < objects.length; i++) {
            obj = objects[i];
            if (parameter.getType().equals(obj.getClass()) || parameter.getType().isInstance(obj)) {
                return i;
            }
        }
        return -1;
    }

    public Object invoke(JSONObject requestBody, Object... objects) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Parameter[] parameters = this.method.getParameters();
        Object[] args = new Object[parameters.length];
        Parameter parameter;
        Object obj;
        int objIndex;
        for (int index = 0; index < parameters.length; index++) {
            parameter = parameters[index];
            objIndex = this.supportsParameter(parameter, objects);
            if (objIndex >= 0) {
                obj = objects[objIndex];
            } else if (requestBody != null) {
                obj = JsonUtils.copy(requestBody, parameter.getType());
            } else {
                obj = null;
            }
            args[index] = obj;
        }

        return this.method.invoke(bean, args);
    }

}
