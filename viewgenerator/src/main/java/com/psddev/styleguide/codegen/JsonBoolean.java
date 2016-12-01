package com.psddev.styleguide.codegen;

/**
 * A boolean value in a JSON object.
 */
class JsonBoolean extends JsonValue {

    private boolean value;

    /**
     * Creates a boolean JSON object with location information.
     *
     * @param location the location of the boolean value within a file.
     * @param value the boolean value.
     */
    public JsonBoolean(JsonDataLocation location, boolean value) {
        super(location);
        this.value = value;
    }

    @Override
    public Boolean toRawValue() {
        return value;
    }

    @Override
    public String getTypeLabel() {
        return "Boolean";
    }

    @Override
    public String toString() {
        return String.valueOf(toRawValue());
    }
}
