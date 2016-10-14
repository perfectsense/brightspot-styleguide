package com.psddev.styleguide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

import static com.psddev.styleguide.StyleguideStringUtils.indent;
import static com.psddev.styleguide.StyleguideStringUtils.NEW_LINE;

class TemplateDefinition implements TemplateFieldType {

    private TemplateDefinitions templateDefinitions;

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
    public TemplateDefinition(TemplateDefinitions templateDefinitions, String name, String javaPackageName, List<JsonTemplateObject> jsonTemplateObjects, Set<String> mapTemplates, String javaClassNamePrefix) {
        this.templateDefinitions = templateDefinitions;
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

    public List<TemplateFieldDefinition> getFields() {
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

    @Override
    public String getFullyQualifiedClassName() {
        return getJavaPackageName() + "." + getJavaClassName();
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

        StringBuilder sourceBuilder = new StringBuilder();

        Set<String> imports = new LinkedHashSet<>();

        final String viewRequestClassName = "com.psddev.cms.view.ViewRequest";
        final String viewInterfaceClassName = "com.psddev.cms.view.ViewInterface";
        final String handlebarsTemplateClassName = "com.psddev.handlebars.HandlebarsTemplate";
        final String jsonTemplateClassName = "com.psddev.cms.view.JsonView";
        String templateClassName = handlebarsTemplateClassName;

        if (isJsonFormat()) {
            templateClassName = jsonTemplateClassName;
        }

        if (!removeDeprecations) {
            imports.add(viewRequestClassName);
        }

        if (classExists(viewInterfaceClassName)) {
            imports.add(viewInterfaceClassName);
        }

        imports.add(templateClassName);

        // Standard messaging for auto-generated file
        sourceBuilder.append(new TemplateJavadocsBuilder()
                .addLine("AUTO-GENERATED FILE.  DO NOT MODIFY.")
                .newLine()
                .add("This class was automatically generated on ")
                .add(new SimpleDateFormat(ViewClassGenerator.DATE_FORMAT).format(new Date()))
                .add(" by the")
                .newLine()
                .addLine("Maven build tool based on JSON files it found. It should NOT be modified by hand.")
                .buildCommentsSource(0));

        // Package declaration
        sourceBuilder.append("package ").append(javaPackageName).append(";").append(NEW_LINE);
        sourceBuilder.append(NEW_LINE);

        // Imports - we collect them as we process, so this just a placeholder which we'll replace at the end.
        sourceBuilder.append("${importsPlaceholder}");
        sourceBuilder.append(NEW_LINE);

        // JSON generated class level javadocs
        if (!notes.isEmpty()) {
            TemplateJavadocsBuilder classNotesBuilder = new TemplateJavadocsBuilder();
            notes.forEach(classNotesBuilder::addParagraph);
            sourceBuilder.append(classNotesBuilder.buildJavadocsSource(0));
        }

        // Annotations
        if (classExists(viewInterfaceClassName)) {
            sourceBuilder.append("@").append(toSimpleClassName(viewInterfaceClassName)).append(NEW_LINE);
        }
        sourceBuilder.append("@").append(toSimpleClassName(templateClassName));
        if (!isJsonFormat()) {
            sourceBuilder.append("(\"").append(StringUtils.removeStart(name, "/")).append("\")");
        }
        sourceBuilder.append(NEW_LINE);

        // Interface declaration
        sourceBuilder.append("public interface ").append(getClassName()).append(getImplementedTemplateFieldDefinitions(imports)).append(" {").append(NEW_LINE);

        // Static view type/element constants
        for (TemplateFieldDefinition fieldDef : fields) {

            if (fieldDef instanceof TemplateFieldDefinitionList
                    || fieldDef instanceof TemplateFieldDefinitionObject
                    || fieldDef instanceof TemplateFieldDefinitionString) {

                String declaration = fieldDef.getInterfaceStaticStringVariableDeclaration(1, "_ELEMENT");
                sourceBuilder.append(NEW_LINE).append(declaration).append(NEW_LINE);
            }
        }

        if (!removeDeprecations) {
            for (TemplateFieldDefinition fieldDef : fields) {

                if (fieldDef instanceof TemplateFieldDefinitionList
                        || fieldDef instanceof TemplateFieldDefinitionObject
                        || fieldDef instanceof TemplateFieldDefinitionString) {

                    String declaration = fieldDef.getInterfaceStaticStringVariableDeclarationDeprecated(1, "_TYPE", "_ELEMENT");
                    sourceBuilder.append(NEW_LINE).append(declaration).append(NEW_LINE);
                }
            }
        }

        // Interface method declarations
        for (TemplateFieldDefinition fieldDef : fields) {
            sourceBuilder.append(NEW_LINE);
            sourceBuilder.append(fieldDef.getInterfaceMethodDeclarationSource(1, imports));
            sourceBuilder.append(NEW_LINE);
        }

        sourceBuilder.append(NEW_LINE);

        // Field level interfaces
        for (TemplateFieldDefinition fieldDef : fields) {

            Set<TemplateFieldType> fieldValueTypes = fieldDef.getFieldValueTypes();

            // Only create the view field level interface if there's more than one field value type
            if (fieldValueTypes.size() > 1 && fieldDef instanceof TemplateFieldType) {

                // Adds javadocs if it exists (which it should).
                sourceBuilder.append(new TemplateJavadocsBuilder()
                        .startParagraph()
                        .addFieldValueTypesSnippet(fieldDef)
                        .endParagraph()
                        .buildJavadocsSource(1));

                // field level interface declaration
                sourceBuilder.append(indent(1)).append("interface ").append(((TemplateFieldType) fieldDef).getLocalClassName()).append(" {").append(NEW_LINE);
                sourceBuilder.append(indent(1)).append("}").append(NEW_LINE);
            }
        }

        // Builder class
        sourceBuilder.append(NEW_LINE);

        // Builder class javadocs
        sourceBuilder.append(new TemplateJavadocsBuilder()
                .startParagraph()
                .add("Builder of ")
                .addLink(getClassName())
                .add(" objects.")
                .endParagraph()
                .buildJavadocsSource(1));

        // Builder class declaration
        sourceBuilder.append(indent(1)).append("class Builder {").append(NEW_LINE);
        {
            if (!removeDeprecations) {
                sourceBuilder.append(NEW_LINE);
                sourceBuilder.append(indent(2)).append("@Deprecated").append(NEW_LINE);
                sourceBuilder.append(indent(2)).append("private ViewRequest request;").append(NEW_LINE);
            }
            if (!fields.isEmpty()) {
                for (TemplateFieldDefinition fieldDef : fields) {
                    sourceBuilder.append(NEW_LINE).append(fieldDef.getInterfaceBuilderFieldDeclarationSource(2, imports));
                }
                sourceBuilder.append(NEW_LINE);
            }

            // Builder class constructor javadocs
            sourceBuilder.append(NEW_LINE);
            sourceBuilder.append(new TemplateJavadocsBuilder()
                    .startParagraph()
                    .add("Creates a builder for ").addLink(getClassName()).add(" objects.")
                    .endParagraph()
                    .buildJavadocsSource(2));

            // Builder class constructor declaration
            sourceBuilder.append(indent(2)).append("public Builder() {").append(NEW_LINE);
            sourceBuilder.append(indent(2)).append("}").append(NEW_LINE);

            // Builder class deprecated ViewRequest constructor
            if (!removeDeprecations) {
                sourceBuilder.append(NEW_LINE);
                sourceBuilder.append(new TemplateJavadocsBuilder()
                        .addLine("@deprecated use {@link #Builder()} instead.")
                        .buildJavadocsSource(2));
                sourceBuilder.append(indent(2)).append("@Deprecated").append(NEW_LINE);
                sourceBuilder.append(indent(2)).append("public Builder(ViewRequest request) {").append(NEW_LINE);
                {
                    sourceBuilder.append(indent(3)).append("this.request = request;").append(NEW_LINE);
                }
                sourceBuilder.append(indent(2)).append("}").append(NEW_LINE);
            }

            // Builder class builder methods for each field
            for (TemplateFieldDefinition fieldDef : fields) {
                sourceBuilder.append(NEW_LINE);
                sourceBuilder.append(fieldDef.getInterfaceBuilderMethodImplementationSource(2, imports, removeDeprecations));
                sourceBuilder.append(NEW_LINE);
            }

            sourceBuilder.append(NEW_LINE);

            // Javadocs for the Builder build method.
            sourceBuilder.append(new TemplateJavadocsBuilder()
                    .startParagraph().add("Builds the ").addLink(getClassName()).add(".").endParagraph()
                    .newLine()
                    .addReturn().add("The fully built ").addLink(getClassName()).add(".")
                    .buildJavadocsSource(2));

            sourceBuilder.append(indent(2)).append("public ").append(getClassName()).append(" build() {\n");
            {
                sourceBuilder.append(indent(3)).append("return new ").append(getClassName()).append("() {\n");
                for (TemplateFieldDefinition fieldDef : fields) {
                    sourceBuilder.append(NEW_LINE).append(fieldDef.getInterfaceBuilderBuildMethodSource(4, imports)).append(NEW_LINE);
                }
                sourceBuilder.append(indent(3)).append("};").append(NEW_LINE);
            }
            // End of build method
            sourceBuilder.append(indent(2)).append("};").append(NEW_LINE);
        }
        // End of Builder class
        sourceBuilder.append(indent(1)).append("};").append(NEW_LINE);

        // Enf of view interface class
        sourceBuilder.append(indent(0)).append("};").append(NEW_LINE);

        String javaSource = sourceBuilder.toString();

        String importsSource = getJavaImportStatements(imports);

        javaSource = javaSource.replace("${importsPlaceholder}", importsSource);

        return javaSource;
    }

    private String getJavaPackageName() {
        return javaPackageName;
    }

    private String getJavaClassName() {
        if (javaClassNamePrefix == null) {
            javaClassNamePrefix = "";
        }

        String className = name;

        if (name.contains(".")) {
            int lastDotAt = name.lastIndexOf('.');
            if (lastDotAt > 0) {
                className = name.substring(lastDotAt + 1);
            }
        } else {
            int lastSlashAt = name.lastIndexOf('/');
            if (lastSlashAt >= 0) {
                className = name.substring(lastSlashAt + 1);
            }
        }

        return javaClassNamePrefix + StyleguideStringUtils.toJavaClassCase(className) + "View";
    }

    private String getJavaImportStatements(Set<String> imports) {

        StringBuilder builder = new StringBuilder();

        for (String importClass : imports) {
            builder.append("import ").append(importClass).append(";\n");
        }

        return builder.toString();
    }

    private List<TemplateFieldDefinition> getImplementedTemplateFieldDefinitions() {

        List<TemplateFieldDefinition> implementedFieldDefs = new ArrayList<>();

        for (TemplateDefinition td : templateDefinitions.get()) {

            for (TemplateFieldDefinition tfd : td.getFields()) {

                Set<TemplateFieldType> fieldValueTypes = tfd.getFieldValueTypes();
                // TODO: Should actually implement equals/hashCode rather than relying on Object impl.
                if (fieldValueTypes.size() > 1 && fieldValueTypes.contains(this)) {
                    implementedFieldDefs.add(tfd);
                }
            }
        }

        return implementedFieldDefs;
    }

    private String getImplementedTemplateFieldDefinitions(Set<String> imports) {

        List<String> classNames = new ArrayList<>();

        for (TemplateFieldDefinition fieldDef : getImplementedTemplateFieldDefinitions()) {

            if (fieldDef instanceof TemplateFieldType) {

                TemplateFieldType fieldType = (TemplateFieldType) fieldDef;

                if (!this.hasSamePackageAs(fieldType)) {
                    // add its parent's fully qualified class name
                    imports.add(fieldDef.getParentTemplate().getFullyQualifiedClassName());
                }

                classNames.add(fieldType.getClassName());
            }
        }

        return classNames.isEmpty() ? "" : " extends " + classNames.stream().sorted().collect(Collectors.joining(", "));
    }

    /**
     * @return true only if all templates specify a JSON format (they should be consistent)
     */
    private boolean isJsonFormat() {
        return jsonTemplateObjects != null
            && jsonTemplateObjects.stream().allMatch(template -> template.getTemplateFormat() == JsonTemplateObject.TemplateFormat.Json);
    }

    // Helper method to convert the String "com.package.name.ClassName" --> "ClassName"
    private static String toSimpleClassName(String className) {

        int lastDotAt = className.lastIndexOf('.');

        if (lastDotAt >= 0) {
            className = className.substring(lastDotAt + 1);
        }

        return className;
    }

    // Checks if a class exists for the given className.
    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
