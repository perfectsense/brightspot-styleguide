package com.psddev.styleguide.viewgenerator;

import java.util.List;
import java.util.stream.Collectors;

import com.psddev.dari.util.ObjectUtils;

/**
 * A list of values in a JSON object.
 */
class JsonList extends JsonValue {

    private List<JsonValue> values;

    /**
     * Creates a list JSON object with location information.
     *
     * @param location the location of the list within a file.
     * @param values the list of JSON values.
     */
    public JsonList(JsonDataLocation location, List<JsonValue> values) {
        super(location);
        this.values = values;
    }

    /**
     * Gets the JSON values in this list.
     *
     * @return the list of JSON values.
     */
    public List<JsonValue> getValues() {
        return values;
    }

    @Override
    public List<Object> toRawValue() {
        return values.stream()
                .map(JsonValue::toRawValue)
                .collect(Collectors.toList());
    }

    @Override
    public String getTypeLabel() {
        return "List";
    }

    @Override
    public String toString() {
        return ObjectUtils.toJson(toRawValue());
    }
}
