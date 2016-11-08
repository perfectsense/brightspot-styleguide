package com.psddev.styleguide.viewgenerator;

/**
 * A number value in a JSON object.
 */
class JsonNumber extends JsonValue {

    private Number value;

    /**
     * Creates a number JSON object with location information.
     *
     * @param location the location of the number value within a file.
     * @param value the number value.
     */
    public JsonNumber(JsonDataLocation location, Number value) {
        super(location);
        this.value = value;
    }

    @Override
    public Number toRawValue() {
        return value;
    }

    @Override
    public String getTypeLabel() {
        return "Number";
    }

    @Override
    public String toString() {
        return String.valueOf(toRawValue());
    }
}
