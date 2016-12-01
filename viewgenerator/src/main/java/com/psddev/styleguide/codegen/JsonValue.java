package com.psddev.styleguide.codegen;

/**
 * A value and location of a JSON object within a file.
 */
abstract class JsonValue {

    protected JsonDataLocation location;

    /**
     * Creates a JSON value with location information.
     *
     * @param location the location of this value within a file.
     */
    public JsonValue(JsonDataLocation location) {
        this.location = location;
    }

    /**
     * Gets the location of this value within a file.
     *
     * @return the location of this value.
     */
    public JsonDataLocation getLocation() {
        return location;
    }

    /**
     * Converts this JSON value to its simplest Java data type. i.e.
     * {@link java.lang.Boolean}, {@link java.lang.String},
     * {@link java.lang.Number}, {@link java.util.Map}, or {@link java.util.List}
     *
     * @return the raw JSON value.
     */
    public abstract Object toRawValue();

    /**
     * Gets a friendly label that describes this type of JSON value.
     *
     * @return the type label.
     */
    public abstract String getTypeLabel();
}
