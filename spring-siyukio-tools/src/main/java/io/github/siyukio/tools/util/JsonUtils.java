package io.github.siyukio.tools.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Provides conversion between Strings and JSON objects or JSON collections.
 * Used for deep copying and transformation between JavaTypes.
 *
 * @author Buddy
 */
public abstract class JsonUtils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setDateFormat(DateUtils.DEFAULT_DATE_FORMAT);
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        OBJECT_MAPPER.registerModule(javaTimeModule);
        //
        OBJECT_MAPPER.registerModule(new JsonOrgModule());
//        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        OBJECT_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
//        OBJECT_MAPPER.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        OBJECT_MAPPER.getFactory().enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature());

        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }


    public static <T> T copy(Object from, Class<T> toClazz) {
        if (from == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(from, toClazz);
    }

    public static <T> T copy(Object from, JavaType javaType) {
        if (from == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(from, javaType);
    }

    public static <T> T copy(Object from, TypeReference<T> toValueTypeRef) {
        if (from == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(from, toValueTypeRef);
    }

    public static <O, I, T> T copy(Object from, final Class<O> outerClass, final Class<I> innerClass) {
        if (from == null) {
            return null;
        }
        JavaType type = OBJECT_MAPPER.getTypeFactory().constructParametricType(outerClass, innerClass);
        return OBJECT_MAPPER.convertValue(from, type);
    }

    public static <O, F, S, T> T copy(Object from, final Class<O> outerClass, final Class<F> firstClass, final Class<S> secondClass) {
        if (from == null) {
            return null;
        }
        JavaType type = OBJECT_MAPPER.getTypeFactory().constructParametricType(outerClass, firstClass, secondClass);
        return OBJECT_MAPPER.convertValue(from, type);
    }

    /**
     * merge source properties from one object into target
     *
     * @param source
     * @param target
     * @param <T>
     * @return
     */
    public static <T> T mergeNotNul(Object source, T target) {
        if (source == null) {
            return target;
        }
        JSONObject sourceObject = copy(source, JSONObject.class);
        JSONObject targetObject = copy(target, JSONObject.class);
        Object value;
        for (String key : sourceObject.keySet()) {
            value = sourceObject.opt(key);
            if (value != null) {
                targetObject.put(key, value);
            }
        }

        @SuppressWarnings("unchecked")
        Class<T> outerClass = (Class<T>) target.getClass();
        return copy(targetObject, outerClass);
    }

    public static <T> T parse(String json, Class<T> toClazz) {
        try {
            return OBJECT_MAPPER.reader().readValue(json, toClazz);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <O, I, T> T parse(String json, final Class<O> outerClass, final Class<I> innerClass) {
        JavaType type = OBJECT_MAPPER.getTypeFactory().constructParametricType(outerClass, innerClass);
        try {
            return OBJECT_MAPPER.readerFor(type).readValue(json);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <O, F, S, T> T parse(String json, final Class<O> outerClass, final Class<F> firstClass, final Class<S> secondClass) {
        JavaType type = OBJECT_MAPPER.getTypeFactory().constructParametricType(outerClass, firstClass, secondClass);
        try {
            return OBJECT_MAPPER.readerFor(type).readValue(json);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static JSONObject parseObject(String json) {

        if (json == null || json.isEmpty()) {
            return new JSONObject();
        }
        try {
            return OBJECT_MAPPER.reader().readValue(json, JSONObject.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static JSONArray parseArray(String json) {
        if (json == null || json.isEmpty()) {
            return new JSONArray();
        }
        try {
            return OBJECT_MAPPER.reader().readValue(json, JSONArray.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String toJSONString(Object from) {
        if (from == null) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writer().writeValueAsString(from);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String toPrettyJSONString(Object from) {
        if (from == null) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writer().withDefaultPrettyPrinter().writeValueAsString(from);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

}
