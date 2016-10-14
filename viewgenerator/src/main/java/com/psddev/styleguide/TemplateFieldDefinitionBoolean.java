package com.psddev.styleguide;

import java.util.Collections;
import java.util.List;
import java.util.Set;

class TemplateFieldDefinitionBoolean extends TemplateFieldDefinition {

    public TemplateFieldDefinitionBoolean(TemplateDefinitions templateDefinitions, String parentTemplate, String name, List<JsonObject> values, Set<String> mapTemplates, String javaClassNamePrefix) {
        super(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix);
    }

    @Override
    public String getJavaFieldType(TemplateImportsBuilder importsBuilder) {
        return "Boolean";
    }

    @Override
    public Set<TemplateFieldType> getFieldValueTypes() {
        return Collections.singleton(NativeJavaTemplateFieldType.BOOLEAN);
    }
}
