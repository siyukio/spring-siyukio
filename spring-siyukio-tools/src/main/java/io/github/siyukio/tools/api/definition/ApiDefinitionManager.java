package io.github.siyukio.tools.api.definition;

import io.github.siyukio.tools.api.ApiRequest;
import io.github.siyukio.tools.api.annotation.ApiController;
import io.github.siyukio.tools.api.annotation.ApiMapping;
import io.github.siyukio.tools.api.annotation.ApiParameter;
import io.github.siyukio.tools.api.annotation.Example;
import io.github.siyukio.tools.api.token.Token;
import io.github.siyukio.tools.util.XDataUtils;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.lang.reflect.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author Buddy
 */
@Slf4j
public final class ApiDefinitionManager {

    private final Map<String, ApiDefinition> apiDefinitionMap = new HashMap<>();

    private final Set<Class<?>> alternativeSet = new HashSet<>();

    public static boolean isBasicType(Class<?> type) {
        return type.isPrimitive() || type == String.class || type == LocalDateTime.class
                || Number.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)
                || type.isEnum();
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
        // Create request parameter
        Map<String, ApiRequestParameter> requestBodyChildMap = new LinkedHashMap<>();
        LinkedList<Class<?>> requestClassLinked = new LinkedList<>();
        List<ApiRequestParameter> subRequestParameters;
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            subRequestParameters = this.getSubRequestParameters(method, parameter.getType(), requestClassLinked);
            for (ApiRequestParameter subRequestParameter : subRequestParameters) {
                if (!requestBodyChildMap.containsKey(subRequestParameter.name())) {
                    requestBodyChildMap.put(subRequestParameter.name(), subRequestParameter);
                }
            }
        }

        ApiSchema.ApiSchemaBuilder requestBodySchemaBuilder = ApiSchema.builder()
                .type(ApiSchema.Type.OBJECT);
        if (requestBodyChildMap.isEmpty()) {
            requestBodySchemaBuilder.properties(Map.of());
        } else {
            Map<String, ApiSchema> properties = new LinkedHashMap<>();
            List<String> requiredList = new ArrayList<>();
            for (Map.Entry<String, ApiRequestParameter> entry : requestBodyChildMap.entrySet()) {
                properties.put(entry.getKey(), entry.getValue().schema());
                if (entry.getValue().required()) {
                    requiredList.add(entry.getKey());
                }
            }
            requestBodySchemaBuilder.properties(properties);
            if (!requiredList.isEmpty()) {
                requestBodySchemaBuilder.required(requiredList);
            }
        }

        ApiRequestParameter requestBodyParameter = ApiRequestParameter.builder()
                .name("requestBody").required(true)
                .schema(requestBodySchemaBuilder.build())
                .properties(new ArrayList<>(requestBodyChildMap.values()))
                .build();


        // Create response parameter
        Class<?> returnValueType = this.getRealReturnType(method);
        if (returnValueType != void.class && returnValueType != Void.class && returnValueType != String.class) {
            if (isBasicType(returnValueType) || returnValueType.isArray() || Collection.class.isAssignableFrom(returnValueType) || returnValueType.getPackageName().startsWith("java.")) {
                throw new IllegalArgumentException(type.getName() + "." + method.getName() + " unsupported returnValueType:" + returnValueType.getSimpleName());
            }
        }

        LinkedList<Class<?>> responseClassLinkedList = new LinkedList<>();
        List<ApiResponseParameter> responseParameters;
        if (returnValueType == void.class || returnValueType == Void.class) {
            responseParameters = List.of();
        } else {
            responseParameters = this.getSubResponseParameters(method, returnValueType, responseClassLinkedList);
        }

        ApiSchema.ApiSchemaBuilder responseBodySchemaBuilder = ApiSchema.builder()
                .type(ApiSchema.Type.OBJECT);
        if (responseParameters.isEmpty()) {
            responseBodySchemaBuilder.properties(Map.of());
        } else {
            Map<String, ApiSchema> properties = new LinkedHashMap<>();
            for (ApiResponseParameter apiResponseParameter : responseParameters) {
                properties.put(apiResponseParameter.name(), apiResponseParameter.schema());
            }
            responseBodySchemaBuilder.properties(properties);
        }

        ApiResponseParameter responseBodyParameter = ApiResponseParameter.builder()
                .name("responseBody")
                .schema(responseBodySchemaBuilder.build())
                .properties(responseParameters)
                .build();


