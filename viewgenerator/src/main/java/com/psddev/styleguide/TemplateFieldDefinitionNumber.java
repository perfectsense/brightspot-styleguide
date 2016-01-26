package com.psddev.styleguide;

import java.util.Collections;
import java.util.List;
import java.util.Set;

class TemplateFieldDefinitionNumber extends TemplateFieldDefinition {

    public TemplateFieldDefinitionNumber(String parentTemplate, String name, List<JsonObject> values, List<String> mapTemplates, String javaClassNamePrefix, String javaClassNameSuffix) {
        super(parentTemplate, name, values, mapTemplates, javaClassNamePrefix, javaClassNameSuffix);
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
