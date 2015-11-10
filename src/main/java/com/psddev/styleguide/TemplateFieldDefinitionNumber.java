package com.psddev.styleguide;

import java.util.Collections;
import java.util.List;
import java.util.Set;

class TemplateFieldDefinitionNumber extends TemplateFieldDefinition {

    public TemplateFieldDefinitionNumber(String parentTemplate, String name, List<JsonObject> values, List<String> mapTemplates) {
        super(parentTemplate, name, values, mapTemplates);
    }

    @Override
    public String getJavaFieldType(Set<String> imports) {
        return "Number";
    }

    @Override
    public Set<String> getValueTypes() {
        return Collections.singleton("java.lang.Number");
    }
}
