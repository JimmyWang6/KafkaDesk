package com.kafkadesk.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * JSON utility class
 */
public class JsonUtil {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Convert object to JSON string
     */
    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert object to JSON", e);
            return null;
        }
    }

    /**
     * Convert JSON string to object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON to object", e);
            return null;
        }
    }

    /**
     * Read JSON from input stream
     */
    public static <T> T fromJson(InputStream inputStream, Class<T> clazz) {
        try {
            return mapper.readValue(inputStream, clazz);
        } catch (IOException e) {
            logger.error("Failed to read JSON from input stream", e);
            return null;
        }
    }

    /**
     * Write JSON to output stream
     */
    public static void writeJson(OutputStream outputStream, Object obj) {
        try {
            mapper.writeValue(outputStream, obj);
        } catch (IOException e) {
            logger.error("Failed to write JSON to output stream", e);
        }
    }

    /**
     * Prettify JSON string
     */
    public static String prettify(String json) {
        try {
            Object obj = mapper.readValue(json, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Failed to prettify JSON", e);
            return json;
        }
    }

    /**
     * Validate if JSON string is valid
     */
    public static boolean isValidJson(String json) {
        try {
            mapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
