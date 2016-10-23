package com.psddev.styleguide.viewgenerator;

import java.util.Arrays;

import com.psddev.dari.util.StringUtils;

public class StyleguideStringUtils {

    public static final String NEW_LINE = "\n";

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
        if (string != null && !string.isEmpty()) {
            char first = string.charAt(0);
            if (" -_.$".indexOf(first) > -1) {
                string = string.substring(1);
            }
            return StringUtils.toPascalCase(string);
        } else {
            return string;
        }
    }

    /** Adds 4 spaces for each indent. */
    public static String indent(int indent) {
        char[] spaces = new char[indent * 4];
        Arrays.fill(spaces, ' ');
        return new String(spaces);
    }
}
