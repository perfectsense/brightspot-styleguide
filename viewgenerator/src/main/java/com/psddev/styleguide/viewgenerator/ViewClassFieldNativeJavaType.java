package com.psddev.styleguide.viewgenerator;

enum ViewClassFieldNativeJavaType implements ViewClassFieldType {

    OBJECT("java.lang.Object"),
    STRING("java.lang.String"),
    NUMBER("java.lang.Number"),
    BOOLEAN("java.lang.Boolean"),
    COLLECTION("java.util.Collection"),
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
