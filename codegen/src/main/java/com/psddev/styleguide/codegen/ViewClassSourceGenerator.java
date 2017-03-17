package com.psddev.styleguide.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.StringUtils;

import static com.psddev.styleguide.codegen.ViewClassStringUtils.NEW_LINE;
import static com.psddev.styleguide.codegen.ViewClassStringUtils.indent;

/**
 * Responsible for generating the source code for a single view class
 * definition. It's possible that a single view class definition results in
 * multiple source files being generated in the case where field level
 * interfaces are also needed.
 */
class ViewClassSourceGenerator {

    private ViewClassGeneratorContext context;

    private ViewClassDefinition classDef;

    private ViewClassImportsBuilder importsBuilder;

    /**
     * Creates a new view class source generator for the given {@code classDef}
     * in the given {@code context}.
     *
     * @param context the view class generation context.
     * @param classDef the view class definition that the source code will be
     *                 generated from.
     */
    public ViewClassSourceGenerator(ViewClassGeneratorContext context,
                                    ViewClassDefinition classDef) {
        this.context = context;
        this.classDef = classDef;
        this.importsBuilder = new ViewClassImportsBuilder(classDef);
    }

    /**
     * Generates the list of view class source objects that represent the
     * underlying view class definition for this generator.
     *
     * @return the list of sources.
     */
    public List<ViewClassSource> generateSources() {

        List<ViewClassSource> sources = new ArrayList<>();

        sources.add(getViewClassSource());
        sources.addAll(getFieldLevelInterfaceSources());

        return sources;
    }

    /*
     * Generates the sources for all field level interfaces of the underlying
     * view class definition.
     */
    private List<ViewClassSource> getFieldLevelInterfaceSources() {

        List<ViewClassSource> fieldLevelInterfaceSources = new ArrayList<>();

        // Field level interfaces
        for (ViewClassFieldDefinition fieldDef : classDef.getNonNullFieldDefinitions()) {

            // Only create the view field level interface if there's more than one field value type
            if (!(fieldDef.getEffectiveValueType() instanceof ViewClassFieldNativeJavaType)) {

                StringBuilder sourceBuilder = new StringBuilder();

                sourceBuilder.append(getSourceCodeHeaderComment());

                // Package declaration
                sourceBuilder.append("package ").append(classDef.getPackageName()).append(";").append(NEW_LINE);
                sourceBuilder.append(NEW_LINE);

                // Adds javadocs if it exists (which it should).
                ViewClassJavadocsBuilder javadocs = new ViewClassJavadocsBuilder();

                javadocs.startParagraph();
                if (fieldDef.isAbstract()) {
                    javadocs.add("An <b>abstract</b> field");
                } else {
                    javadocs.add("Field");
                }
                javadocs.add(" level interface for the return type of ");
                javadocs.addLink(fieldDef.getClassDefinition().getClassName() + "#" + getJavaInterfaceMethodName(fieldDef) + "()");
                javadocs.add(".");
                if (!fieldDef.isAbstract()) {
                    javadocs.add(" ");
                    javadocs.newLine();
                    javadocs.addFieldValueTypesSnippet(fieldDef);
                }
                javadocs.endParagraph();

                sourceBuilder.append(javadocs.buildJavadocsSource(0));

                // field level interface declaration
                sourceBuilder.append(indent(0)).append("public interface ").append(fieldDef.getClassName()).append(" {").append(NEW_LINE);
                sourceBuilder.append(indent(0)).append("}").append(NEW_LINE);

                fieldLevelInterfaceSources.add(new ViewClassSource(
                        classDef.getPackageName(),
                        fieldDef.getClassName(),
                        sourceBuilder.toString()));
            }
        }

        return fieldLevelInterfaceSources;
    }

