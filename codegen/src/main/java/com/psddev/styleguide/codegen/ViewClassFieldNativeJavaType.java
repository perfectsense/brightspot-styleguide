package com.psddev.styleguide.codegen;

/**
 * Different types of fields that are native to the Java programming language.
 */
enum ViewClassFieldNativeJavaType implements ViewClassFieldType {

    /**
     * Native Java Object type.
     */
    OBJECT("java.lang.Object"),

    /**
     * Native Java String type.
     */
    STRING("java.lang.String"),

    /**
     * Native Java CharSequence type.
     */
    CHAR_SEQUENCE("java.lang.CharSequence"),

    /**
     * Native Java Number type.
     */
    NUMBER("java.lang.Number"),

    /**
     * Native Java Boolean type.
     */
    BOOLEAN("java.lang.Boolean"),

    /**
     * Native Java Collection type.
     */
    COLLECTION("java.util.Collection"),

    /**
     * Native Java Iterable type.
     */
    ITERABLE("java.lang.Iterable"),

    /**
     * Native Java Map type.
     */
    MAP("java.util.Map");

    private String fullyQualifiedClassName;

    ViewClassFieldNativeJavaType(String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
    }

    @Override
    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }
}
