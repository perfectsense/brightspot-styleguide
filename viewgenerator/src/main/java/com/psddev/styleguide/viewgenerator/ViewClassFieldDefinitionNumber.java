package com.psddev.styleguide.viewgenerator;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.psddev.styleguide.JsonObject;

class ViewClassFieldDefinitionNumber extends ViewClassFieldDefinition {

    public ViewClassFieldDefinitionNumber(ViewClassDefinitions templateDefinitions, String parentTemplate, String name, List<JsonObject> values, Set<String> mapTemplates, String javaClassNamePrefix, boolean isDefaulted, boolean isStrictlyTyped) {
        super(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix, isDefaulted, isStrictlyTyped);
    }

    @Override
    public String getJavaFieldType(ViewClassImportsBuilder importsBuilder) {
        return "Number";
    }

    @Override
    public Set<ViewClassFieldType> getFieldValueTypes() {
        return Collections.singleton(ViewClassFieldNativeJavaType.NUMBER);
    }
}
