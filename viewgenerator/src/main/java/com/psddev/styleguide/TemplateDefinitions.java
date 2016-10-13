package com.psddev.styleguide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.psddev.dari.util.StringUtils;

class TemplateDefinitions {

    private Map<String, TemplateDefinition> definitions;

    private boolean isTemplateFieldDefinitionsResolved = false;

    public TemplateDefinitions(
            Set<JsonTemplateObject> jsonTemplateObjects,
            Set<String> mapTemplates,
            String javaPackagePrefix,
            String javaClassNamePrefix) {

        Map<String, List<JsonTemplateObject>> jsonTemplateObjectsMap = new HashMap<>();

        for (JsonTemplateObject jsonTemplateObject : jsonTemplateObjects) {

            String templateName = jsonTemplateObject.getTemplateName();

            if (templateName == null) {
                continue;
            }

            List<JsonTemplateObject> list = jsonTemplateObjectsMap.get(templateName);
            if (list == null) {
                list = new ArrayList<>();
                jsonTemplateObjectsMap.put(templateName, list);
            }

            list.add(jsonTemplateObject);
        }

        definitions = new HashMap<>();

        jsonTemplateObjectsMap.entrySet().forEach((entry) -> {

            // again for consistency, remove the leading slash
            String name = StringUtils.removeStart(entry.getKey(), "/");
            String namePath = "";
            int lastPathIndex = -1;

            if (name.contains(".")) {
                lastPathIndex = name.lastIndexOf('.');
            } else {
                lastPathIndex = name.lastIndexOf('/');
            }

            if (lastPathIndex > 0) {
                namePath = name.substring(0, lastPathIndex);
            }

            String javaPackageName = generateJavaPackageName(javaPackagePrefix, namePath);

            if (!mapTemplates.contains(name)) {
                definitions.put(StringUtils.ensureStart(name, "/"), new TemplateDefinition(
                        this,
                        // add the leading slash back when passing to the TemplateDefinition
                        StringUtils.ensureStart(name, "/"),
                        StringUtils.removeSurrounding(javaPackageName, "."),
                        entry.getValue(),
                        mapTemplates,
                        javaClassNamePrefix));
            }
        });
    }

    // generates the package name for the template at the given path using
    // the given prefix
    private String generateJavaPackageName(String javaPackagePrefix, String templatePath) {

        StringBuilder javaPackageNameBuilder = new StringBuilder();

        String javaPackageSuffix = templatePath.replaceAll("/", ".");

        for (char c : (javaPackagePrefix + "." + javaPackageSuffix).replaceAll("\\.+", ".").toCharArray()) {

            // excludes characters that would be invalid in a java package name
            if (Character.isJavaIdentifierPart(c) || c == '.') {
                javaPackageNameBuilder.append(c);
            }
        }

        return javaPackageNameBuilder.toString();
    }

    private void resolveAllTemplateFieldDefinitions() {

        if (!isTemplateFieldDefinitionsResolved) {

            for (TemplateDefinition templateDef : definitions.values()) {
                templateDef.resolveFields(this);
            }

            isTemplateFieldDefinitionsResolved = true;
        }
    }

    public Collection<TemplateDefinition> get() {
        resolveAllTemplateFieldDefinitions();
        return definitions.values();
    }

    public TemplateDefinition getByName(String name) {
        resolveAllTemplateFieldDefinitions();
        return definitions.get(StringUtils.ensureStart(name, "/"));
    }
}
