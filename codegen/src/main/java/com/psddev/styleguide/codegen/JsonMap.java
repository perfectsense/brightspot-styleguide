package com.psddev.styleguide.codegen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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
    public boolean containsKey(Object name) {
        if (name instanceof JsonSpecialKey) {
            return ((JsonSpecialKey) name).getAliases().stream().anyMatch(this::containsKey);

        } else if (name instanceof String) {
            return keyMap.containsKey(name);

        } else {
            return false;
        }
    }

    /**
     * Recursively checks if a given key is contained within the map, or any
     * of the map's values.
     *
     * @param name the name of the JSON key.
     * @return true if the key is contained anywhere in the map.
     */
    public boolean containsKeyAnywhere(Object name) {
        if (name instanceof JsonSpecialKey) {
            return ((JsonSpecialKey) name).getAliases().stream().anyMatch(this::containsKeyAnywhere);

        } else if (name instanceof String) {
            return keyMap.containsKey(name)
                    || getValues().values().stream().anyMatch(itemValue -> containsKeyAnywhere((String) name, itemValue));

        } else {
            return false;
        }
    }

    /*
     * Recursively checks if a given key is contained anywhere within value.
     *
     * @param name the name of the JSON key.
     * @param value the value to check for the key
     * @return true if the key is contained anywhere in the value.
     */
    private boolean containsKeyAnywhere(String name, JsonValue value) {

        if (value instanceof JsonMap) {
            return ((JsonMap) value).containsKeyAnywhere(name);

        } else if (value instanceof JsonList) {
            return ((JsonList) value).getValues().stream().anyMatch(itemValue -> containsKeyAnywhere(name, itemValue));

        } else {
            return false;
        }
    }

    /**
     * Gets the JSON key for a given name.
     *
     * @param name the name of the JSON key.
     * @return the JSON key for the given name.
     */
    public JsonKey getKey(Object name) {
        if (name instanceof JsonSpecialKey) {
            return ((JsonSpecialKey) name).getAliases().stream().map(this::getKey).filter(Objects::nonNull).findFirst().orElse(null);

        } else if (name instanceof String) {
            return keyMap.get(name);

        } else {
            return null;
        }
    }

    /**
     * Gets the JSON value for the given key name.
     *
     * @param name the name of the JSON key.
     * @return the JSON value for the given key name.
     */
    public JsonValue getValue(Object name) {
        if (name instanceof JsonSpecialKey) {
            return ((JsonSpecialKey) name).getAliases().stream().map(this::getValue).filter(Objects::nonNull).findFirst().orElse(null);

        } else if (name instanceof String) {
            return getValue(getKey(name));

        } else {
            return null;
        }
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
    public <T extends JsonValue> T getValueAs(Class<T> valueClass, Object name) {

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
    public Object getRawValue(Object name) {
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
    public <T> T getRawValueAs(Class<T> valueClass, Object name) {
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
