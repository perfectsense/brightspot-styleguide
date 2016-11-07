package com.psddev.styleguide.viewgenerator;

import java.util.Arrays;

import com.psddev.dari.util.StringUtils;

/**
 * Collection of utility methods for generating Java source code.
 */
class ViewClassStringUtils {

    /**
     * New line character.
     */
    public static final String NEW_LINE = "\n";

    /**
     * Converts a java field name into its method equivalent minus the get/set/add prefix
     *
     * @param string the string to convert.
     * @return a String that is valid to be used as the name of a Java method.
     */
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

    /**
     * Converts a file name into its java class name equivalent.
     *
     * @param string the string to convert.
     * @return a String that is valid to be used as the name of a Java class.
     */
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

    /**
     * Adds 4 spaces for each indent.
     *
     * @param indent the number of 4-spaced indents to return.
     * @return spaces characters representing the desired indentation level.
     */
    public static String indent(int indent) {
        char[] spaces = new char[indent * 4];
        Arrays.fill(spaces, ' ');
        return new String(spaces);
    }
}
