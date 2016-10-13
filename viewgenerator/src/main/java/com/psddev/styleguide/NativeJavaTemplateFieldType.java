package com.psddev.styleguide;

public enum NativeJavaTemplateFieldType implements TemplateFieldType {

    OBJECT("java.lang.Object"),
    STRING("java.lang.String"),
    NUMBER("java.lang.Number"),
    BOOLEAN("java.lang.Boolean"),
    COLLECTION("java.util.Collection"),
    MAP("java.util.Map");

    private String fullyQualifiedClassName;

    NativeJavaTemplateFieldType(String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
    }

    @Override
    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }
}
