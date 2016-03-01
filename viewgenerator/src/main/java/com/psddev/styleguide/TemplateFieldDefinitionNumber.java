package com.psddev.styleguide;

import java.util.Collections;
import java.util.List;
import java.util.Set;

class TemplateFieldDefinitionNumber extends TemplateFieldDefinition {

    public TemplateFieldDefinitionNumber(TemplateDefinitions templateDefinitions, String parentTemplate, String name, List<JsonObject> values, Set<String> mapTemplates, String javaClassNamePrefix) {
        super(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix);
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
