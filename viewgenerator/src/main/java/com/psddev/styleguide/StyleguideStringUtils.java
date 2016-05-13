package com.psddev.styleguide;

class StyleguideStringUtils {

    /** Converts a java field name into its method equivalent minus the get/set/add prefix */
    public static String toJavaMethodCase(String string) {
        if (string != null && !string.isEmpty()) {
            char first = string.charAt(0);
            if (" -_.$".indexOf(first) > -1) {
                string = string.substring(1);
            }
            return Character.toUpperCase(string.charAt(0)) + string.substring(1);
        } else {
            return string;
        }
    }

    /** Converts a file name into its java class name equivalent. */
    public static String toJavaClassCase(String string) {
        return toJavaMethodCase(string);
    }
}
