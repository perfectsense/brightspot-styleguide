package com.psddev.styleguide;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.StringUtils;

class TemplateDefinition {

    private String name;

    private List<TemplateFieldDefinition> fields;

    private Set<String> notes = new LinkedHashSet<>();

    /**
     * @param name the template name.
     * @param jsonTemplateObjects all the JSON template objects found for this template.
     * @param mapTemplates list of template names that are actually just String key/value pairs,
     *                     and should be treated as a Map of String ot String instead of a fielded Object.
     */
    public TemplateDefinition(String name, List<JsonTemplateObject> jsonTemplateObjects, List<String> mapTemplates) {
        this.name = name;
        this.fields = aggregateFieldDefinitions(jsonTemplateObjects, mapTemplates);
    }

    public String getName() {
        return name;
    }

    public List<TemplateFieldDefinition> getFields() {
        return new ArrayList<>(fields);
    }

    public Set<String> getNotes() {
        return notes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("--- ").append(name).append(" ---\n");
        for (TemplateFieldDefinition fieldDef : fields) {
            builder.append(fieldDef).append("\n");
        }
        builder.append("\n");

        return builder.toString();
    }

    private List<TemplateFieldDefinition> aggregateFieldDefinitions(List<JsonTemplateObject> jsonTemplateObjects, List<String> mapTemplates) {

        Map<String, List<JsonObject>> fieldInstances = new LinkedHashMap<>();

        for (JsonTemplateObject jsonTemplateObject : jsonTemplateObjects) {

            String templateNotes = jsonTemplateObject.getTemplateNotes();
            if (templateNotes != null) {
                notes.add(templateNotes);
            }

            for (Map.Entry<String, JsonObject> entry : jsonTemplateObject.getFields().entrySet()) {

                String fieldName = entry.getKey();
                JsonObject fieldValue = entry.getValue();

                List<JsonObject> fieldValues = fieldInstances.get(fieldName);
                if (fieldValues == null) {
                    fieldValues = new ArrayList<>();
                    fieldInstances.put(fieldName, fieldValues);
                }

                fieldValues.add(fieldValue);
            }
        }

        return fieldInstances.entrySet().stream()
                .map((entry) -> TemplateFieldDefinition.createInstance(getName(), entry.getKey(), entry.getValue(), mapTemplates))
                .collect(Collectors.toList());
    }

    public String getJavaClassSource(String packageName) {

        StringBuilder builder = new StringBuilder();

        Set<String> imports = new LinkedHashSet<>();

        imports.add("com.psddev.cms.view.ViewRequest");
        imports.add("com.psddev.handlebars.HandlebarsTemplate");

        builder.append("/* AUTO-GENERATED FILE.  DO NOT MODIFY.\n");
        builder.append(" *\n");
        builder.append(" * This class was automatically generated on ").append(new Date()).append(" by the\n");
        builder.append(" * Maven build tool based on JSON files it found. It should NOT be modified by hand.\n");
        builder.append(" */\n");
        builder.append("package ").append(packageName).append(";\n");
        builder.append("\n");
        builder.append("${importsPlaceholder}");
        builder.append("\n");

        if (!notes.isEmpty()) {
            builder.append("/**\n");
            for (String note : notes) {
                builder.append(" * <p>").append(note).append("</p>\n");
            }
            builder.append(" */\n");
        }

        builder.append("@HandlebarsTemplate(\"").append(StringUtils.removeStart(name, "/")).append("\")\n");
        builder.append("public interface ").append(getJavaClassName()).append(" {\n");

        for (TemplateFieldDefinition fieldDef : fields) {
            builder.append("\n").append(fieldDef.getInterfaceMethodDeclarationSource(1, imports)).append("\n");
        }

        // static inner Builder class.

        builder.append("\n");
        builder.append("    /**\n");
        builder.append("     * <p>Builder of ").append("{@link ").append(getJavaClassName()).append("}").append(" objects.</p>\n");
        builder.append("     */\n");
        builder.append("    class Builder {\n");
        builder.append("\n");
        builder.append("        private ViewRequest request;\n");
        builder.append("\n");
        for (TemplateFieldDefinition fieldDef : fields) {
            builder.append(fieldDef.getInterfaceBuilderFieldDeclarationSource(2, imports)).append("\n");
        }
        builder.append("\n");
        builder.append("        /**\n");
        builder.append("         * <p>Creates a builder for ").append("{@link ").append(getJavaClassName()).append("}").append(" objects.</p>\n");
        builder.append("         */\n");
        builder.append("        public Builder(ViewRequest request) {\n");
        builder.append("            this.request = request;\n");
        builder.append("        }\n");
        for (TemplateFieldDefinition fieldDef : fields) {
            builder.append("\n").append(fieldDef.getInterfaceBuilderMethodImplementationSource(2, imports)).append("\n");
        }

        builder.append("\n");
        builder.append("        /**\n");
        builder.append("         * Builds the {@link ").append(getJavaClassName()).append("}.\n");
        builder.append("         *\n");
        builder.append("         * @return the fully built {@link ").append(getJavaClassName()).append("}.\n");
        builder.append("         */\n");
        builder.append("        public ").append(getJavaClassName()).append(" build() {\n");
        builder.append("            return new ").append(getJavaClassName()).append("() {\n");
        for (TemplateFieldDefinition fieldDef : fields) {
            builder.append("\n").append(fieldDef.getInterfaceBuilderBuildMethodSource(4, imports)).append("\n");
        }
        builder.append("            };\n");
        builder.append("        }\n");
        builder.append("    }\n");
        builder.append("}\n");

        String javaSource = builder.toString();

        String importsSource = getJavaImportStatements(imports);

        javaSource = javaSource.replace("${importsPlaceholder}", importsSource);

        return javaSource;
    }

    public String getJavaClassName() {
        return getJavaClassNameForTemplate(name);
    }

    private String getJavaImportStatements(Set<String> imports) {

        StringBuilder builder = new StringBuilder();

        for (String importClass : imports) {
            builder.append("import ").append(importClass).append(";\n");
        }

        return builder.toString();
    }

    public static String getJavaClassNameForTemplate(String templateName) {
        String className;

        int lastSlashAt = templateName.lastIndexOf('/');
        if (lastSlashAt >= 0) {
            className = templateName.substring(lastSlashAt + 1);
        } else {
            className = templateName;
        }

        return StringUtils.toPascalCase(className) + "View";
    }
}
