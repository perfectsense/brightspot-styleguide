package com.psddev.styleguide.viewgenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import com.psddev.styleguide.JsonObject;
import com.psddev.styleguide.JsonTemplateObject;

import static com.psddev.styleguide.viewgenerator.ViewClassStringUtils.indent;
import static com.psddev.styleguide.viewgenerator.ViewClassStringUtils.NEW_LINE;

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
                            javaClassNamePrefix,
                            /* isDefaulted */ false,
                            /* isStrictlyTyped */ true))

                    .sorted((tfd1, tfd2) -> ObjectUtils.compare(tfd1.getName(), tfd2.getName(), true))

                    .forEach(fields::add);

            isFieldsResolved = true;
        }
    }

    public List<ViewClassSource> getViewClassSources() {
        List<ViewClassSource> sources = new ArrayList<>();
        sources.addAll(getFieldLevelInterfaceSources());
        sources.add(getViewClassSource());
        return sources;
    }

    // Standard messaging for auto-generated file header.
    private String getSourceCodeHeaderComment() {
        return new ViewClassJavadocsBuilder()
                .addLine("AUTO-GENERATED FILE.  DO NOT MODIFY.")
                .newLine()
                .addLine("This class was automatically generated by the Maven build tool based on")
                .addLine("discovered JSON data files. It should NOT be modified by hand nor checked")
                .addLine("into source control.")
                .buildCommentsSource(0);
    }

    private List<ViewClassSource> getFieldLevelInterfaceSources() {

        List<ViewClassSource> fieldLevelInterfaceSources = new ArrayList<>();

        // Field level interfaces
        for (TemplateFieldDefinition fieldDef : fields) {

            Set<TemplateFieldType> fieldValueTypes = fieldDef.getFieldValueTypes();

            // Only create the view field level interface if there's more than one field value type
            if (fieldValueTypes.size() > 1 && fieldDef instanceof TemplateFieldType) {

                StringBuilder sourceBuilder = new StringBuilder();

                sourceBuilder.append(getSourceCodeHeaderComment());

                // Package declaration
                sourceBuilder.append("package ").append(javaPackageName).append(";").append(NEW_LINE);
                sourceBuilder.append(NEW_LINE);

                // Adds javadocs if it exists (which it should).
                sourceBuilder.append(new ViewClassJavadocsBuilder()
                        .startParagraph()
                        .add("Field level interface for the return type of ")
                        .addLink(fieldDef.getParentTemplate().getClassName() + "#" + fieldDef.getJavaInterfaceMethodName() + "()")
                        .add(". ")
                        .newLine()
                        .addFieldValueTypesSnippet(fieldDef)
                        .endParagraph()
                        .buildJavadocsSource(0));

                // field level interface declaration
                sourceBuilder.append(indent(0)).append("public interface ").append(((TemplateFieldType) fieldDef).getClassName()).append(" {").append(NEW_LINE);
                sourceBuilder.append(indent(0)).append("}").append(NEW_LINE);

                fieldLevelInterfaceSources.add(new ViewClassSource(
                        getPackageName(),
                        ((TemplateFieldType) fieldDef).getClassName(),
                        sourceBuilder.toString()));
            }
        }

        return fieldLevelInterfaceSources;
    }

    private String getViewRendererAnnotation(ViewClassImportsBuilder importsBuilder) {

        StringBuilder builder = new StringBuilder();

        final String handlebarsTemplateClassName = "com.psddev.handlebars.HandlebarsTemplate";
        final String jsonTemplateClassName = "com.psddev.cms.view.JsonView";

        if (isJsonFormat()) {
            importsBuilder.add(jsonTemplateClassName);
            builder.append("@");
            builder.append(TemplateFieldType.from(jsonTemplateClassName).getClassName());
            builder.append(NEW_LINE);
        } else {
            importsBuilder.add(handlebarsTemplateClassName);
            builder.append("@");
            builder.append(TemplateFieldType.from(handlebarsTemplateClassName).getClassName());
            builder.append("(\"").append(StringUtils.removeStart(name, "/")).append("\")");
            builder.append(NEW_LINE);
        }

        return builder.toString();
    }

    private ViewClassSource getViewClassSource() {

        StringBuilder sourceBuilder = new StringBuilder();
        ViewClassImportsBuilder importsBuilder = new ViewClassImportsBuilder(this);

        // File header
        sourceBuilder.append(getSourceCodeHeaderComment());

        // Package declaration
        sourceBuilder.append("package ").append(javaPackageName).append(";").append(NEW_LINE);
        sourceBuilder.append(NEW_LINE);

        // Imports - we collect them as we process, so this just a placeholder which we'll replace at the end.
        sourceBuilder.append(ViewClassImportsBuilder.PLACEHOLDER);
        sourceBuilder.append(NEW_LINE);

        // JSON generated class level javadocs
        if (!notes.isEmpty()) {
            ViewClassJavadocsBuilder classNotesBuilder = new ViewClassJavadocsBuilder();
            notes.forEach(classNotesBuilder::addParagraph);
            sourceBuilder.append(classNotesBuilder.buildJavadocsSource(0));
        }

        // Annotations
        // ViewInterface annotation
        final String viewInterfaceClassName = "com.psddev.cms.view.ViewInterface";
        if (importsBuilder.addIfExists(viewInterfaceClassName)) {
            sourceBuilder.append("@").append(TemplateFieldType.from(viewInterfaceClassName).getClassName()).append(NEW_LINE);
        }
        // ViewRenderer annotation
        sourceBuilder.append(getViewRendererAnnotation(importsBuilder));

        // Interface declaration / Ex: public interface ExampleView extends .... {
        sourceBuilder.append(getViewInterfaceDeclaration(importsBuilder));

        // Static view type/element constants
        for (TemplateFieldDefinition fieldDef : fields) {

            if (fieldDef instanceof TemplateFieldDefinitionList
                    || fieldDef instanceof TemplateFieldDefinitionObject
                    || fieldDef instanceof TemplateFieldDefinitionString) {

                String declaration = fieldDef.getInterfaceStaticStringVariableDeclaration(1, "_ELEMENT");
                sourceBuilder.append(NEW_LINE).append(declaration).append(NEW_LINE);
            }
        }

        // Interface method declarations
        for (TemplateFieldDefinition fieldDef : fields) {
            sourceBuilder.append(NEW_LINE);
            sourceBuilder.append(fieldDef.getInterfaceMethodDeclarationSource(1, importsBuilder));
            sourceBuilder.append(NEW_LINE);
        }

        // Builder class
        sourceBuilder.append(NEW_LINE);

        // Builder class javadocs
        sourceBuilder.append(new ViewClassJavadocsBuilder()
                .startParagraph()
                .add("Builder of ")
                .addLink(getClassName())
                .add(" objects.")
                .endParagraph()
                .buildJavadocsSource(1));

        // Builder class declaration
        sourceBuilder.append(indent(1)).append("class Builder {").append(NEW_LINE);
        {
            if (!fields.isEmpty()) {
                for (TemplateFieldDefinition fieldDef : fields) {
                    sourceBuilder.append(NEW_LINE).append(fieldDef.getInterfaceBuilderFieldDeclarationSource(2, importsBuilder));
                }
                sourceBuilder.append(NEW_LINE);
            }

            // Builder class constructor javadocs
            sourceBuilder.append(NEW_LINE);
            sourceBuilder.append(new ViewClassJavadocsBuilder()
                    .startParagraph()
                    .add("Creates a builder for ").addLink(getClassName()).add(" objects.")
                    .endParagraph()
                    .buildJavadocsSource(2));

            // Builder class constructor declaration
            sourceBuilder.append(indent(2)).append("public Builder() {").append(NEW_LINE);
            sourceBuilder.append(indent(2)).append("}").append(NEW_LINE);

            // Builder class builder methods for each field
            for (TemplateFieldDefinition fieldDef : fields) {
                sourceBuilder.append(NEW_LINE);
                sourceBuilder.append(fieldDef.getInterfaceBuilderMethodImplementationSource(2, importsBuilder));
                sourceBuilder.append(NEW_LINE);
            }

            sourceBuilder.append(NEW_LINE);

            // Javadocs for the Builder build method.
            sourceBuilder.append(new ViewClassJavadocsBuilder()
                    .startParagraph().add("Builds the ").addLink(getClassName()).add(".").endParagraph()
                    .newLine()
                    .addReturn().add("The fully built ").addLink(getClassName()).add(".")
                    .buildJavadocsSource(2));

            sourceBuilder.append(indent(2)).append("public ").append(getClassName()).append(" build() {\n");
            {
                sourceBuilder.append(indent(3)).append("return new ").append(getClassName()).append("() {\n");
                for (TemplateFieldDefinition fieldDef : fields) {
                    sourceBuilder.append(NEW_LINE).append(fieldDef.getInterfaceBuilderBuildMethodSource(4, importsBuilder)).append(NEW_LINE);
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

        String importsSource = importsBuilder.getImportStatements();

        javaSource = javaSource.replace("${importsPlaceholder}", importsSource);

        return new ViewClassSource(getPackageName(), getClassName(), javaSource);
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

        return javaClassNamePrefix + ViewClassStringUtils.toJavaClassCase(className) + "View";
    }

    private List<TemplateFieldDefinition> getImplementedTemplateFieldDefinitions() {

        List<TemplateFieldDefinition> implementedFieldDefs = new ArrayList<>();

        for (TemplateDefinition td : templateDefinitions.get()) {

            for (TemplateFieldDefinition tfd : td.getFields()) {

                Set<TemplateFieldType> fieldValueTypes = tfd.getFieldValueTypes();
                if (fieldValueTypes.size() > 1 && fieldValueTypes.stream()
                        .map(TemplateFieldType::getFullyQualifiedClassName)
                        .filter(cn -> cn.equals(this.getFullyQualifiedClassName()))
                        .findAny()
                        .isPresent()) {

                    implementedFieldDefs.add(tfd);
                }
            }
        }

        return implementedFieldDefs;
    }

    private String getViewInterfaceDeclaration(ViewClassImportsBuilder importsBuilder) {

        List<String> classNames = new ArrayList<>();

        for (TemplateFieldDefinition fieldDef : getImplementedTemplateFieldDefinitions()) {

            if (fieldDef instanceof TemplateFieldType) {

                TemplateFieldType fieldType = (TemplateFieldType) fieldDef;

                importsBuilder.add(fieldType);

                classNames.add(fieldType.getClassName());
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("public interface ");
        builder.append(getClassName());

        if (!classNames.isEmpty()) {
            builder.append(" extends ");

            char[] spaces = new char[builder.length()];
            Arrays.fill(spaces, ' ');
            String indent = new String(spaces);

            builder.append(classNames.stream().sorted().collect(Collectors.joining("," + NEW_LINE + indent)));
        }

        builder.append(" {");
        builder.append(NEW_LINE);

        return builder.toString();
    }

    /**
     * @return true only if all templates specify a JSON format (they should be consistent)
     */
    private boolean isJsonFormat() {
        return jsonTemplateObjects != null
            && jsonTemplateObjects.stream().allMatch(template -> template.getTemplateFormat() == JsonTemplateObject.TemplateFormat.Json);
    }
}
