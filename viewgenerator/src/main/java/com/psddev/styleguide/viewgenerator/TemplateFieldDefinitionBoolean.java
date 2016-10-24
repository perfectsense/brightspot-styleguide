package com.psddev.styleguide.viewgenerator;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.psddev.styleguide.JsonObject;

class TemplateFieldDefinitionBoolean extends TemplateFieldDefinition {

    public TemplateFieldDefinitionBoolean(TemplateDefinitions templateDefinitions, String parentTemplate, String name, List<JsonObject> values, Set<String> mapTemplates, String javaClassNamePrefix, boolean isDefaulted, boolean isStrictlyTyped) {
        super(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix, isDefaulted, isStrictlyTyped);
    }

    @Override
    public String getJavaFieldType(ViewClassImportsBuilder importsBuilder) {
        return "Boolean";
    }

    @Override
    public Set<TemplateFieldType> getFieldValueTypes() {
        return Collections.singleton(NativeJavaTemplateFieldType.BOOLEAN);
    }
}
