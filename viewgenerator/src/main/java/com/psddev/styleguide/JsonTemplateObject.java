package com.psddev.styleguide;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

class JsonTemplateObject extends JsonObject {

    private String template;

    private Map<String, JsonObject> fields;

    private String fieldNotes;
    private String templateNotes;

    public JsonTemplateObject(String template, Map<String, JsonObject> fields, String fieldNotes, String templateNotes) {
        this.template = template;
        this.fields = fields;
        this.fieldNotes = fieldNotes;
        this.templateNotes = templateNotes;
    }

    public String getTemplateName() {
        return template;
    }

    public Map<String, JsonObject> getFields() {
        return fields;
    }

    @Override
    public String getNotes() {
        return fieldNotes;
    }

    public String getFieldNotes() {
        return fieldNotes;
    }

    public String getTemplateNotes() {
        return templateNotes;
    }

    @Override
    public Set<JsonTemplateObject> getIdentityTemplateObjects() {

        Set<JsonTemplateObject> set = Collections.newSetFromMap(new IdentityHashMap<>());

        set.add(this);

        for (JsonObject jsonObject : fields.values()) {
            set.addAll(jsonObject.getIdentityTemplateObjects());
        }

        return set;
    }

    @Override
    public JsonObjectType getType() {
        return JsonObjectType.TEMPLATE_OBJECT;
    }

    @Override
    public String toString() {
        return template + ": " + fields.keySet();
    }
}
