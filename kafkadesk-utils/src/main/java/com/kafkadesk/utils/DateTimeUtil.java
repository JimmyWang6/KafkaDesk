package com.kafkadesk.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Date and time utility class
 */
public class DateTimeUtil {
    private static final DateTimeFormatter DEFAULT_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static final DateTimeFormatter ISO_FORMATTER = 
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Format LocalDateTime
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DEFAULT_FORMATTER);
    }

    /**
     * Format LocalDateTime (Custom format)
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Convert timestamp to LocalDateTime
     */
    public static LocalDateTime fromTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
    }

    /**
     * Convert LocalDateTime to timestamp
     */
    public static long toTimestamp(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * Format timestamp
     */
    public static String formatTimestamp(long timestamp) {
        return format(fromTimestamp(timestamp));
    }

    /**
     * Parse string to LocalDateTime
     */
    public static LocalDateTime parse(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DEFAULT_FORMATTER);
    }

    /**
     * Get current time string
     */
    public static String now() {
        return format(LocalDateTime.now());
    }
}
