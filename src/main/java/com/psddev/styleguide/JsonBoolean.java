package com.psddev.styleguide;

import java.util.Collections;
import java.util.Set;

class JsonBoolean extends JsonObject {

    private Boolean value;
    private String notes;

    public JsonBoolean(Boolean value, String notes) {
        this.value = value;
        this.notes = notes;
    }

    public Boolean getValue() {
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
        return JsonObjectType.BOOLEAN;
    }
}
