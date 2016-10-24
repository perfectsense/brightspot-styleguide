package com.psddev.styleguide.viewgenerator;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.psddev.styleguide.JsonObject;

class ViewClassFieldDefinitionBoolean extends ViewClassFieldDefinition {

    public ViewClassFieldDefinitionBoolean(ViewClassDefinitions templateDefinitions, String parentTemplate, String name, List<JsonObject> values, Set<String> mapTemplates, String javaClassNamePrefix, boolean isDefaulted, boolean isStrictlyTyped) {
        super(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix, isDefaulted, isStrictlyTyped);
    }

    @Override
    public String getJavaFieldType(ViewClassImportsBuilder importsBuilder) {
        return "Boolean";
    }

    @Override
    public Set<ViewClassFieldType> getFieldValueTypes() {
        return Collections.singleton(ViewClassFieldNativeJavaType.BOOLEAN);
    }
}
