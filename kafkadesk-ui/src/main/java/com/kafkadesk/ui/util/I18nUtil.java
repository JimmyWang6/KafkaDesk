package com.kafkadesk.ui.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Internationalization utility class
 */
public class I18nUtil {
    private static final String BUNDLE_NAME = "i18n.messages";
    private static ResourceBundle bundle;
    private static Locale currentLocale;

    static {
        // Use system language by default
        setLocale(Locale.getDefault());
    }

    /**
     * Set locale
     */
    public static void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
    }

    /**
     * Set locale（By language code）
     */
    public static void setLocale(String languageTag) {
        Locale locale;
        switch (languageTag) {
            case "zh_CN":
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case "en":
                locale = Locale.ENGLISH;
                break;
            default:
                locale = Locale.ENGLISH;
        }
        setLocale(locale);
    }

    /**
     * Get current locale
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Get internationalized message
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key;
        }
    }

    /**
     * Get internationalized message（With parameters）
     */
    public static String get(String key, Object... args) {
        try {
            String message = bundle.getString(key);
            return MessageFormat.format(message, args);
        } catch (Exception e) {
            return key;
        }
    }

    /**
     * Get all supported locales
     */
    public static Locale[] getSupportedLocales() {
        return new Locale[]{
                Locale.ENGLISH,
                Locale.SIMPLIFIED_CHINESE
        };
    }

    /**
     * Get language display name
     */
    public static String getLanguageDisplayName(Locale locale) {
        if (locale.equals(Locale.SIMPLIFIED_CHINESE)) {
            return "中文";
        } else if (locale.equals(Locale.ENGLISH)) {
            return "English";
        }
        return locale.getDisplayName();
    }
}
