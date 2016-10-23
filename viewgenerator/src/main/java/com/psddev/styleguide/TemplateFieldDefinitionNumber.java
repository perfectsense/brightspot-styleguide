package com.psddev.styleguide;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TemplateFieldDefinitionNumber extends TemplateFieldDefinition {

    public TemplateFieldDefinitionNumber(TemplateDefinitions templateDefinitions, String parentTemplate, String name, List<JsonObject> values, Set<String> mapTemplates, String javaClassNamePrefix, boolean isDefaulted, boolean isStrictlyTyped) {
        super(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix, isDefaulted, isStrictlyTyped);
    }

    @Override
    public String getJavaFieldType(TemplateImportsBuilder importsBuilder) {
        return "Number";
    }

    @Override
    public Set<TemplateFieldType> getFieldValueTypes() {
        return Collections.singleton(NativeJavaTemplateFieldType.NUMBER);
    }
}
