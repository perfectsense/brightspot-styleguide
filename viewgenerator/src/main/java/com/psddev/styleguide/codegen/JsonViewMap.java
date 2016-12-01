package com.psddev.styleguide.codegen;

import java.util.Map;

import com.psddev.dari.util.ObjectUtils;

/**
 * A specialized JsonMap that has been associated with a particular view and/or
 * template. It has been resolved to a form where specialized JSON keys have
 * been removed from the underlying map and stored as discreet data attributes
 * of the class.
 */
class JsonViewMap extends JsonMap {

    private JsonFile wrapper;

    private ViewKey viewKey;

    private String notes;

    /**
     * Creates a new JSON view map.
     *
     * @param location the location of the view map within a JSON file.
     * @param values the values of the map.
     * @param viewKey the associated view key.
     * @param notes the documentation notes.
     */
    public JsonViewMap(JsonDataLocation location, Map<JsonKey, JsonValue> values, JsonFile wrapper, ViewKey viewKey, String notes) {
        super(location, values);
        this.wrapper = wrapper;
        this.viewKey = viewKey;
        this.notes = notes;
    }

    /**
     * Gets the JSON file declared as being the wrapper for this view map.
     *
     * @return the wrapper file
     */
    public JsonFile getWrapper() {
        return wrapper;
    }

    /**
     * Gets the view key associated with this view map.
     *
     * @return the view key.
     */
    public ViewKey getViewKey() {
        return viewKey;
    }

    /**
     * Gets the documentation notes that have been associated with this view map.
     *
     * @return the notes.
     */
    public String getNotes() {
        return notes;
    }

    @Override
    public String getTypeLabel() {
        return "ViewMap";
    }

    @Override
    public String toString() {
        return ObjectUtils.toJson(toRawValue());
    }
}
