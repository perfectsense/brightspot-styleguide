package com.psddev.styleguide;

import java.util.Arrays;

import com.psddev.dari.util.StringUtils;

public final class PathUtils {

    public static final String SLASH = System.getProperty("file.separator");
    public static final char SLASH_CHAR = System.getProperty("file.separator").charAt(0);

    public static String buildPath(String... pathParts) {
        return StringUtils.join(Arrays.asList(pathParts), SLASH);
    }

    public static String buildPathWithEndingSlash(String... pathParts) {
        return buildPath(pathParts) + SLASH;
    }

    public static String replaceAllWithSlash(String text, String regex) {
        return text.replaceAll(regex, getRegexSlash());
    }

    private static String getRegexSlash() {
        return "\\".equals(SLASH) ? "\\\\" : SLASH;
    }
}
