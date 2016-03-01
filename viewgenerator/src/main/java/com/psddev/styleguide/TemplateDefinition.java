package com.psddev.styleguide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

class TemplateDefinition {

    private String name;
    private String javaPackageName;
    private String javaClassNamePrefix;

    private List<JsonTemplateObject> jsonTemplateObjects;
    private Set<String> mapTemplates;

    private List<TemplateFieldDefinition> fields = new ArrayList<>();
    private Set<String> notes = new LinkedHashSet<>();

    private boolean isFieldsResolved;

    /**
     * @param name the template name.
     * @param jsonTemplateObjects all the JSON template objects found for this template.
     * @param mapTemplates list of template names that are actually just String key/value pairs,
     *                     and should be treated as a Map of String ot String instead of a fielded Object.
     */
    public TemplateDefinition(String name, String javaPackageName, List<JsonTemplateObject> jsonTemplateObjects, Set<String> mapTemplates, String javaClassNamePrefix) {
        this.name = name;
        this.javaPackageName = javaPackageName;
        this.javaClassNamePrefix = javaClassNamePrefix;
        this.jsonTemplateObjects = jsonTemplateObjects;
        this.mapTemplates = mapTemplates;
        // add notes
        jsonTemplateObjects.stream()
                .map(JsonTemplateObject::getTemplateNotes)
                .filter(notes -> notes != null)
                .forEach(notes::add);
    }

    public String getName() {
        return name;
    }

    public String getJavaPackageName() {
        return javaPackageName;
    }

    public List<TemplateFieldDefinition> getFields(TemplateDefinitions templateDefinitions) {
        resolveFields(templateDefinitions);
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

    public void resolveFields(TemplateDefinitions templateDefinitions) {

        if (!isFieldsResolved) {

            Map<String, List<JsonObject>> fieldInstances = new LinkedHashMap<>();

            for (JsonTemplateObject jsonTemplateObject : jsonTemplateObjects) {

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

            fieldInstances.entrySet().stream()
                    .map((entry) -> TemplateFieldDefinition.createInstance(
                            templateDefinitions,
                            getName(),
                            entry.getKey(),
                            entry.getValue(),
                            mapTemplates,
                            javaClassNamePrefix))

                    .sorted((tfd1, tfd2) -> ObjectUtils.compare(tfd1.getName(), tfd2.getName(), true))

                    .forEach(fields::add);

            isFieldsResolved = true;
        }
    }

    public String getJavaClassSource(boolean removeDeprecations) {

        StringBuilder builder = new StringBuilder();

        Set<String> imports = new LinkedHashSet<>();

        if (!removeDeprecations) {
            imports.add("com.psddev.cms.view.ViewRequest");
        }
        imports.add("com.psddev.handlebars.HandlebarsTemplate");

        builder.append("/* AUTO-GENERATED FILE.  DO NOT MODIFY.\n");
        builder.append(" *\n");
        builder.append(" * This class was automatically generated on ").append(new SimpleDateFormat(ViewClassGenerator.DATE_FORMAT).format(new Date())).append(" by the\n");
        builder.append(" * Maven build tool based on JSON files it found. It should NOT be modified by hand.\n");
        builder.append(" */\n");
        builder.append("package ").append(javaPackageName).append(";\n");
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

        boolean printedAnyStaticStringVariableDeclaration = false;
        for (TemplateFieldDefinition fieldDef : fields) {

            if (fieldDef instanceof TemplateFieldDefinitionList
                    || fieldDef instanceof TemplateFieldDefinitionObject
                    || fieldDef instanceof TemplateFieldDefinitionString) {

                String declaration = fieldDef.getInterfaceStaticStringVariableDeclaration(1);
                if (declaration != null) {
                    builder.append("\n").append(declaration);
                    printedAnyStaticStringVariableDeclaration = true;
                }
            }
        }
        if (printedAnyStaticStringVariableDeclaration) {
            builder.append("\n");
        }

        for (TemplateFieldDefinition fieldDef : fields) {
            builder.append("\n").append(fieldDef.getInterfaceMethodDeclarationSource(1, imports)).append("\n");
        }

        // static inner Builder class.

        builder.append("\n");
        builder.append("    /**\n");
        builder.append("     * <p>Builder of ").append("{@link ").append(getJavaClassName()).append("}").append(" objects.</p>\n");
        builder.append("     */\n");
        builder.append("    class Builder {\n");
        if (!removeDeprecations) {
            builder.append("\n");
            builder.append("        @Deprecated\n");
            builder.append("        private ViewRequest request;\n");
        }
        if (!fields.isEmpty()) {
            for (TemplateFieldDefinition fieldDef : fields) {
                builder.append("\n").append(fieldDef.getInterfaceBuilderFieldDeclarationSource(2, imports));
            }
            builder.append("\n");
        }
        builder.append("\n");
        builder.append("        /**\n");
        builder.append("         * <p>Creates a builder for ").append("{@link ").append(getJavaClassName()).append("}").append(" objects.</p>\n");
        builder.append("         */\n");
        builder.append("        public Builder() {\n");
        builder.append("        }\n");
        if (!removeDeprecations) {
            builder.append("\n");
            builder.append("        /**\n");
            builder.append("         * @deprecated use {@link #Builder()} instead.\n");
            builder.append("         */\n");
            builder.append("        @Deprecated\n");
            builder.append("        public Builder(ViewRequest request) {\n");
            builder.append("            this.request = request;\n");
            builder.append("        }\n");
        }
        for (TemplateFieldDefinition fieldDef : fields) {
            builder.append("\n").append(fieldDef.getInterfaceBuilderMethodImplementationSource(2, imports, removeDeprecations)).append("\n");
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
        if (javaClassNamePrefix == null) {
            javaClassNamePrefix = "";
        }

        String className;

        int lastSlashAt = name.lastIndexOf('/');
        if (lastSlashAt >= 0) {
            className = name.substring(lastSlashAt + 1);
        } else {
            className = name;
        }

        return javaClassNamePrefix + StyleguideStringUtils.toPascalCase(className) + "View";
    }

    private String getJavaImportStatements(Set<String> imports) {

        StringBuilder builder = new StringBuilder();

        for (String importClass : imports) {
            builder.append("import ").append(importClass).append(";\n");
        }

        return builder.toString();
    }
}
