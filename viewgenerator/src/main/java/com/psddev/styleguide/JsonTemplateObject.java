package com.psddev.styleguide;

import static com.psddev.styleguide.JsonTemplateObject.TemplateFormat.Handlebars;
import static com.psddev.styleguide.JsonTemplateObject.TemplateFormat.Json;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.psddev.dari.util.ObjectUtils;

public class JsonTemplateObject extends JsonObject {

    public static final String TEMPLATE_KEY = "_template";
    public static final String JSON_VIEW_KEY = "_view";

    private String template;

    private Map<String, JsonObject> fields;

    private String fieldNotes;
    private String templateNotes;
    private TemplateFormat templateFormat;

    public enum TemplateFormat {
        Handlebars,
        Json
    }

    public JsonTemplateObject(String template,
                              Map<String, JsonObject> fields,
                              String fieldNotes,
                              String templateNotes,
                              TemplateFormat templateFormat) {
        this.template = template;
        this.fields = fields != null ? fields : Collections.emptyMap();
        this.fieldNotes = fieldNotes;
        this.templateNotes = templateNotes;
        this.templateFormat = templateFormat;
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

    public TemplateFormat getTemplateFormat() {
        return templateFormat;
    }

    public static String getTemplateName(Map<?, ?> map) {
        return (String) ObjectUtils.firstNonBlank(map.get(TEMPLATE_KEY), map.get(JSON_VIEW_KEY));
    }

    public static TemplateFormat getTemplateFormat(Map<?, ?> map) {
        if (!ObjectUtils.isBlank(map.get(TEMPLATE_KEY))) {
            return Handlebars;
        } else if (!ObjectUtils.isBlank(map.get(JSON_VIEW_KEY))) {
            return Json;
        }

        return null;
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
