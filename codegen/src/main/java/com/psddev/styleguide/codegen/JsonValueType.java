package com.psddev.styleguide.codegen;

/**
 * Classification of the different types of JSON values based on class name.
 */
enum JsonValueType {

    NULL("Null", JsonNull.class),

    BOOLEAN("Boolean", JsonBoolean.class),

    NUMBER("Number", JsonNumber.class),

    STRING("String", JsonString.class),

    LIST("List", JsonList.class),

    MAP("Map", JsonMap.class),

    VIEW("View", JsonViewMap.class, JsonDelegateMap.class, JsonAbstractMap.class),

    OBJECT("Object", JsonValue.class);

    private String label;
    private Class<? extends JsonValue>[] classes;

    @SafeVarargs
    JsonValueType(String label, Class<? extends JsonValue>... classes) {
        this.label = label;
        this.classes = classes;
    }

    public String getLabel() {
        return label;
    }

    public static JsonValueType forClass(Class<? extends JsonValue> jsonValueClass) {

        for (JsonValueType valueType : JsonValueType.values()) {

            if (valueType.classes != null) {
                for (Class<? extends JsonValue> valueTypeClass : valueType.classes) {

                    if (valueTypeClass == jsonValueClass) {
                        return valueType;
                    }
                }
            }
        }

        return null;
    }
}
