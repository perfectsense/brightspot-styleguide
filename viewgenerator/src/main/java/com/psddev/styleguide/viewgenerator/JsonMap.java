package com.psddev.styleguide.viewgenerator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.psddev.dari.util.ObjectUtils;

/**
 * A map of values in a JSON object.
 */
class JsonMap extends JsonValue {

    private Map<JsonKey, JsonValue> values;

    // Used for powering more convenient String based API lookups within the main map.
    private Map<String, JsonKey> keyMap;

    /**
     * Creates a map JSON object with location information.
     *
     * @param location the location of the map within a file.
     * @param values the map of JSON key/values.
     */
    public JsonMap(JsonDataLocation location, Map<JsonKey, JsonValue> values) {
        super(location);
        this.values = values;
        this.keyMap = values.entrySet().stream().collect(Collectors.toMap(
                entry -> entry.getKey().getName(),
                Map.Entry::getKey,
                (key, value) -> value,
                LinkedHashMap::new));
    }

    /**
     * Gets the underlying values of this JSON map.
     *
     * @return the map of key/value pairs for this json map.
     */
    public Map<JsonKey, JsonValue> getValues() {
        return values;
    }

    /**
     * Checks if a given key is contained within the map.
     *
     * @param name the name of the JSON key.
     * @return true if the key is contained in the map.
     */
    public boolean containsKey(String name) {
        return keyMap.containsKey(name);
    }

    /**
     * Gets the JSON key for a given name.
     *
     * @param name the name of the JSON key.
     * @return the JSON key for the given name.
     */
    public JsonKey getKey(String name) {
        return keyMap.get(name);
    }

    /**
     * Gets the JSON value for the given key name.
     *
     * @param name the name of the JSON key.
     * @return the JSON value for the given key name.
     */
    public JsonValue getValue(String name) {
        return getValue(getKey(name));
    }

    /**
     * Gets the JSON value for the given key.
     *
     * @param jsonKey the key to lookup.
     * @return the JSON value for the given key.
     */
    public JsonValue getValue(JsonKey jsonKey) {
        return jsonKey != null ? values.get(jsonKey) : null;
    }

    /**
     * Gets a specific type of JSON value for the given key. If a value at the
     * specified key is found but the resulting value type does not match the
     * desired valueClass, then null is returned.
     *
     * @param valueClass the type of JSON value expected.
     * @param name the name of JSON key.
     * @param <T> the type of JsonValue to check for.
     * @return the value for the given key.
     */
    public <T extends JsonValue> T getValueAs(Class<T> valueClass, String name) {

        JsonValue value = getValue(name);

        if (value != null && valueClass.isAssignableFrom(value.getClass())) {
            @SuppressWarnings("unchecked")
            T typedValue = (T) value;
            return typedValue;
        } else {
            return null;
        }
    }

    /**
     * Gets the raw value at the JSON key with the specified name.
     *
     * @param name the name of the JSON key.
     * @return the raw value at the given key name.
     */
    public Object getRawValue(String name) {
        JsonValue jsonValue = getValue(name);
        return jsonValue != null ? jsonValue.toRawValue() : null;
    }

    /**
     * Gets the raw value at the JSON key with the specified name and converts
     * it to the desired type.
     *
     * @param valueClass the type the raw value should be converted to.
     * @param name the name of the JSON key.
     * @param <T> the type of the raw value returned.
     * @return the raw value converted to the given type for a particular key.
     */
    public <T> T getRawValueAs(Class<T> valueClass, String name) {
        JsonValue jsonValue = getValue(name);
        return jsonValue != null ? ObjectUtils.to(valueClass, jsonValue.toRawValue()) : null;
    }

    @Override
    public Map<String, Object> toRawValue() {
        return values.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getName(),
                        entry -> entry.getValue().toRawValue(),
                        (key, value) -> value,
                        LinkedHashMap::new));
    }

    @Override
    public String getTypeLabel() {
        return "Map";
    }

    @Override
    public String toString() {
        return ObjectUtils.toJson(toRawValue());
    }
}