    /*
     * Generates the main source for the underlying view class definition.
     */
    private ViewClassSource getViewClassSource() {

        List<ViewClassFieldDefinition> fieldDefs = classDef.getNonNullFieldDefinitions();

        StringBuilder sourceBuilder = new StringBuilder();

        // File header
        sourceBuilder.append(getSourceCodeHeaderComment());

        // Package declaration
        sourceBuilder.append("package ").append(classDef.getPackageName()).append(";").append(NEW_LINE);
        sourceBuilder.append(NEW_LINE);

        // Imports - we collect them as we process, so this just a placeholder which we'll replace at the end.
        sourceBuilder.append(ViewClassImportsBuilder.PLACEHOLDER);
        sourceBuilder.append(NEW_LINE);

        // JSON generated class level javadocs
        ViewClassJavadocsBuilder classNotesBuilder = new ViewClassJavadocsBuilder();
        classNotesBuilder.addClassOccurrencesList(classDef);
        classDef.getNotes().forEach(classNotesBuilder::addParagraph);
        sourceBuilder.append(classNotesBuilder.buildJavadocsSource(0));

        // Annotations

        // ViewInterface annotation
        final String viewInterfaceClassName = "com.psddev.cms.view.ViewInterface";
        importsBuilder.add(viewInterfaceClassName);
        sourceBuilder.append("@").append(ViewClassFieldType.from(viewInterfaceClassName).getClassName()).append(NEW_LINE);

        // ViewRenderer annotation
        sourceBuilder.append(getViewRendererAnnotation());

        // Interface declaration / Ex: public interface ExampleView extends .... {
        sourceBuilder.append(getViewInterfaceDeclaration());

        // Static view type/element constants
        /*
        for (ViewClassFieldDefinition fieldDef : fieldDefs) {

            Class<? extends JsonValue> effectiveType = fieldDef.getEffectiveType();

            if (effectiveType == JsonList.class || effectiveType == JsonViewMap.class) {
                String declaration = getInterfaceStaticStringVariableDeclaration(fieldDef, 1, "_ELEMENT");
                sourceBuilder.append(NEW_LINE).append(declaration).append(NEW_LINE);
            }
        }
        */

        // Interface method declarations
        for (ViewClassFieldDefinition fieldDef : fieldDefs) {
            sourceBuilder.append(NEW_LINE);
            sourceBuilder.append(getInterfaceMethodDeclarationSource(fieldDef, 1));
            sourceBuilder.append(NEW_LINE);
        }

        // Builder class
        sourceBuilder.append(NEW_LINE);

        // Builder class javadocs
        sourceBuilder.append(new ViewClassJavadocsBuilder()
                .startParagraph()
                .add("Builder of ")
                .addLink(classDef.getClassName())
                .add(" objects.")
                .endParagraph()
                .buildJavadocsSource(1));

        // Builder class declaration
        sourceBuilder.append(indent(1)).append("class Builder {").append(NEW_LINE);
        {

            if (!fieldDefs.isEmpty()) {
                for (ViewClassFieldDefinition fieldDef : fieldDefs) {
                    sourceBuilder.append(NEW_LINE).append(getInterfaceBuilderFieldDeclarationSource(fieldDef, 2));
                }
                sourceBuilder.append(NEW_LINE);
            }

            // Builder class constructor javadocs
            sourceBuilder.append(NEW_LINE);
            sourceBuilder.append(new ViewClassJavadocsBuilder()
                    .startParagraph()
                    .add("Creates a builder for ").addLink(classDef.getClassName()).add(" objects.")
                    .endParagraph()
                    .buildJavadocsSource(2));

            // Builder class constructor declaration
            sourceBuilder.append(indent(2)).append("public Builder() {").append(NEW_LINE);
            sourceBuilder.append(indent(2)).append("}").append(NEW_LINE);

            // Builder class builder methods for each field
            for (ViewClassFieldDefinition fieldDef : fieldDefs) {
                sourceBuilder.append(NEW_LINE);
                sourceBuilder.append(getInterfaceBuilderMethodImplementationSource(fieldDef, 2));
                sourceBuilder.append(NEW_LINE);
            }

            sourceBuilder.append(NEW_LINE);

            // Javadocs for the Builder build method.
            sourceBuilder.append(new ViewClassJavadocsBuilder()
                    .startParagraph().add("Builds the ").addLink(classDef.getClassName()).add(".").endParagraph()
                    .newLine()
                    .addReturn().add("The fully built ").addLink(classDef.getClassName()).add(".")
                    .buildJavadocsSource(2));

            sourceBuilder.append(indent(2)).append("public ").append(classDef.getClassName()).append(" build() {\n");
            {
                sourceBuilder.append(indent(3)).append("return new ").append(classDef.getClassName()).append("() {\n");
                for (ViewClassFieldDefinition fieldDef : fieldDefs) {
                    sourceBuilder.append(NEW_LINE).append(getInterfaceBuilderBuildMethodSource(fieldDef, 4)).append(NEW_LINE);
                }
                sourceBuilder.append(indent(3)).append("};").append(NEW_LINE);
            }
            // End of build method
            sourceBuilder.append(indent(2)).append("}").append(NEW_LINE);
        }
        // End of Builder class
        sourceBuilder.append(indent(1)).append("}").append(NEW_LINE);

        // Enf of view interface class
        sourceBuilder.append(indent(0)).append("}").append(NEW_LINE);

        String javaSource = sourceBuilder.toString();

        String importsSource = importsBuilder.getImportStatements();

        javaSource = javaSource.replace("${importsPlaceholder}", importsSource);

        return new ViewClassSource(classDef.getPackageName(), classDef.getClassName(), javaSource);
    }

