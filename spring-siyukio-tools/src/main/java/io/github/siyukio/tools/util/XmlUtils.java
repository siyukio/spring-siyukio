package io.github.siyukio.tools.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Provides conversion from XML to JSON objects.
 *
 * @author Buddy
 */
@Slf4j
public abstract class XmlUtils {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    static {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

        XML_MAPPER.setDateFormat(DateUtils.DEFAULT_DATE_FORMAT);
        XML_MAPPER.registerModule(javaTimeModule);
        XML_MAPPER.registerModule(new JsonOrgModule());

        XML_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        XML_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        XML_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
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
}
