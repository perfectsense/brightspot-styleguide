package com.psddev.styleguide;

import java.util.Collections;
import java.util.Set;

class JsonNumber extends JsonObject {

    private Number value;
    private String notes;

    public JsonNumber(Number value, String notes) {
        this.value = value;
        this.notes = notes;
    }

    public Number getValue() {
        return value;
    }

    @Override
    public String getNotes() {
        return notes;
    }

    @Override
    public Set<JsonTemplateObject> getIdentityTemplateObjects() {
        return Collections.emptySet();
    }

    @Override
    public JsonObjectType getType() {
        return JsonObjectType.NUMBER;
    }
}