    /*
     * Standard messaging for auto-generated file header.
     */
    private String getSourceCodeHeaderComment() {
        return new ViewClassJavadocsBuilder()
                .addLine("AUTO-GENERATED FILE.  DO NOT MODIFY.")
                .newLine()
                .addLine("This class was automatically generated by the Maven build tool based on")
                .addLine("discovered JSON data files. It should NOT be modified by hand nor checked")
                .addLine("into source control.")
                .buildCommentsSource(0);
    }

    /*
     * Creates the view renderer annotation to be placed at the top of the class.
     */
    private String getViewRendererAnnotation() {

        ViewKey viewKey = classDef.getViewKey();

        StringBuilder builder = new StringBuilder();

        String annotationClass = viewKey.getAnnotationClass();

        if (annotationClass != null) {

            importsBuilder.add(annotationClass);

            builder.append("@");
            builder.append(ViewClassFieldType.from(annotationClass).getClassName());

            Map<String, String> annotationArguments = viewKey.getAnnotationArguments();
            if (!annotationArguments.isEmpty()) {

                int argsSize = annotationArguments.size();

                builder.append("(");

                for (Iterator<Map.Entry<String, String>> it = annotationArguments.entrySet().iterator(); it.hasNext();) {

                    Map.Entry<String, String> arg = it.next();

                    String key = arg.getKey();
                    String value = arg.getValue();

                    if (!"value".equals(key) || argsSize != 1) {
                        builder.append(key);
                        builder.append(" = ");
                    }

                    builder.append("\"");
                    builder.append(value);
                    builder.append("\"");

                    if (it.hasNext()) {
                        builder.append(", ");
                    }
                }

                builder.append(")");
            }

            builder.append(NEW_LINE);
        }

        return builder.toString();
    }

