package com.siyukio.tools.api.definition;

import com.siyukio.tools.api.ApiRequest;
import com.siyukio.tools.api.annotation.ApiController;
import com.siyukio.tools.api.annotation.ApiMapping;
import com.siyukio.tools.api.annotation.ApiParameter;
import com.siyukio.tools.api.annotation.Example;
import com.siyukio.tools.api.constants.ApiConstants;
import com.siyukio.tools.api.token.Token;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import java.lang.reflect.*;
import java.util.*;

/**
 * @author Buddy
 */
@Slf4j
public final class ApiDefinitionManager {

    private final Map<String, ApiDefinition> apiDefinitionMap = new HashMap<>();

    private final Set<Class<?>> alternativeSet = new HashSet<>();

    public static boolean isBasicType(Class<?> type) {
        return type.isPrimitive() || type == String.class || type == Date.class || Number.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type);
    }

    public void addAlternative(Class<?> clazz) {
        this.alternativeSet.add(clazz);
    }

    public ApiDefinition getApiDefinition(String path) {
        ApiDefinition apiDefinition = this.apiDefinitionMap.get(path);
        if (apiDefinition == null) {
            //Wildcard Path
            int index = path.lastIndexOf("/");
            if (index > 0) {
                String newPath = path.substring(0, index);
                newPath += "/*";
                apiDefinition = this.apiDefinitionMap.get(newPath);
            }
        }
        return apiDefinition;
    }

    public Map<String, ApiDefinition> getApiDefinitionMap() {
        return Collections.unmodifiableMap(this.apiDefinitionMap);
    }

    public boolean isApi(Class<?> type, Method method) {
        return type.isAnnotationPresent(ApiController.class) && method.isAnnotationPresent(ApiMapping.class);
    }

    public ApiDefinition addApi(Class<?> type, Method method) {
        ApiController apiController = type.getAnnotation(ApiController.class);
        assert apiController != null;
        ApiMapping apiMapping = method.getAnnotation(ApiMapping.class);
        assert apiMapping != null;

        ApiDefinition apiDefinition = this.parseMethod(type, method, apiController, apiMapping);
        String apiPath;
        for (String mappingPath : apiMapping.path()) {
            if (apiController.path().length > 0) {
                for (String controllerPath : apiController.path()) {
                    apiPath = controllerPath + mappingPath;
                    if (this.apiDefinitionMap.containsKey(apiPath)) {
                        continue;
                    }
                    apiDefinition.paths().add(apiPath);
                    this.apiDefinitionMap.put(apiPath, apiDefinition);
                }
            } else {
                apiPath = mappingPath;
                apiDefinition.paths().add(apiPath);
                this.apiDefinitionMap.put(apiPath, apiDefinition);
            }
        }
        return apiDefinition;
    }

    private Class<?> getRealReturnType(Method method) {
        Class<?> returnValueType = method.getReturnType();
        for (Class<?> clazz : alternativeSet) {
            if (clazz.isAssignableFrom(returnValueType)) {
                Type generictype = method.getGenericReturnType();
                ParameterizedType listGenericType = (ParameterizedType) generictype;
                Type[] listActualTypeArguments = listGenericType.getActualTypeArguments();
                if (listActualTypeArguments.length > 0) {
                    returnValueType = (Class<?>) listActualTypeArguments[0];
                    break;
                }
            }
        }
        return returnValueType;
    }

    private ApiDefinition parseMethod(Class<?> type, Method method, ApiController apiController, ApiMapping apiMapping) {

        boolean sampling = false;
        JSONArray requestParameters = new JSONArray();
        //create request parameter
        LinkedList<Class<?>> requestClassLinked = new LinkedList<>();
        JSONObject requestParameter;
        Set<String> existParameterNameSet = new HashSet<>();
        JSONArray subRequestParameters;
        String parameterName;
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            if (!sampling && parameter.getType() == McpSyncServerExchange.class) {
                sampling = true;
            }
            subRequestParameters = this.getSubRequestParameters(method, parameter.getType(), requestClassLinked);
            for (int index = 0; index < subRequestParameters.length(); index++) {
                requestParameter = subRequestParameters.getJSONObject(index);
                parameterName = requestParameter.optString("name");
                if (!existParameterNameSet.contains(parameterName)) {
                    existParameterNameSet.add(parameterName);
                    requestParameters.put(requestParameter);
                }
            }
        }

        //create response parameter
        Class<?> returnValueType = this.getRealReturnType(method);
        if (returnValueType != void.class && returnValueType != Void.class && returnValueType != String.class) {
            if (isBasicType(returnValueType) || returnValueType.isArray() || Collection.class.isAssignableFrom(returnValueType) || returnValueType.getPackageName().startsWith("java.")) {
                throw new RuntimeException(type.getName() + "." + method.getName() + " unsupported returnValueType:" + returnValueType.getSimpleName());
            }
        }
        existParameterNameSet.clear();

        LinkedList<Class<?>> responseClassLinkedList = new LinkedList<>();
        JSONArray responseParameters = this.getSubResponseParameters(method, returnValueType, responseClassLinkedList);

        String summary = apiMapping.summary();
        if (!StringUtils.hasText(summary)) {
            summary = method.getName();
        }
        ApiDefinition.ApiDefinitionBuilder builder = ApiDefinition.builder();
        builder.id(type.getSimpleName() + "_" + method.getName())
                .paths(new ArrayList<>())
                .summary(summary)
                .deprecated(apiMapping.deprecated())
                .description(apiMapping.description())
                .authorization(apiMapping.authorization())
                .signature(apiMapping.signature())
                .tags(List.of(apiController.tags()))
                .mcpTool(apiMapping.mcpTool())
                .roles(List.of(apiMapping.roles()))
                .returnType(method.getReturnType())
                .realReturnType(returnValueType)
                .sampling(sampling)
                .requestParameters(requestParameters)
                .responseParameters(responseParameters);
        return builder.build();
    }

    private JSONObject createBasicArrayRequestParameter(Class<?> typeClass, String name, ApiParameter apiParameter) {
        JSONObject requestParameter = new JSONObject();
        requestParameter.put("name", name);
        requestParameter.put("type", ApiConstants.TYPE_ARRAY);
        requestParameter.put("description", apiParameter.description());
        requestParameter.put("required", apiParameter.required());
        requestParameter.put("error", apiParameter.error());

        String itemType;
        if (typeClass == boolean.class || typeClass == Boolean.class) {
            itemType = ApiConstants.TYPE_BOOLEAN;
        } else if (typeClass == long.class || typeClass == Long.class || int.class == typeClass || typeClass == Integer.class || short.class == typeClass || typeClass == Short.class) {
            itemType = ApiConstants.TYPE_INTEGER;
        } else if (typeClass == double.class || typeClass == Double.class || typeClass == float.class || typeClass == Float.class || Number.class.isAssignableFrom(typeClass)) {
            itemType = ApiConstants.TYPE_NUMBER;
        } else if (typeClass == String.class) {
            itemType = ApiConstants.TYPE_STRING;
        } else if (typeClass == Date.class) {
            itemType = ApiConstants.TYPE_DATE;
        } else {
            itemType = typeClass.getSimpleName();
        }
        JSONObject items = new JSONObject();
        items.put("type", itemType);
        requestParameter.put("items", items);
        return requestParameter;
    }

    private JSONObject createObjectArrayRequestParameter(Method method, Class<?> typeClass, LinkedList<Class<?>> requestClassLinked, String name, ApiParameter apiParameter) {
        JSONObject requestParameter = new JSONObject();
        requestParameter.put("name", name);
        requestParameter.put("type", ApiConstants.TYPE_ARRAY);
        requestParameter.put("description", apiParameter.description());
        requestParameter.put("required", apiParameter.required());
        requestParameter.put("error", apiParameter.error());
        requestParameter.put("masked", false);
        //
        JSONObject items = new JSONObject();
        items.put("type", ApiConstants.TYPE_OBJECT);
        requestParameter.put("items", items);
        JSONArray subRequestParameters = this.getSubRequestParameters(method, typeClass, requestClassLinked);
        items.put("childArray", subRequestParameters);
        //
        requestParameter.put("items", items);
        return requestParameter;
    }

    private void defineNumber(JSONObject requestParameter, ApiParameter apiParameter) {
        long maximum = apiParameter.maximum();
        long minimum = apiParameter.minimum();
        if (maximum != -1 && minimum != -1) {
            long newMax = Math.max(maximum, minimum);
            long newMin = Math.min(maximum, minimum);
            maximum = newMax;
            minimum = newMin;
        }
        if (minimum != -1) {
            requestParameter.put("minimum", minimum);
        }
        if (maximum != -1) {
            requestParameter.put("maximum", maximum);
        }
    }

    private void defineString(JSONObject requestParameter, ApiParameter apiParameter) {
        if (apiParameter.regex().isEmpty()) {
            //common String
            int maxLength = apiParameter.maxLength();
            int minLength = apiParameter.minLength();
            if (minLength < 0) {
                minLength = 0;
            }
            if (maxLength <= 0) {
                maxLength = 255;
            }
            int newMaxLength = Math.max(maxLength, minLength);
            int newMinLength = Math.min(maxLength, minLength);
            maxLength = newMaxLength;
            minLength = newMinLength;
            requestParameter.put("maxLength", maxLength);
            if (minLength > 0) {
                requestParameter.put("minLength", minLength);
            }
        } else {
            //regex
            String regex = apiParameter.regex();
            requestParameter.put("regex", regex);
        }
        if (apiParameter.password()) {
            requestParameter.put("format", "password");
        }
    }

    private void defineDate(JSONObject requestParameter) {
        requestParameter.put("format", "date-time");
        requestParameter.put("example", "2025-01-01 00:00:00");
    }

    private JSONObject createBasicRequestParameter(Class<?> typeClass, String name, ApiParameter apiParameter) {
        JSONObject requestParameter = new JSONObject();
        requestParameter.put("name", name);
        requestParameter.put("description", apiParameter.description());
        requestParameter.put("error", apiParameter.error());

        String type;
        if (typeClass == boolean.class || typeClass == Boolean.class) {
            type = ApiConstants.TYPE_BOOLEAN;
        } else if (typeClass == long.class || typeClass == Long.class || int.class == typeClass || typeClass == Integer.class || short.class == typeClass || typeClass == Short.class) {
            type = ApiConstants.TYPE_INTEGER;
            this.defineNumber(requestParameter, apiParameter);
        } else if (typeClass == double.class || typeClass == Double.class || typeClass == float.class || typeClass == Float.class || Number.class.isAssignableFrom(typeClass)) {
            type = ApiConstants.TYPE_NUMBER;
            this.defineNumber(requestParameter, apiParameter);
        } else if (typeClass == String.class) {
            type = ApiConstants.TYPE_STRING;
            this.defineString(requestParameter, apiParameter);
        } else if (typeClass == Date.class) {
            type = ApiConstants.TYPE_DATE;
            this.defineDate(requestParameter);
        } else {
            type = typeClass.getSimpleName();
        }
        requestParameter.put("type", type);

        requestParameter.put("required", apiParameter.required());
        if (!apiParameter.required()) {
            if (StringUtils.hasText(apiParameter.defaultValue())) {
                Object defaultValue = this.getDefaultValue(type, apiParameter.defaultValue());
                requestParameter.put("default", defaultValue);
            }
        }

        if (apiParameter.examples().length > 0) {
            Example[] examples = apiParameter.examples();
            JSONObject examplesJson = new JSONObject();
            JSONObject exampleJson;
            for (Example example : examples) {
                exampleJson = new JSONObject();
                exampleJson.put("value", example.value());
                exampleJson.put("summary", example.summary());
                examplesJson.put(example.value(), exampleJson);
            }
            requestParameter.put("examples", examplesJson);
        }

        return requestParameter;
    }

    private Object getDefaultValue(String type, String defaultValue) {
        Object result = null;
        if (!defaultValue.isEmpty()) {
            switch (type) {
                case ApiConstants.TYPE_BOOLEAN:
                    result = defaultValue.equalsIgnoreCase("true");
                    break;
                case ApiConstants.TYPE_INTEGER:
                    result = Long.valueOf(defaultValue).intValue();
                    break;
                case ApiConstants.TYPE_NUMBER:
                    result = Double.parseDouble(defaultValue);
                    break;
                case ApiConstants.TYPE_STRING:
                case ApiConstants.TYPE_DATE:
                    result = defaultValue;
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    private JSONObject createJsonArrayRequestParam(String name, ApiParameter apiParameter) {
        JSONObject requestParam = new JSONObject();
        requestParam.put("name", name);
        requestParam.put("type", ApiConstants.TYPE_ARRAY);
        requestParam.put("description", apiParameter.description());
        requestParam.put("required", apiParameter.required());
        requestParam.put("error", apiParameter.error());
        requestParam.put("masked", false);
        JSONObject items = new JSONObject();
        items.put("type", ApiConstants.TYPE_OBJECT);
        items.put("childArray", new JSONArray());
        items.put("dynamic", true);

        requestParam.put("items", items);
        int maxItems = apiParameter.maxItems();
        int minItems = apiParameter.minItems();
        if (minItems < 0) {
            minItems = 0;
        }
        if (maxItems <= 0) {
            maxItems = 255;
        }
        int newMaxSize = Math.max(maxItems, minItems);
        int newMinSize = Math.min(maxItems, minItems);
        maxItems = newMaxSize;
        minItems = newMinSize;
        requestParam.put("maxItems", maxItems);
        if (minItems > 0) {
            requestParam.put("minItems", minItems);
        }
        return requestParam;
    }

    private JSONObject createCollectionRequestParameter(Method method, Class<?> typeClass, String name, ApiParameter apiParameter, LinkedList<Class<?>> requestClassLinked) {
        JSONObject requestParameter;
        if (isBasicType(typeClass)) {
            requestParameter = this.createBasicArrayRequestParameter(typeClass, name, apiParameter);
        } else {
            requestParameter = this.createObjectArrayRequestParameter(method, typeClass, requestClassLinked, name, apiParameter);
        }
        int maxItems = apiParameter.maxItems();
        int minItems = apiParameter.minItems();
        if (minItems < 0) {
            minItems = 0;
        }
        if (maxItems <= 0) {
            maxItems = 255;
        }
        int newMaxSize = Math.max(maxItems, minItems);
        int newMinSize = Math.min(maxItems, minItems);
        maxItems = newMaxSize;
        minItems = newMinSize;
        requestParameter.put("maxItems", maxItems);
        if (minItems > 0) {
            requestParameter.put("minItems", minItems);
        }
        return requestParameter;
    }

    private JSONObject createObjectRequestParameter(String name, ApiParameter apiParameter) {
        JSONObject requestParam = new JSONObject();
        requestParam.put("name", name);
        requestParam.put("type", ApiConstants.TYPE_OBJECT);
        requestParam.put("description", apiParameter.description());
        requestParam.put("required", apiParameter.required());
        requestParam.put("error", apiParameter.error());
        //dynamic object
        requestParam.put("dynamic", true);
        //
        JSONArray subRequestParameters = new JSONArray();
        requestParam.put("childArray", subRequestParameters);
        return requestParam;
    }

    private JSONObject createObjectRequestParameter(Method method, Class<?> typeClass, LinkedList<Class<?>> requestClassLinked, String name, ApiParameter apiParameter) {
        JSONObject requestParam = new JSONObject();
        requestParam.put("name", name);
        requestParam.put("type", ApiConstants.TYPE_OBJECT);
        requestParam.put("description", apiParameter.description());
        requestParam.put("required", apiParameter.required());
        requestParam.put("error", apiParameter.error());
        requestParam.put("dynamic", false);
        //
        JSONArray subRequestParameters = this.getSubRequestParameters(method, typeClass, requestClassLinked);
        requestParam.put("childArray", subRequestParameters);
        return requestParam;
    }

    private List<Field> getClassFieldList(Class<?> parameterClass) {
        if (parameterClass == void.class || parameterClass == Void.class) {
            return Collections.emptyList();
        }
        Field[] fields = parameterClass.getDeclaredFields();
        List<Field> fileList = new ArrayList<>(Arrays.asList(fields));
        //super
        Field[] parentFields;
        Class<?> parentClass = parameterClass.getSuperclass();
        while (parentClass != null && parentClass != Object.class) {
            parentFields = parentClass.getDeclaredFields();
            fileList.addAll(Arrays.asList(parentFields));
            parentClass = parentClass.getSuperclass();
        }
        return fileList;
    }

    private Class<?> getRequestListActualType(Method method, Class<?> paramClass, Field field) {
        Type generictype = field.getGenericType();
        if (generictype instanceof ParameterizedType parameterizedType) {
            Type[] listActualTypeArguments = parameterizedType.getActualTypeArguments();
            Type listType = listActualTypeArguments[0];
            if (!(listType instanceof TypeVariable)) {
                //not T
                return (Class<?>) listType;
            }
        }
        //Type = T
        Class<?> subType = null;
        //find in method
        Type[] paramTypes = method.getGenericParameterTypes();
        for (Type type : paramTypes) {
            if (type instanceof ParameterizedType parameterizedType) {
                if (parameterizedType.getRawType().equals(paramClass)) {
                    Type[] types = parameterizedType.getActualTypeArguments();
                    subType = (Class<?>) types[0];
                    return subType;
                }
            }
        }

        //find in class
        Class<?> typeClass = paramClass;
        while (typeClass != Object.class) {
            Type type = typeClass.getGenericSuperclass();
            if (type instanceof ParameterizedType parameterizedType) {
                Type[] types = parameterizedType.getActualTypeArguments();
                subType = (Class<?>) types[0];
                break;
            } else {
                typeClass = typeClass.getSuperclass();
            }
        }
        if (subType == null) {
            log.error("{}.{} getRequestListActualType genericType error", paramClass.getName(), field.getName());
            throw new RuntimeException(paramClass.getName() + "." + field.getName() + " get genericType error");
        }
        return subType;
    }

    private Class<?> getResponseListActualType(Method method, Class<?> paramClass, Field field) {
        Type generictype = field.getGenericType();
        ParameterizedType listGenericType = (ParameterizedType) generictype;
        Type[] listActualTypeArguments = listGenericType.getActualTypeArguments();
        Type listType = listActualTypeArguments[0];
        if (!(listType instanceof TypeVariable)) {
            //not T
            return (Class<?>) listType;
        }
        //Type = T
        Class<?> subType = null;
        //find in method
        generictype = method.getGenericReturnType();
        if (generictype instanceof ParameterizedType) {
            listGenericType = (ParameterizedType) generictype;
            listActualTypeArguments = listGenericType.getActualTypeArguments();
            subType = (Class<?>) listActualTypeArguments[0];
            return subType;
        }

        //find in class
        Class<?> typeClass = paramClass;
        while (typeClass != Object.class) {
            Type type = typeClass.getGenericSuperclass();
            if (type instanceof ParameterizedType parameterizedType) {
                Type[] types = parameterizedType.getActualTypeArguments();
                subType = (Class<?>) types[0];
                break;
            } else {
                typeClass = typeClass.getSuperclass();
            }
        }
        if (subType == null) {
            log.error("{}.{} getResponseListActualType genericType error", paramClass.getName(), field.getName());
            throw new RuntimeException(paramClass.getName() + "." + field.getName() + " get genericType error");
        }
        return subType;
    }

    public JSONArray getSubRequestParameters(Method method, Class<?> typeClass, LinkedList<Class<?>> requestClassLinked) {
        JSONArray requestParameters = new JSONArray();
        if (typeClass == String.class || typeClass == ApiRequest.class || typeClass == Token.class || typeClass == McpSyncServerExchange.class) {
            return requestParameters;
        }
        //
        if (!requestClassLinked.contains(typeClass)) {
            requestClassLinked.addLast(typeClass);
            String parameterName;
            Class<?> type;
            JSONObject requestParameter;
            List<Field> fileList = this.getClassFieldList(typeClass);
            ApiParameter apiParameter;
            for (Field field : fileList) {
                if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                    if (field.isAnnotationPresent(ApiParameter.class)) {
                        apiParameter = field.getAnnotation(ApiParameter.class);
                        parameterName = field.getName();
                        type = field.getType();
                        if (isBasicType(type)) {
                            requestParameter = this.createBasicRequestParameter(type, parameterName, apiParameter);
                            requestParameters.put(requestParameter);
                        } else if (Collection.class.isAssignableFrom(type)) {
                            Class<?> subType = this.getRequestListActualType(method, typeClass, field);
                            requestParameter = this.createCollectionRequestParameter(method, subType, parameterName, apiParameter, requestClassLinked);
                            requestParameters.put(requestParameter);
                        } else if (type.isArray()) {
                            Class<?> componentType = type.getComponentType();
                            requestParameter = this.createCollectionRequestParameter(method, componentType, parameterName, apiParameter, requestClassLinked);
                            requestParameters.put(requestParameter);
                        } else if (JSONArray.class == type) {
                            requestParameter = this.createJsonArrayRequestParam(parameterName, apiParameter);
                            requestParameters.put(requestParameter);
                        } else if (JSONObject.class == type) {
                            requestParameter = this.createObjectRequestParameter(parameterName, apiParameter);
                            requestParameters.put(requestParameter);
                        } else if (Object.class == type) {
                            Class<?> subType = this.getRequestListActualType(method, typeClass, field);
                            JSONObject objectRequestParamVo = this.createObjectRequestParameter(method, subType, requestClassLinked, parameterName, apiParameter);
                            requestParameters.put(objectRequestParamVo);
                        } else {
                            JSONObject objectRequestParamVo = this.createObjectRequestParameter(method, type, requestClassLinked, parameterName, apiParameter);
                            requestParameters.put(objectRequestParamVo);
                        }
                    }
                }
            }
            requestClassLinked.removeLast();
        }
        return requestParameters;
    }

    public JSONArray getSubResponseParameters(Method method, Class<?> paramClass, LinkedList<Class<?>> responseClassLinked) {
        JSONArray responseParameters = new JSONArray();
        if (!responseClassLinked.contains(paramClass)) {
            responseClassLinked.addLast(paramClass);
            //
            String parameterName;
            Class<?> type;
            JSONObject responseParameter;
            List<Field> fieldList = this.getClassFieldList(paramClass);
            ApiParameter apiParameter;
            for (Field field : fieldList) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    if (field.isAnnotationPresent(ApiParameter.class)) {
                        apiParameter = field.getAnnotation(ApiParameter.class);
                        parameterName = field.getName();
                        type = field.getType();
                        if (isBasicType(type)) {
                            responseParameter = this.createBasicResponseParameter(type, parameterName, apiParameter);
                            responseParameters.put(responseParameter);
                        } else if (Collection.class.isAssignableFrom(type)) {
                            Class<?> subType = this.getResponseListActualType(method, paramClass, field);
                            responseParameter = this.createCollectionResponseParameter(method, subType, parameterName, apiParameter, responseClassLinked);
                            responseParameters.put(responseParameter);
                        } else if (type.isArray()) {
                            Class<?> componentType = type.getComponentType();
                            responseParameter = this.createCollectionResponseParameter(method, componentType, parameterName, apiParameter, responseClassLinked);
                            responseParameters.put(responseParameter);
                        } else if (JSONArray.class == type) {
                            responseParameter = this.createObjectResponseParameter(parameterName, apiParameter);
                            responseParameters.put(responseParameter);
                        } else if (JSONObject.class == type) {
                            responseParameter = createObjectResponseParameter(parameterName, apiParameter);
                            responseParameters.put(responseParameter);
                        } else if (Object.class == type) {
                            Class<?> subType = this.getRequestListActualType(method, paramClass, field);
                            responseParameter = this.createObjectResponseParameter(method, subType, responseClassLinked, parameterName, apiParameter);
                            responseParameters.put(responseParameter);
                        } else {
                            responseParameter = this.createObjectResponseParameter(method, type, responseClassLinked, parameterName, apiParameter);
                            responseParameters.put(responseParameter);
                        }
                    }
                }
            }
            responseClassLinked.removeLast();
        }
        return responseParameters;
    }

    private JSONObject createBasicResponseParameter(Class<?> typeClass, String name, ApiParameter apiParameter) {
        JSONObject responseParameter = new JSONObject();
        responseParameter.put("name", name);
        responseParameter.put("description", apiParameter.description());
        String type;
        if (typeClass == boolean.class || typeClass == Boolean.class) {
            type = ApiConstants.TYPE_BOOLEAN;
        } else if (typeClass == long.class || typeClass == Long.class || int.class == typeClass || typeClass == Integer.class || short.class == typeClass || typeClass == Short.class) {
            type = ApiConstants.TYPE_INTEGER;
        } else if (typeClass == double.class || typeClass == Double.class || typeClass == float.class || typeClass == Float.class || Number.class.isAssignableFrom(typeClass)) {
            type = ApiConstants.TYPE_NUMBER;
        } else if (typeClass == String.class) {
            type = ApiConstants.TYPE_STRING;
        } else if (typeClass == Date.class) {
            type = ApiConstants.TYPE_DATE;
        } else {
            type = typeClass.getSimpleName();
        }
        responseParameter.put("type", type);
        return responseParameter;
    }

    private JSONObject createBasicArrayResponseParameter(Class<?> typeClass, String name, ApiParameter apiParameter) {
        JSONObject responseParameter = new JSONObject();
        responseParameter.put("name", name);
        responseParameter.put("type", ApiConstants.TYPE_ARRAY);
        responseParameter.put("description", apiParameter.description());
        String itemType;
        if (typeClass == boolean.class || typeClass == Boolean.class) {
            itemType = ApiConstants.TYPE_BOOLEAN;
        } else if (typeClass == long.class || typeClass == Long.class || int.class == typeClass || typeClass == Integer.class || short.class == typeClass || typeClass == Short.class) {
            itemType = ApiConstants.TYPE_INTEGER;
        } else if (typeClass == double.class || typeClass == Double.class || typeClass == float.class || typeClass == Float.class || Number.class.isAssignableFrom(typeClass)) {
            itemType = ApiConstants.TYPE_NUMBER;
        } else if (typeClass == String.class) {
            itemType = ApiConstants.TYPE_STRING;
        } else if (typeClass == Date.class) {
            itemType = ApiConstants.TYPE_DATE;
        } else {
            itemType = typeClass.getSimpleName();
        }
        JSONObject items = new JSONObject();
        items.put("type", itemType);
        responseParameter.put("items", items);
        return responseParameter;
    }

    private JSONObject createObjectArrayResponseParameter(Method method, Class<?> typeClass, LinkedList<Class<?>> responseClassLinked, String name, ApiParameter apiParameter) {
        JSONObject responseParameter = new JSONObject();
        responseParameter.put("name", name);
        responseParameter.put("type", ApiConstants.TYPE_ARRAY);
        responseParameter.put("description", apiParameter.description());
        if (typeClass.isAnnotationPresent(ApiParameter.class)) {
            ApiParameter myApiParameter = typeClass.getAnnotation(ApiParameter.class);
            responseParameter.put("description", myApiParameter.description());
        }
        boolean dynamic = typeClass.equals(JSONObject.class);
        responseParameter.put("dynamic", dynamic);
        JSONObject items = new JSONObject();
        items.put("type", ApiConstants.TYPE_OBJECT);
        JSONArray childArray = this.getSubResponseParameters(method, typeClass, responseClassLinked);
        items.put("childArray", childArray);
        responseParameter.put("items", items);
        return responseParameter;
    }

    private JSONObject createCollectionResponseParameter(Method method, Class<?> typeClass, String name, ApiParameter apiParameter, LinkedList<Class<?>> responseClassLinked) {
        JSONObject responseParam;
        if (isBasicType(typeClass)) {
            responseParam = this.createBasicArrayResponseParameter(typeClass, name, apiParameter);
        } else {
            responseParam = this.createObjectArrayResponseParameter(method, typeClass, responseClassLinked, name, apiParameter);
        }
        return responseParam;
    }

    private JSONObject createObjectResponseParameter(Method method, Class<?> paramClass, LinkedList<Class<?>> responseClassLinked, String name, ApiParameter apiParameter) {
        JSONObject responseParam = new JSONObject();
        responseParam.put("name", name);
        responseParam.put("type", ApiConstants.TYPE_OBJECT);
        responseParam.put("description", apiParameter.description());
        if (paramClass.isAnnotationPresent(ApiParameter.class)) {
            ApiParameter myApiParameter = paramClass.getAnnotation(ApiParameter.class);
            responseParam.put("description", myApiParameter.description());
        }
        responseParam.put("dynamic", false);
        JSONArray childArray = this.getSubResponseParameters(method, paramClass, responseClassLinked);
        responseParam.put("childArray", childArray);
        return responseParam;
    }

    private JSONObject createObjectResponseParameter(String name, ApiParameter apiParameter) {
        JSONObject responseParameter = new JSONObject();
        responseParameter.put("name", name);
        responseParameter.put("type", ApiConstants.TYPE_OBJECT);
        responseParameter.put("description", apiParameter.description());
        responseParameter.put("dynamic", true);
        JSONArray childArray = new JSONArray();
        responseParameter.put("childArray", childArray);
        return responseParameter;
    }
}
