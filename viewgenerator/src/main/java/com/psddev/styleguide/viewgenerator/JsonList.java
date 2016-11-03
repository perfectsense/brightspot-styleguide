package com.psddev.styleguide.viewgenerator;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
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

        StringBuilder builder = new StringBuilder();

        // use tree set for sort consistency
        Set<String> valueTypeLabels = getValues().stream()
                .map(JsonValue::getTypeLabel)
                .collect(Collectors.toCollection(TreeSet::new));

        builder.append("List");

        if (!valueTypeLabels.isEmpty()) {
            builder.append("<");
            builder.append(valueTypeLabels.stream().collect(Collectors.joining("|")));
            builder.append(">");
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return ObjectUtils.toJson(toRawValue());
    }
}
