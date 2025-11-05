package com.kafkadesk.ui.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 国际化工具类
 */
public class I18nUtil {
    private static final String BUNDLE_NAME = "i18n.messages";
    private static ResourceBundle bundle;
    private static Locale currentLocale;

    static {
        // 默认使用系统语言
        setLocale(Locale.getDefault());
    }

    /**
     * 设置语言
     */
    public static void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
    }

    /**
     * 设置语言（根据语言代码）
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
     * 获取当前语言
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * 获取国际化消息
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key;
        }
    }

    /**
     * 获取国际化消息（带参数）
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
     * 获取所有支持的语言
     */
    public static Locale[] getSupportedLocales() {
        return new Locale[]{
                Locale.ENGLISH,
                Locale.SIMPLIFIED_CHINESE
        };
    }

    /**
     * 获取语言显示名称
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
