package com.psddev.styleguide.codegen;

/**
 * A string value in a JSON object.
 */
class JsonString extends JsonValue {

    private String value;

    /**
     * Creates a string JSON object with location information.
     *
     * @param location the location of the string value within a file.
     * @param value the string value.
     */
    public JsonString(JsonDataLocation location, String value) {
        super(location);
        this.value = value;
    }

    @Override
    public String toRawValue() {
        return value;
    }

    @Override
    public String getTypeLabel() {
        return "String";
    }

    @Override
    public String toString() {
        return toRawValue();
    }
}
