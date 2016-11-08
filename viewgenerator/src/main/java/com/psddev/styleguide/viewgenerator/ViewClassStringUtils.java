package com.psddev.styleguide.viewgenerator;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.StringUtils;

/**
 * Collection of utility methods for generating Java source code.
 */
class ViewClassStringUtils {

    static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
            "assert", "abstract", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double",
            "else", "enum", "extends", "final", "finally", "float", "for",
            "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "null", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while", "true", "false",
            "null"
    ));

    /**
     * New line character.
     */
    public static final String NEW_LINE = "\n";

    /**
     * Converts a java field name into its method equivalent minus the get/set/add prefix
     * such that it conforms with the Java bean spec for converting to and from
     * read method names and property descriptors.
     *
     * @param string the string to convert.
     * @return a String that is valid to be used as the name of a Java method.
     */
    public static String toJavaMethodCase(String string) {
        if (string != null && !string.isEmpty()) {
            if (string.length() > 1) {
                if (Character.isUpperCase(string.charAt(1))) {
                    return string;
                } else {
                    return Character.toUpperCase(string.charAt(0)) + string.substring(1);
                }
            } else {
                return string.toUpperCase();
            }
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
     * Converts a path to a Java class name by grabbing the file name part of
     * the path, removing the file extension and converting it to a valid Java
     * class name. If the path is null or has no name parts, null is returned.
     *
     * @param path the path to convert.
     * @return a Java class name from the file path.
     */
    public static String toJavaClassName(Path path) {

        if (path == null) {
            return null;
        }

        int pathNameCount = path.getNameCount();
        if (pathNameCount >= 1) {

            String fileName = path.getName(pathNameCount - 1).toString();

            // Remove any file extension, or if it starts with a dot, just remove it.
            int lastDotAt = fileName.lastIndexOf('.');
            if (lastDotAt == 0) {
                fileName = fileName.substring(1);

            } else if (lastDotAt > 0) {
                fileName = fileName.substring(0, lastDotAt);
            }

            return ViewClassStringUtils.toJavaClassCase(fileName);

        } else {
            return null;
        }
    }

    /**
     * Converts a template file path into an associated Java package name. If
     * the path is only a file name with no path parts, then the empty String
     * is returned.
     *
     * @param path the path to convert.
     * @return a Java package name from the file path.
     */
    public static String toJavaPackageName(Path path) {

        if (path == null) {
            return null;
        }

        int pathNameCount = path.getNameCount();
        if (pathNameCount > 1) {

            //List<String> pathParts = new ArrayList<>();
            String[] pathNames = new String[pathNameCount - 1];

            for (int i = 0; i < pathNames.length; i++) {

                String name = path.getName(i).toString();

                // make sure names that are java keywords are suffixed with an underscore.
                if (ViewClassStringUtils.JAVA_KEYWORDS.contains(name)) {

                    name = name + "_";

                } else {

                    // replace all characters that are not valid Java identifier parts with an underscore.
                    name = name.chars().boxed()
                            .map(c -> Character.isJavaIdentifierPart(c) ? String.valueOf((char) (int) c) : "_")
                            .collect(Collectors.joining());

                    // make sure names starting with invalid Java identifier starts are prefixed with an underscore.
                    if (!Character.isJavaIdentifierStart(name.charAt(0))) {
                        name = "_" + name;
                    }
                }

                pathNames[i] = name;
            }

            return Arrays.stream(pathNames).collect(Collectors.joining("."));

        } else {
            return "";
        }
    }

    /**
     * Checks if {@code packageName} is a valid Java package name.
     *
     * @param packageName the package name to check.
     * @return true if the package name is valid, false otherwise.
     */
    public static boolean isValidJavaPackageName(String packageName) {
        return packageName != null
                && !packageName.isEmpty()
                && Arrays.stream(packageName.split("\\.")).allMatch(name -> isValidJavaIdentifier(name) && !isJavaKeyword(name));
    }

    /**
     * Checks if {@code className} is a valid Java class name.
     *
     * @param className the class name to check.
     * @return true if the class name is valid, false otherwise.
     */
    public static boolean isValidJavaClassName(String className) {
        // TODO: Still need to implement
        return true;
    }

    /**
     * Checks if the given String is a valid Java identifier. This method does
     * not take into account if it is a reserved Java keyword or not. You can
     * call {@link #isJavaKeyword(String)} for that check.
     *
     * @param identifier the Java identifier to check.
     * @return true if it's a valid identifier, false otherwise.
     */
    public static boolean isValidJavaIdentifier(String identifier) {
        return identifier != null && !identifier.isEmpty()
                && Character.isJavaIdentifierStart(identifier.charAt(0))
                && (identifier.length() == 1 || identifier.substring(1).chars().allMatch(Character::isJavaIdentifierPart));
    }

    /**
     * Checks if the given identifier is a reserved Java keyword.
     *
     * @param identifier the Java identifier to check.
     * @return true if this identifier is a reserved Java keyword, false otherwise.
     */
    public static boolean isJavaKeyword(String identifier) {
        return JAVA_KEYWORDS.contains(identifier);
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
