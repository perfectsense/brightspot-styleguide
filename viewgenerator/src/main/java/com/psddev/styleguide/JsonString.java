package com.psddev.styleguide;

import java.util.Collections;
import java.util.Set;

public class JsonString extends JsonObject {

    private String value;
    private String notes;

    public JsonString(String value, String notes) {
        this.value = value;
        this.notes = notes;
    }

    public String getValue() {
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
        return JsonObjectType.STRING;
    }
}
