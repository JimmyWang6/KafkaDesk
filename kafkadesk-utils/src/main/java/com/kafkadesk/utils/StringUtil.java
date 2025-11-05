package com.kafkadesk.utils;

/**
 * String utility class
 */
public class StringUtil {

    /**
     * Check if string is empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if string is not empty
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Return default value if string is empty
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }

    /**
     * Truncate string
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * Capitalize first letter
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Safe trim
     */
    public static String safeTrim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * Check if string contains Chinese
     */
    public static boolean containsChinese(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return str.matches(".*[\\u4e00-\\u9fa5]+.*");
    }

    /**
     * Convert byte array to hex string
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Repeat string
     */
    public static String repeat(String str, int count) {
        if (str == null || count <= 0) {
            return "";
        }
        return str.repeat(count);
    }
}