//        responseParameters.add(ApiException.getErrorResponseParameter());

        String summary = apiMapping.summary();
        if (!StringUtils.hasText(summary)) {
            summary = method.getName();
        }

        // Determine the roles
        String[] roles = apiMapping.roles();
        if (roles.length == 0) {
            roles = apiController.roles();
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
                .roles(List.of(roles))
                .returnType(method.getReturnType())
                .realReturnType(returnValueType)
                .requestBodyParameter(requestBodyParameter)
                .responseBodyParameter(responseBodyParameter);
        return builder.build();
    }

    private ApiRequestParameter createBasicArrayRequestParameter(Class<?> typeClass, String name, ApiParameter apiParameter) {
        ApiSchema.ApiSchemaBuilder itemsSchemaBuilder = ApiSchema.builder();
        if (typeClass == boolean.class || typeClass == Boolean.class) {
            itemsSchemaBuilder.type(ApiSchema.Type.BOOLEAN);
        } else if (typeClass == long.class || typeClass == Long.class || int.class == typeClass || typeClass == Integer.class || short.class == typeClass || typeClass == Short.class) {
            itemsSchemaBuilder.type(ApiSchema.Type.INTEGER);
            this.defineNumber(itemsSchemaBuilder, apiParameter);
        } else if (typeClass == double.class || typeClass == Double.class || typeClass == float.class || typeClass == Float.class || Number.class.isAssignableFrom(typeClass)) {
            itemsSchemaBuilder.type(ApiSchema.Type.NUMBER);
            this.defineNumber(itemsSchemaBuilder, apiParameter);
        } else if (typeClass == String.class) {
            itemsSchemaBuilder.type(ApiSchema.Type.STRING);
            this.defineString(itemsSchemaBuilder, apiParameter, apiParameter.pattern());
        } else if (typeClass == LocalDateTime.class) {
            itemsSchemaBuilder.type(ApiSchema.Type.STRING);
            this.defineDate(itemsSchemaBuilder);
        } else {
            throw new IllegalArgumentException(String.format("Schema type error: %s -> %s", name, typeClass.getSimpleName()));
        }

        ApiSchema itemsSchema = itemsSchemaBuilder.build();

        ApiRequestParameter items = ApiRequestParameter.builder()
                .name("items")
                .schema(itemsSchema)
                .build();

        ApiSchema.ApiSchemaBuilder schemaBuilder = ApiSchema.builder()
                .type(ApiSchema.Type.ARRAY)
                .items(itemsSchema);

        if (StringUtils.hasText(apiParameter.description())) {
            schemaBuilder.description(apiParameter.description());
        }

        this.defineArray(schemaBuilder, apiParameter);

        return ApiRequestParameter.builder()
                .name(name)
                .required(apiParameter.required())
                .error(apiParameter.error())
                .schema(schemaBuilder.build())
                .items(items)
                .build();
    }

    private ApiRequestParameter createObjectArrayRequestParameter(Method method, Class<?> typeClass, LinkedList<Class<?>> requestClassLinked, String name, ApiParameter apiParameter) {
        ApiSchema.ApiSchemaBuilder itemsSchemaBuilder = ApiSchema.builder()
                .type(ApiSchema.Type.OBJECT);

        List<ApiRequestParameter> subRequestParameters = this.getSubRequestParameters(method, typeClass, requestClassLinked);
        if (subRequestParameters.isEmpty()) {
            itemsSchemaBuilder.properties(Map.of());
            itemsSchemaBuilder.additionalProperties(true);
        } else {
            Map<String, ApiSchema> properties = new LinkedHashMap<>();
            for (ApiRequestParameter subRequestParameter : subRequestParameters) {
                properties.put(subRequestParameter.name(), subRequestParameter.schema());
            }
            itemsSchemaBuilder.properties(properties);
        }

        ApiSchema itemsSchema = itemsSchemaBuilder.build();

        ApiRequestParameter items = ApiRequestParameter.builder()
                .name("items")
                .schema(itemsSchema)
                .properties(subRequestParameters)
                .build();

        ApiSchema.ApiSchemaBuilder schemaBuilder = ApiSchema.builder()
                .type(ApiSchema.Type.ARRAY)
                .items(itemsSchema);

        if (StringUtils.hasText(apiParameter.description())) {
            schemaBuilder.description(apiParameter.description());
        }

        if (typeClass.isAnnotationPresent(ApiParameter.class)) {
            ApiParameter myApiParameter = typeClass.getAnnotation(ApiParameter.class);
            schemaBuilder.description(myApiParameter.description());
        }

        this.defineArray(schemaBuilder, apiParameter);

        return ApiRequestParameter.builder()
                .name(name)
                .required(apiParameter.required())
                .error(apiParameter.error())
                .schema(schemaBuilder.build())
                .items(items)
                .build();
    }

    private void defineNumber(ApiSchema.ApiSchemaBuilder schemaBuilder, ApiParameter apiParameter) {
        if (apiParameter.minimum() > Long.MIN_VALUE) {
            schemaBuilder.minimum(apiParameter.minimum());
        }
        if (apiParameter.maximum() < Long.MAX_VALUE) {
            schemaBuilder.maximum(apiParameter.maximum());
        }
    }

    private void defineString(ApiSchema.ApiSchemaBuilder schemaBuilder, ApiParameter apiParameter, String pattern) {
        if (StringUtils.hasText(pattern)) {
            schemaBuilder.pattern(pattern);
        } else {
            //common String
            if (apiParameter.maxLength() > 0) {
                schemaBuilder.maxLength(apiParameter.maxLength());
            }

            if (apiParameter.minLength() > 0) {
                schemaBuilder.minLength(apiParameter.minLength());
            }
        }
        if (apiParameter.password()) {
            schemaBuilder.format("password");
        }
    }

    private void defineDate(ApiSchema.ApiSchemaBuilder schemaBuilder) {
        schemaBuilder.format("date-time");
        schemaBuilder.example("2025-01-01 00:00:00");
    }

    private List<ApiSchema.Example> createExamples(Example[] examples) {
        List<ApiSchema.Example> exampleList = new ArrayList<>();
        for (Example example : examples) {
            ApiSchema.Example.ExampleBuilder exampleBuilder = ApiSchema.Example.builder()
                    .value(example.value());
            if (StringUtils.hasText(example.summary())) {
                exampleBuilder.summary(example.summary());
            }
            exampleList.add(exampleBuilder.build());
        }
        return exampleList;
    }

    private void defineArray(ApiSchema.ApiSchemaBuilder schemaBuilder, ApiParameter apiParameter) {
        if (apiParameter.examples().length > 0) {
            List<ApiSchema.Example> exampleList = this.createExamples(apiParameter.examples());
            schemaBuilder.examples(exampleList);
        }

        if (apiParameter.maxItems() > 0) {
            schemaBuilder.maxItems(apiParameter.maxItems());
        }
        if (apiParameter.minItems() > 0) {
            schemaBuilder.minItems(apiParameter.minItems());
        }
    }

    private ApiRequestParameter createBasicRequestParameter(Class<?> typeClass, String name, ApiParameter apiParameter) {
        ApiSchema.ApiSchemaBuilder schemaBuilder = ApiSchema.builder();
        if (StringUtils.hasText(apiParameter.description())) {
            schemaBuilder.description(apiParameter.description());
        }

        ApiSchema.Type type;
        if (typeClass == boolean.class || typeClass == Boolean.class) {
            type = ApiSchema.Type.BOOLEAN;
        } else if (typeClass == long.class || typeClass == Long.class || int.class == typeClass || typeClass == Integer.class || short.class == typeClass || typeClass == Short.class) {
            type = ApiSchema.Type.INTEGER;
            this.defineNumber(schemaBuilder, apiParameter);
        } else if (typeClass == double.class || typeClass == Double.class || typeClass == float.class || typeClass == Float.class || Number.class.isAssignableFrom(typeClass)) {
            type = ApiSchema.Type.NUMBER;
            this.defineNumber(schemaBuilder, apiParameter);
        } else if (typeClass == String.class) {
            type = ApiSchema.Type.STRING;
            this.defineString(schemaBuilder, apiParameter, apiParameter.pattern());
        } else if (typeClass.isEnum()) {
            String pattern = XDataUtils.getEnumPattern(typeClass);
            type = ApiSchema.Type.STRING;
            this.defineString(schemaBuilder, apiParameter, pattern);
        } else if (typeClass == LocalDateTime.class) {
            type = ApiSchema.Type.STRING;
            this.defineDate(schemaBuilder);
        } else {
            throw new IllegalArgumentException(String.format("Schema type error: %s -> %s", name, typeClass.getSimpleName()));
        }

        schemaBuilder.type(type);
        if (!apiParameter.required()) {
            if (StringUtils.hasText(apiParameter.defaultValue())) {
                Object defaultValue = this.getDefaultValue(type, apiParameter.defaultValue());
                schemaBuilder.defaultValue(defaultValue);
            }
        }

        return ApiRequestParameter.builder()
                .name(name)
                .required(apiParameter.required())
                .error(apiParameter.error())
                .schema(schemaBuilder.build()).build();
    }

    private Object getDefaultValue(ApiSchema.Type type, String defaultValue) {
        Object result = null;
        if (!defaultValue.isEmpty()) {
            switch (type) {
                case ApiSchema.Type.BOOLEAN:
                    result = defaultValue.equalsIgnoreCase("true");
                    break;
                case ApiSchema.Type.INTEGER:
                    result = Long.valueOf(defaultValue).intValue();
                    break;
                case ApiSchema.Type.NUMBER:
                    result = Double.parseDouble(defaultValue);
                    break;
                case ApiSchema.Type.STRING:
                    result = defaultValue;
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    private ApiRequestParameter createCollectionRequestParameter(Method method, Class<?> typeClass, String name, ApiParameter apiParameter, LinkedList<Class<?>> requestClassLinked) {
        if (isBasicType(typeClass)) {
            return this.createBasicArrayRequestParameter(typeClass, name, apiParameter);
        } else {
            return this.createObjectArrayRequestParameter(method, typeClass, requestClassLinked, name, apiParameter);
        }
    }

    private ApiRequestParameter createObjectRequestParameter(Method method, Class<?> typeClass, LinkedList<Class<?>> requestClassLinked, String name, ApiParameter apiParameter) {
        ApiSchema.ApiSchemaBuilder schemaBuilder = ApiSchema.builder()
                .type(ApiSchema.Type.OBJECT);
        if (StringUtils.hasText(apiParameter.description())) {
            schemaBuilder.description(apiParameter.description());
        }

        if (typeClass.isAnnotationPresent(ApiParameter.class)) {
            ApiParameter myApiParameter = typeClass.getAnnotation(ApiParameter.class);
            schemaBuilder.description(myApiParameter.description());
        }

        List<ApiRequestParameter> subRequestParameters = this.getSubRequestParameters(method, typeClass, requestClassLinked);
        if (subRequestParameters.isEmpty()) {
            schemaBuilder.properties(Map.of());
            schemaBuilder.additionalProperties(true);
        } else {
            Map<String, ApiSchema> map = new LinkedHashMap<>();
            List<String> requiredList = new ArrayList<>();
            for (ApiRequestParameter subRequestParameter : subRequestParameters) {
                map.put(subRequestParameter.name(), subRequestParameter.schema());
                if (subRequestParameter.required()) {
                    requiredList.add(subRequestParameter.name());
                }
            }
            schemaBuilder.properties(map);
            if (!requiredList.isEmpty()) {
                schemaBuilder.required(requiredList);
            }
        }

        if (apiParameter.examples().length > 0) {
            List<ApiSchema.Example> exampleList = this.createExamples(apiParameter.examples());
            schemaBuilder.examples(exampleList);
        }

        return ApiRequestParameter.builder()
                .name(name)
                .required(apiParameter.required())
                .error(apiParameter.error())
                .schema(schemaBuilder.build())
                .properties(subRequestParameters)
                .build();
    }

    private Class<?> getRequestActualType(Method method, Class<?> paramClass, RecordComponent recordComponent) {
        Type generictype = recordComponent.getGenericType();
        if (generictype instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            Type actualType = actualTypeArguments[0];
            if (!(actualType instanceof TypeVariable)) {
                //not T
                return (Class<?>) actualType;
            }
        }
        //Type = T
        Class<?> subType = null;
        //find in method
        Type[] paramTypes = method.getGenericParameterTypes();
        for (Type paramType : paramTypes) {
            if (paramType instanceof ParameterizedType parameterizedType) {
                if (parameterizedType.getRawType().equals(paramClass)) {
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    subType = (Class<?>) actualTypeArguments[0];
                    return subType;
                }
            }
        }

        //find in class
        Class<?> typeClass = paramClass;
        while (typeClass != Object.class) {
            Type type = typeClass.getGenericSuperclass();
            if (type instanceof ParameterizedType parameterizedType) {
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                subType = (Class<?>) actualTypeArguments[0];
                break;
            } else {
                typeClass = typeClass.getSuperclass();
            }
        }
        if (subType == null) {
            subType = Object.class;
        }
        return subType;
    }

    private Class<?> getResponseActualType(Method method, Class<?> paramClass, RecordComponent recordComponent) {
        Type generictype = recordComponent.getGenericType();
        if (generictype instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            Type listType = actualTypeArguments[0];
            if (!(listType instanceof TypeVariable)) {
                //not T
                return (Class<?>) listType;
            }
        }

        //Type = T
        Class<?> subType = null;
        //find in method
        generictype = method.getGenericReturnType();
        if (generictype instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            subType = (Class<?>) actualTypeArguments[0];
            return subType;
        }

        //find in class
        Class<?> typeClass = paramClass;
        while (typeClass != Object.class) {
            Type type = typeClass.getGenericSuperclass();
            if (type instanceof ParameterizedType parameterizedType) {
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                subType = (Class<?>) actualTypeArguments[0];
                break;
            } else {
                typeClass = typeClass.getSuperclass();
            }
        }
        if (subType == null) {
            subType = Object.class;
        }
        return subType;
    }

    public List<ApiRequestParameter> getSubRequestParameters(Method method, Class<?> typeClass, LinkedList<Class<?>> requestClassLinked) {
        List<ApiRequestParameter> requestParameters = new ArrayList<>();
        if (typeClass == String.class || typeClass == ApiRequest.class || typeClass == Token.class || typeClass == McpSyncServerExchange.class) {
            return requestParameters;
        }
        //
        if (!requestClassLinked.contains(typeClass)) {
            requestClassLinked.addLast(typeClass);
            String parameterName;
            Class<?> type;
            ApiRequestParameter requestParameter;
            RecordComponent[] recordComponents = typeClass.getRecordComponents();
            if (recordComponents == null) {
                return requestParameters;
            }
            ApiParameter apiParameter;
            RecordComponent recordComponent;
            for (RecordComponent component : recordComponents) {
                recordComponent = component;
                if (recordComponent.isAnnotationPresent(ApiParameter.class)) {
                    apiParameter = recordComponent.getAnnotation(ApiParameter.class);
                    parameterName = recordComponent.getName();
                    type = recordComponent.getType();
                    XDataUtils.checkType(typeClass, type);
                    if (isBasicType(type)) {
                        requestParameter = this.createBasicRequestParameter(type, parameterName, apiParameter);
                    } else if (Iterable.class.isAssignableFrom(type)) {
                        Class<?> subType = this.getRequestActualType(method, typeClass, recordComponent);
                        requestParameter = this.createCollectionRequestParameter(method, subType, parameterName, apiParameter, requestClassLinked);
                    } else if (type.isArray()) {
                        Class<?> componentType = type.getComponentType();
                        requestParameter = this.createCollectionRequestParameter(method, componentType, parameterName, apiParameter, requestClassLinked);
                    } else if (Object.class == type) {
                        // T
                        Class<?> subType = this.getRequestActualType(method, typeClass, recordComponent);
                        requestParameter = this.createObjectRequestParameter(method, subType, requestClassLinked, parameterName, apiParameter);
                    } else {
                        requestParameter = this.createObjectRequestParameter(method, type, requestClassLinked, parameterName, apiParameter);
                    }
                    requestParameters.add(requestParameter);
                }
            }
            requestClassLinked.removeLast();
        }
        return requestParameters;
    }

    public List<ApiResponseParameter> getSubResponseParameters(Method method, Class<?> paramClass, LinkedList<Class<?>> responseClassLinked) {
        List<ApiResponseParameter> responseParameters = new ArrayList<>();
        if (!responseClassLinked.contains(paramClass)) {
            responseClassLinked.addLast(paramClass);
            //
            String parameterName;
            Class<?> type;
            ApiResponseParameter responseParameter;
            RecordComponent[] recordComponents = paramClass.getRecordComponents();
            if (recordComponents == null) {
                return responseParameters;
            }
            ApiParameter apiParameter;
            RecordComponent recordComponent;
            for (RecordComponent component : recordComponents) {
                recordComponent = component;
                if (recordComponent.isAnnotationPresent(ApiParameter.class)) {
                    apiParameter = recordComponent.getAnnotation(ApiParameter.class);
                    parameterName = recordComponent.getName();
                    type = recordComponent.getType();
                    XDataUtils.checkType(paramClass, type);
                    if (isBasicType(type)) {
                        responseParameter = this.createBasicResponseParameter(type, parameterName, apiParameter);
                    } else if (Iterable.class.isAssignableFrom(type)) {
                        Class<?> subType = this.getResponseActualType(method, paramClass, recordComponent);
                        responseParameter = this.createCollectionResponseParameter(method, subType, parameterName, apiParameter, responseClassLinked);
                    } else if (type.isArray()) {
                        Class<?> componentType = type.getComponentType();
                        responseParameter = this.createCollectionResponseParameter(method, componentType, parameterName, apiParameter, responseClassLinked);
                    } else if (Object.class == type) {
                        Class<?> subType = this.getResponseActualType(method, paramClass, recordComponent);
                        responseParameter = this.createObjectResponseParameter(method, subType, responseClassLinked, parameterName, apiParameter);
                    } else {
                        responseParameter = this.createObjectResponseParameter(method, type, responseClassLinked, parameterName, apiParameter);
                    }
                    responseParameters.add(responseParameter);
                }
            }
            responseClassLinked.removeLast();
        }
        return responseParameters;
    }

    private ApiResponseParameter createBasicResponseParameter(Class<?> typeClass, String name, ApiParameter apiParameter) {
        ApiSchema.ApiSchemaBuilder schemaBuilder = ApiSchema.builder();
        if (StringUtils.hasText(apiParameter.description())) {
            schemaBuilder.description(apiParameter.description());
        }

        if (typeClass == boolean.class || typeClass == Boolean.class) {
            schemaBuilder.type(ApiSchema.Type.BOOLEAN);
        } else if (typeClass == long.class || typeClass == Long.class || int.class == typeClass || typeClass == Integer.class || short.class == typeClass || typeClass == Short.class) {
            schemaBuilder.type(ApiSchema.Type.INTEGER);
        } else if (typeClass == double.class || typeClass == Double.class || typeClass == float.class || typeClass == Float.class || Number.class.isAssignableFrom(typeClass)) {
            schemaBuilder.type(ApiSchema.Type.NUMBER);
        } else if (typeClass == String.class || typeClass.isEnum()) {
            schemaBuilder.type(ApiSchema.Type.STRING);
        } else if (typeClass == LocalDateTime.class) {
            schemaBuilder.type(ApiSchema.Type.STRING);
        } else {
            throw new IllegalArgumentException(String.format("Schema type error: %s -> %s", name, typeClass.getSimpleName()));
        }

        return ApiResponseParameter.builder()
                .name(name)
                .schema(schemaBuilder.build())
                .build();
    }

    private ApiResponseParameter createBasicArrayResponseParameter(Class<?> typeClass, String name, ApiParameter apiParameter) {
        ApiSchema.ApiSchemaBuilder itemsSchemaBuilder = ApiSchema.builder();

        if (typeClass == boolean.class || typeClass == Boolean.class) {
            itemsSchemaBuilder.type(ApiSchema.Type.BOOLEAN);
        } else if (typeClass == long.class || typeClass == Long.class || int.class == typeClass || typeClass == Integer.class || short.class == typeClass || typeClass == Short.class) {
            itemsSchemaBuilder.type(ApiSchema.Type.INTEGER);
        } else if (typeClass == double.class || typeClass == Double.class || typeClass == float.class || typeClass == Float.class || Number.class.isAssignableFrom(typeClass)) {
            itemsSchemaBuilder.type(ApiSchema.Type.NUMBER);
        } else if (typeClass == String.class) {
            itemsSchemaBuilder.type(ApiSchema.Type.STRING);
        } else if (typeClass == LocalDateTime.class) {
            itemsSchemaBuilder.type(ApiSchema.Type.STRING);
        } else {
            throw new IllegalArgumentException(String.format("Schema type error: %s -> %s", name, typeClass.getSimpleName()));
        }

        ApiSchema itemsSchema = itemsSchemaBuilder.build();

        ApiResponseParameter items = ApiResponseParameter.builder()
                .name("items")
                .schema(itemsSchema)
                .build();

        ApiSchema.ApiSchemaBuilder schemaBuilder = ApiSchema.builder()
                .type(ApiSchema.Type.ARRAY)
                .items(itemsSchema);
        if (StringUtils.hasText(apiParameter.description())) {
            schemaBuilder.description(apiParameter.description());
        }

        return ApiResponseParameter.builder()
                .name(name)
                .schema(schemaBuilder.build())
                .items(items)
                .build();
    }

    private ApiResponseParameter createObjectArrayResponseParameter(Method method, Class<?> typeClass, LinkedList<Class<?>> responseClassLinked, String name, ApiParameter apiParameter) {
        ApiSchema.ApiSchemaBuilder itemsSchemaBuilder = ApiSchema.builder()
                .type(ApiSchema.Type.OBJECT);

        List<ApiResponseParameter> subResponseParameters = this.getSubResponseParameters(method, typeClass, responseClassLinked);
        if (subResponseParameters.isEmpty()) {
            itemsSchemaBuilder.properties(Map.of());
            itemsSchemaBuilder.additionalProperties(true);
        } else {
            Map<String, ApiSchema> map = new LinkedHashMap<>();
            for (ApiResponseParameter apiResponseParameter : subResponseParameters) {
                map.put(apiResponseParameter.name(), apiResponseParameter.schema());
            }
            itemsSchemaBuilder.properties(map);
        }

        ApiSchema itemsSchema = itemsSchemaBuilder.build();

        ApiResponseParameter items = ApiResponseParameter.builder()
                .name("items")
                .schema(itemsSchema)
                .properties(subResponseParameters)
                .build();

        ApiSchema.ApiSchemaBuilder schemaBuilder = ApiSchema.builder()
                .type(ApiSchema.Type.ARRAY)
                .items(itemsSchema);
        if (StringUtils.hasText(apiParameter.description())) {
            schemaBuilder.description(apiParameter.description());
        }

        if (typeClass.isAnnotationPresent(ApiParameter.class)) {
            ApiParameter myApiParameter = typeClass.getAnnotation(ApiParameter.class);
            schemaBuilder.description(myApiParameter.description());
        }

        return ApiResponseParameter.builder()
                .name(name)
                .schema(schemaBuilder.build())
                .items(items)
                .build();
    }

    private ApiResponseParameter createCollectionResponseParameter(Method method, Class<?> typeClass, String name, ApiParameter apiParameter, LinkedList<Class<?>> responseClassLinked) {
        if (isBasicType(typeClass)) {
            return this.createBasicArrayResponseParameter(typeClass, name, apiParameter);
        } else {
            return this.createObjectArrayResponseParameter(method, typeClass, responseClassLinked, name, apiParameter);
        }
    }

    private ApiResponseParameter createObjectResponseParameter(Method method, Class<?> paramClass, LinkedList<Class<?>> responseClassLinked, String name, ApiParameter apiParameter) {
        ApiSchema.ApiSchemaBuilder schemaBuilder = ApiSchema.builder()
                .type(ApiSchema.Type.OBJECT);
        if (StringUtils.hasText(apiParameter.description())) {
            schemaBuilder.description(apiParameter.description());
        }

        List<ApiResponseParameter> subResponseParameters = this.getSubResponseParameters(method, paramClass, responseClassLinked);
        if (subResponseParameters.isEmpty()) {
            schemaBuilder.properties(Map.of());
            schemaBuilder.additionalProperties(true);
        } else {
            Map<String, ApiSchema> map = new LinkedHashMap<>();
            for (ApiResponseParameter apiResponseParameter : subResponseParameters) {
                map.put(apiResponseParameter.name(), apiResponseParameter.schema());
            }
            schemaBuilder.properties(map);
        }

        if (paramClass.isAnnotationPresent(ApiParameter.class)) {
            ApiParameter myApiParameter = paramClass.getAnnotation(ApiParameter.class);
            schemaBuilder.description(myApiParameter.description());
        }

        return ApiResponseParameter.builder()
                .name(name)
                .schema(schemaBuilder.build())
                .properties(subResponseParameters)
                .build();
    }
}
