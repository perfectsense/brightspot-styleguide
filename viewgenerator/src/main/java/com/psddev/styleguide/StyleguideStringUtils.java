package com.psddev.styleguide;

import com.psddev.dari.util.StringUtils;

class StyleguideStringUtils {

    /**
     * Hack to fix a StringIndexOutOfBoundsException in com.psddev.dari.util.StringUtils#toPascalCase
     */
    public static String toPascalCase(String string) {
        if (string != null && !string.isEmpty()) {
            char first = string.charAt(0);
            if (" -_.$".indexOf(first) > -1) {
                string = string.substring(1);
            }
        }
        return StringUtils.toPascalCase(string);
    }
}