    /*
     * Generates the class name declaration along with all the interfaces that
     * it implements.
     */
    private String getViewInterfaceDeclaration() {

        List<String> classNames = new ArrayList<>();

        for (ViewClassFieldDefinition fieldDef : getImplementedTemplateFieldDefinitions()) {
            if (importsBuilder.add(fieldDef)) {
                classNames.add(fieldDef.getLocalClassName());
            } else {
                classNames.add(fieldDef.getFullyQualifiedClassName());
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("public interface ");
        builder.append(classDef.getClassName());

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

    /*
     * Gets the list of view class field definitions that the underlying view
     * class definition should implement.
     */
    private List<ViewClassFieldDefinition> getImplementedTemplateFieldDefinitions() {

        List<ViewClassFieldDefinition> implementedFieldDefs = new ArrayList<>();

        for (ViewClassDefinition classDef : context.getClassDefinitions()) {

            for (ViewClassFieldDefinition fieldDef : classDef.getNonNullFieldDefinitions()) {

                if (fieldDef.getFieldValueTypes().stream()
                        .map(ViewClassFieldType::getFullyQualifiedClassName)
                        .anyMatch(fqcn -> fqcn.equals(this.classDef.getFullyQualifiedClassName()))) {

                    implementedFieldDefs.add(fieldDef);
                }
            }
        }

        return implementedFieldDefs;
    }

    /*
     * Generates the static variables that can be used as the value of the
     * "types" argument in the @ViewBinding annotation.
     */
    private String getInterfaceStaticStringVariableDeclaration(ViewClassFieldDefinition fieldDef, int indent, String suffix) {

        String varName = StringUtils.toUnderscored(fieldDef.getFieldName()).toUpperCase() + suffix;
        String varValue = fieldDef.getFieldName();

        String declaration = "";

        declaration += indent(indent) + "static final String " + varName + " = \"" + varValue + "\";";

        return declaration;
    }

    /*
     * Generates the interface method declarations for a given field at the
     * specified indent.
     */
    private String getInterfaceMethodDeclarationSource(ViewClassFieldDefinition fieldDef, int indent) {

        // collect the methods' javadocs
        ViewClassJavadocsBuilder methodJavadocs = new ViewClassJavadocsBuilder();

        fieldDef.getNotes().forEach(methodJavadocs::addParagraph);

        methodJavadocs.startParagraph();

        if (fieldDef.getEffectiveType() == JsonList.class) {
            methodJavadocs.addCollectionFieldValueTypesSnippet(fieldDef);
        } else {
            methodJavadocs.addFieldValueTypesSnippet(fieldDef);
        }

        methodJavadocs.endParagraph();

        methodJavadocs.addFieldOccurrencesList(fieldDef);

        methodJavadocs.addSampleValuesList(fieldDef, 10);

        boolean isDefaulted = context.isGenerateDefaultMethods() || fieldDef.getEffectiveType() == JsonMap.class;

        // if it's a default interface method just make the body return null;
        String methodBody = !isDefaulted ? ";" : (" {\n"
                + indent(indent + 1) + "return null;\n"
                + indent(indent) + "}");

        return methodJavadocs.buildJavadocsSource(indent)
                + indent(indent) + (isDefaulted ? "default " : "") + getJavaFieldType(fieldDef) + " " + getJavaInterfaceMethodName(fieldDef) + "()" + methodBody;
    }

    /*
     * Generates the fields that are used to bower the view interface builder
     * class.
     */
    private String getInterfaceBuilderFieldDeclarationSource(ViewClassFieldDefinition fieldDef, int indent) {
        return indent(indent) + "private " + getJavaFieldTypeForBuilder(fieldDef) + " " + fieldDef.getFieldName() + ";";
    }

    /*
     * Gets the Java field type for a given field definition used as the return
     * type for the interface methods.
     */
    private String getJavaFieldType(ViewClassFieldDefinition fieldDef) {

        Class<? extends JsonValue> effectiveType = fieldDef.getEffectiveType();

        if (effectiveType == JsonBoolean.class) {
            return ViewClassFieldNativeJavaType.BOOLEAN.getClassName();

        } else if (effectiveType == JsonNumber.class) {
            return ViewClassFieldNativeJavaType.NUMBER.getClassName();

        } else if (effectiveType == JsonString.class) {
            ViewClassFieldType type;
            if (context.isGenerateStrictTypes()) {
                type = ViewClassFieldNativeJavaType.CHAR_SEQUENCE;
            } else {
                type = ViewClassFieldNativeJavaType.OBJECT;
            }

            importsBuilder.add(type);
            return type.getClassName();

        } else if (effectiveType == JsonMap.class) {
            importsBuilder.add(ViewClassFieldNativeJavaType.MAP);
            importsBuilder.add(ViewClassFieldNativeJavaType.STRING);
            importsBuilder.add(ViewClassFieldNativeJavaType.OBJECT);
            return String.format("%s<%s, %s>",
                    ViewClassFieldNativeJavaType.MAP.getClassName(),
                    ViewClassFieldNativeJavaType.STRING.getClassName(),
                    ViewClassFieldNativeJavaType.OBJECT.getClassName());

        } else if (effectiveType == JsonViewMap.class) {
            ViewClassFieldType fieldType = fieldDef.getEffectiveValueType();
            if (importsBuilder.add(fieldType)) {
                return fieldType.getLocalClassName();
            } else {
                return fieldType.getFullyQualifiedClassName();
            }

        } else if (effectiveType == JsonList.class) {

            importsBuilder.add(ViewClassFieldNativeJavaType.COLLECTION);

            String genericArgument;

            ViewClassFieldType effectiveFieldValueType = fieldDef.getEffectiveValueType();

            if (effectiveFieldValueType == null || effectiveFieldValueType.contentEquals(ViewClassFieldNativeJavaType.OBJECT)) {
                genericArgument = "?";

            } else {
                if (importsBuilder.add(effectiveFieldValueType)) {
                    genericArgument = "? extends " + effectiveFieldValueType.getLocalClassName();
                } else {
                    genericArgument = "? extends " + effectiveFieldValueType.getFullyQualifiedClassName();
                }
            }

            return ViewClassFieldNativeJavaType.COLLECTION.getClassName() + "<" + genericArgument + ">";

        } else {
            throw new IllegalStateException("Field definitions must have a valid effective type!");
        }
    }

    /*
     * Gets the Java field type for a given field definition used for the
     * builder instance variables and/or builder method arguments. This is
     * slightly different than its {@link #getJavaFieldType} counter part with
     * regard to collections.
     */
    private String getJavaFieldTypeForBuilder(ViewClassFieldDefinition fieldDef) {

        if (fieldDef.getEffectiveType() == JsonList.class) {
            importsBuilder.add(ViewClassFieldNativeJavaType.COLLECTION);

            // Collection<?> --> Collection<Object>
            // Collection<? extends Foo> --> Collection<Foo>
            return getJavaFieldType(fieldDef).replace("? extends ", "").replace("?", ViewClassFieldNativeJavaType.OBJECT.getClassName());

        } else {
            return getJavaFieldType(fieldDef);
        }
    }

    /*
     * Gets all the builder related methods for the give field definition at the
     * specified indent.
     */
    private String getInterfaceBuilderMethodImplementationSource(ViewClassFieldDefinition fieldDef, int indent) {

        StringBuilder builder = new StringBuilder();

        Class<? extends JsonValue> effectiveType = fieldDef.getEffectiveType();

        String fieldName = fieldDef.getFieldName();

        Set<String> notes = fieldDef.getNotes();

        // Lists have a specialized set of builder methods.
        if (effectiveType == JsonList.class) {

            importsBuilder.add(ArrayList.class.getName());

            ViewClassJavadocsBuilder method1Javadocs = new ViewClassJavadocsBuilder();
            method1Javadocs.addParagraph("Sets the " + fieldName + " field.");
            notes.forEach(method1Javadocs::addParagraph);
            method1Javadocs.addFieldOccurrencesList(fieldDef);
            method1Javadocs.addSampleValuesList(fieldDef, 10);
            method1Javadocs.newLine();
            method1Javadocs.addParameter(fieldName).addCollectionFieldValueTypesSnippet(fieldDef).newLine();
            method1Javadocs.addReturn().add("this builder.");

            String[] method1 = {
                    method1Javadocs.buildJavadocsSource(indent),
                    indent(indent) + "public Builder " + fieldName + "(" + getJavaFieldType(fieldDef) + " " + fieldName + ") {\n",
                    indent(indent + 1) + "this." + fieldName + " = " + fieldName + " != null ? new ArrayList<>(" + fieldName + ") : null;\n",
                    indent(indent + 1) + "return this;\n",
                    indent(indent) + "}"
            };

            /* *
             * Adds a single item to the articleBody field.
             *
             * @param articleBody the item to add, typically a {@link FigureView}.
             * @return this builder.
             */
            ViewClassJavadocsBuilder method2Javadocs = new ViewClassJavadocsBuilder();
            method2Javadocs.addParagraph("Adds a single item to the " + fieldName + " field.");
            notes.forEach(method2Javadocs::addParagraph);
            method2Javadocs.addFieldOccurrencesList(fieldDef);
            method2Javadocs.addSampleValuesList(fieldDef, 10);
            method2Javadocs.newLine();
            method2Javadocs.addParameter(fieldName).add("the item to add. ").addFieldValueTypesSnippet(fieldDef).newLine();
            method2Javadocs.addReturn().add("this builder.");
            /*
            public Builder addToAuthors(Object authors) { // OR if strictly typed --> public Builder addToAuthors(FigureView authors) {
                if (this.authors == null) {
                    this.authors = new ArrayList<>();
                }
                this.authors.add(authors);
                return this;
            }
             */
            String[] method2 = {
                    method2Javadocs.buildJavadocsSource(indent),
                    indent(indent) + "public Builder addTo" + ViewClassStringUtils.toJavaMethodCase(fieldName) + "(" + fieldDef.getEffectiveValueType().getLocalClassName() + " " + fieldName + ") {\n",
                    indent(indent + 1) + "if (this." + fieldName + " == null) {\n",
                    indent(indent + 2) + "this." + fieldName + " = new ArrayList<>();\n",
                    indent(indent + 1) + "}\n",
                    indent(indent + 1) + "this." + fieldName + ".add(" + fieldName + ");\n",
                    indent(indent + 1) + "return this;\n",
                    indent(indent) + "}"
            };

            /* **
             * Adds a Collection of items to the articleBody field.
             *
             * @param articleBody the items to add, typically a {@link FigureView}.
             * @return this builder.
             */
            ViewClassJavadocsBuilder method3Javadocs = new ViewClassJavadocsBuilder();
            method3Javadocs.addParagraph("Adds a Collection of items to the " + fieldName + " field.");
            notes.forEach(method3Javadocs::addParagraph);
            method3Javadocs.addFieldOccurrencesList(fieldDef);
            method3Javadocs.addSampleValuesList(fieldDef, 10);
            method3Javadocs.newLine();
            method3Javadocs.addParameter(fieldName).add("the items to add. ").addCollectionFieldValueTypesSnippet(fieldDef).newLine();
            method3Javadocs.addReturn().add("this builder.");
            /*
            public Builder addAllToAuthors(Collection<?> authors) {
                if (this.authors == null) {
                    this.authors = new ArrayList<>();
                }
                this.authors.addAll(authors);
                return this;
            }
             */
            String[] method3 = {
                    method3Javadocs.buildJavadocsSource(indent),
                    indent(indent) + "public Builder addAllTo" + ViewClassStringUtils.toJavaMethodCase(fieldName) + "(" + getJavaFieldType(fieldDef) + " " + fieldName + ") {\n",
                    indent(indent + 1) + "if (this." + fieldName + " == null) {\n",
                    indent(indent + 2) + "this." + fieldName + " = new ArrayList<>();\n",
                    indent(indent + 1) + "}\n",
                    indent(indent + 1) + "this." + fieldName + ".addAll(" + fieldName + ");\n",
                    indent(indent + 1) + "return this;\n",
                    indent(indent) + "}"
            };

            builder.append(Arrays.stream(method1).collect(Collectors.joining(""))).append("\n\n");

            builder.append(Arrays.stream(method2).collect(Collectors.joining(""))).append("\n\n");

            builder.append(Arrays.stream(method3).collect(Collectors.joining("")));

        } else {
            // All other types follow a similar pattern

            ViewClassJavadocsBuilder methodJavadocs = new ViewClassJavadocsBuilder();

            methodJavadocs.addParagraph("Sets the " + fieldName + " field.");
            notes.forEach(methodJavadocs::addParagraph);
            methodJavadocs.addFieldOccurrencesList(fieldDef);
            methodJavadocs.addSampleValuesList(fieldDef, 10);
            methodJavadocs.newLine();
            methodJavadocs.addParameter(fieldName).addFieldAwareValueTypesSnippet(fieldDef).newLine();
            methodJavadocs.addReturn().add("this builder.");

            methodJavadocs.buildJavadocsSource(indent);

            String[] method = {
                    methodJavadocs.buildJavadocsSource(indent),
                    indent(indent) + "public Builder " + fieldName + "(" + getJavaFieldType(fieldDef) + " " + fieldName + ") {\n",
                    indent(indent + 1) + "this." + fieldName + " = " + fieldName + ";\n",
                    indent(indent + 1) + "return this;\n",
                    indent(indent) + "}"
            };

            builder.append(Arrays.stream(method).collect(Collectors.joining("")));

            // Map has one additional method
            if (effectiveType == JsonMap.class) {
                builder.append("\n\n");

                importsBuilder.add(LinkedHashMap.class.getName());
                importsBuilder.add(Map.class.getName());

                ViewClassJavadocsBuilder javadocsBuilder = new ViewClassJavadocsBuilder();
                javadocsBuilder.add("Adds an entry to the " + fieldName + " field .").addLink("java.util.Map").addLine(".");
                notes.forEach(javadocsBuilder::addParagraph);
                javadocsBuilder.addFieldOccurrencesList(fieldDef);
                javadocsBuilder.addSampleValuesList(fieldDef, 10);
                javadocsBuilder.newLine();
                javadocsBuilder.addParameter("key").addLine("the key to add.");
                javadocsBuilder.addParameter("value").addLine("the value at the associated key.");
                javadocsBuilder.addReturn().add("this builder.");

                /*
                 * <p>Adds an entry to the attributes field {@link java.util.Map}.</p>
                 *
                 * @param key the key to set.
                 * @param value the value at the given key.
                 * @return this builder.
                 */
                /*
                public Builder addDisplayOptions(String key, Object value) {
                    if (this.displayOptions == null) {
                        this.displayOptions = new LinkedHashMap<>();
                    }
                    if (key != null) {
                        this.displayOptions.put(key, value);
                    }
                    return this;
                }
                */
                String[] mapAddMethod = {
                        javadocsBuilder.buildJavadocsSource(indent),
                        indent(indent) + "public Builder add" + ViewClassStringUtils.toJavaMethodCase(fieldName) + "(String key, Object value) {\n",
                        indent(indent + 1) + "if (this." + fieldName + " == null) {\n",
                        indent(indent + 2) + "this." + fieldName + " = new LinkedHashMap<>();\n",
                        indent(indent + 1) + "}\n",
                        indent(indent + 1) + "if (key != null) {\n",
                        indent(indent + 2) + "this." + fieldName + ".put(key, value);\n",
                        indent(indent + 1) + "}\n",
                        indent(indent + 1) + "return this;\n",
                        indent(indent) + "}"
                };

                builder.append(Arrays.stream(mapAddMethod).collect(Collectors.joining("")));
            }
        }

        return builder.toString();
    }

    /*
     * Gets the interface builder's build method source code for a given field
     * definition.
     */
    private String getInterfaceBuilderBuildMethodSource(ViewClassFieldDefinition fieldDef, int indent) {
        String[] method = {
                indent(indent) + "@Override\n",
                indent(indent) + "public " + getJavaFieldType(fieldDef) + " " + getJavaInterfaceMethodName(fieldDef) + "() {\n",
                indent(indent + 1) + "return " + fieldDef.getFieldName() + ";\n",
                indent(indent) + "}"
        };

        return Arrays.stream(method).collect(Collectors.joining(""));
    }

    /*
     * Gets the interface method name for a given field definition.
     */
    private String getJavaInterfaceMethodName(ViewClassFieldDefinition fieldDef) {
        return "get" + ViewClassStringUtils.toJavaMethodCase(fieldDef.getFieldName());
    }
}
