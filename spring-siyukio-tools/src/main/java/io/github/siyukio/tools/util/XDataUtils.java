package io.github.siyukio.tools.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.siyukio.tools.api.constants.ApiConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Provides conversion between Strings and JSON objects or JSON collections.
 * Used for deep copying and transformation between JavaTypes.
 *
 * @author Buddy
 */
public abstract class XDataUtils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DEFAULT_DATE_TIME_FORMATTER,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DEFAULT_DATE_FORMATTER,
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    static {
        // Supports LocalDateTime
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSmartSerializer());
        javaTimeModule.addDeserializer(LocalDateTime.class, new MultiFormatLocalDateTimeDeserializer());

        // Config json
        OBJECT_MAPPER.registerModule(javaTimeModule);

        // Support JSONObject and JSONArray
        OBJECT_MAPPER.registerModule(new JsonOrgModule());

        // Ignore properties with null values
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Others...
        OBJECT_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
//        OBJECT_MAPPER.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);

        OBJECT_MAPPER.getFactory().enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature());

        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        // Config xml
        XML_MAPPER.registerModule(javaTimeModule);
        XML_MAPPER.registerModule(new JsonOrgModule());

        XML_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        XML_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        XML_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    public static void checkType(Class<?> clazz, Class<?> type) {
        if (java.util.Date.class == type || java.sql.Date.class == type) {
            String error = String.format(ApiConstants.ERROR_DATE_UNSUPPORTED_FORMAT, type, clazz);
            throw new IllegalArgumentException(error);
        }
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

    public static LocalDateTime parse(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (Exception ignored) {
            }
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(text, formatter).atStartOfDay();
            } catch (Exception ignored) {
            }
        }

        // Supports timestamp
        if (text.matches("\\d+")) {
            long epoch = Long.parseLong(text);
            ZoneId zone = ZoneId.systemDefault();
            if (text.length() == 10) { // seconds
                return LocalDateTime.ofEpochSecond(epoch, 0, zone.getRules().getOffset(Instant.ofEpochSecond(epoch)));
            } else { // mills
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), zone);
            }
        }

        throw new IllegalArgumentException("Unsupported LocalDateTime format: " + text);
    }

    public static LocalDateTime toLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.systemDefault());
    }

    public static long toMills(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static String format(LocalDateTime localDateTime) {
        // Check if the time is 00:00:00
        if (localDateTime.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            return localDateTime.toLocalDate().format(DEFAULT_DATE_FORMATTER);
        }
        return localDateTime.format(DEFAULT_DATE_TIME_FORMATTER);
    }

    public static String formatMs(long timestamp) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zone);
        return format(localDateTime);
    }

    public static <T> T parseXml(String xml, Class<T> toClazz) {
        try {
            return XML_MAPPER.reader().readValue(xml, toClazz);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static JSONObject parseXml(String xml) {

        if (xml == null || xml.isEmpty()) {
            return new JSONObject();
        }
        try {
            return XML_MAPPER.reader().readValue(xml, JSONObject.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static class MultiFormatLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            String text = p.getText().trim();
            try {
                return parse(text);
            } catch (Exception ignored) {
            }

            throw new JsonParseException(p, "Unsupported LocalDateTime format: " + text);
        }
    }

    public static class LocalDateTimeSmartSerializer extends JsonSerializer<LocalDateTime> {

        @Override
        public void serialize(LocalDateTime value,
                              JsonGenerator gen,
                              SerializerProvider serializers) throws IOException {
            gen.writeString(format(value));
        }
    }
}
