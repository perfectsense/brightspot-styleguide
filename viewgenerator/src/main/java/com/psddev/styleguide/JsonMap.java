package com.psddev.styleguide;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class JsonMap extends JsonObject {

    private Map<String, ?> value;
    private String notes;

    public JsonMap(Map<String, ?> value, String notes) {
        this.value = value;
        this.notes = notes;
    }

    @Override
    public Set<JsonTemplateObject> getIdentityTemplateObjects() {
        return Collections.emptySet();
    }

    @Override
    public JsonObjectType getType() {
        return JsonObjectType.MAP;
    }

    @Override
    public String getNotes() {
        return notes;
    }
}
