package com.psddev.styleguide.codegen;

/**
 * A null value in a JSON object.
 */
class JsonNull extends JsonValue {

    /**
     * Creates a null JSON value with location information.
     *
     * @param location the location of this null value within a file.
     */
    public JsonNull(JsonDataLocation location) {
        super(location);
    }

    @Override
    public Object toRawValue() {
        return null;
    }

    @Override
    public String getTypeLabel() {
        return "null";
    }

    @Override
    public String toString() {
        return getTypeLabel();
    }
}
