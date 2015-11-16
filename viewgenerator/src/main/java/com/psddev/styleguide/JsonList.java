package com.psddev.styleguide;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

class JsonList extends JsonObject {

    private List<JsonObject> values;
    private JsonObjectType valuesType;
    private String notes;

    public JsonList(List<JsonObject> values, JsonObjectType valuesType, String notes) {
        this.values = values;
        this.valuesType = valuesType;
        this.notes = notes;
    }

    @Override
    public String getNotes() {
        return notes;
    }

    @Override
    public Set<JsonTemplateObject> getIdentityTemplateObjects() {

        Set<JsonTemplateObject> set = Collections.newSetFromMap(new IdentityHashMap<>());

        for (JsonObject jsonObject : values) {
            set.addAll(jsonObject.getIdentityTemplateObjects());
        }

        return set;
    }

    @Override
    public JsonObjectType getType() {
        return JsonObjectType.LIST;
    }

    public List<JsonObject> getValues() {
        return values;
    }

    public JsonObjectType getValuesType() {
        return valuesType;
    }
}
