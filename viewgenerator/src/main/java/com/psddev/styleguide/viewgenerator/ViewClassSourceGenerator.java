package com.psddev.styleguide.viewgenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.StringUtils;

import static com.psddev.styleguide.viewgenerator.ViewClassStringUtils.NEW_LINE;
import static com.psddev.styleguide.viewgenerator.ViewClassStringUtils.indent;

class ViewClassSourceGenerator {

    private ViewClassGeneratorContext context;
    private ViewClassDefinition classDef;
    private ViewClassImportsBuilder importsBuilder;

    public ViewClassSourceGenerator(ViewClassGeneratorContext context,
                                    ViewClassDefinition classDef) {
        this.context = context;
        this.classDef = classDef;
        this.importsBuilder = new ViewClassImportsBuilder(classDef);
    }

    public List<ViewClassSource> generateSources() {

        List<ViewClassSource> sources = new ArrayList<>();

        sources.add(getViewClassSource());
        sources.addAll(getFieldLevelInterfaceSources());

        return sources;
    }

    private List<ViewClassSource> getFieldLevelInterfaceSources() {

        List<ViewClassSource> fieldLevelInterfaceSources = new ArrayList<>();

        // Field level interfaces
        for (ViewClassFieldDefinition fieldDef : classDef.getNonNullFieldDefinitions()) {

            Set<ViewClassFieldType> fieldValueTypes = fieldDef.getFieldValueTypes();

            // Only create the view field level interface if there's more than one field value type
            if (fieldValueTypes.size() > 1) {

                StringBuilder sourceBuilder = new StringBuilder();

                sourceBuilder.append(getSourceCodeHeaderComment());

                // Package declaration
                sourceBuilder.append("package ").append(classDef.getPackageName()).append(";").append(NEW_LINE);
                sourceBuilder.append(NEW_LINE);

                // Adds javadocs if it exists (which it should).
                sourceBuilder.append(new ViewClassJavadocsBuilder()
                        .startParagraph()
                        .add("Field level interface for the return type of ")
                        .addLink(fieldDef.getClassDefinition().getClassName() + "#" + getJavaInterfaceMethodName(fieldDef) + "()")
                        .add(". ")
                        .newLine()
                        .addFieldValueTypesSnippet(fieldDef)
                        .endParagraph()
                        .buildJavadocsSource(0));

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
        for (ViewClassFieldDefinition fieldDef : fieldDefs) {

            Class<? extends JsonValue> effectiveType = fieldDef.getEffectiveType();

            if (effectiveType == JsonList.class || effectiveType == JsonViewMap.class) {
                String declaration = getInterfaceStaticStringVariableDeclaration(fieldDef, 1, "_ELEMENT");
                sourceBuilder.append(NEW_LINE).append(declaration).append(NEW_LINE);
            }
        }

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

    private String getViewInterfaceDeclaration() {

        List<String> classNames = new ArrayList<>();

        for (ViewClassFieldDefinition fieldDef : getImplementedTemplateFieldDefinitions()) {
            importsBuilder.add(fieldDef);
            classNames.add(fieldDef.getClassName());
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

    private List<ViewClassFieldDefinition> getImplementedTemplateFieldDefinitions() {

        List<ViewClassFieldDefinition> implementedFieldDefs = new ArrayList<>();

        for (ViewClassDefinition classDef : context.getClassDefinitions()) {

            for (ViewClassFieldDefinition fieldDef : classDef.getNonNullFieldDefinitions()) {

                Set<ViewClassFieldType> fieldValueTypes = fieldDef.getFieldValueTypes();
                if (fieldValueTypes.size() > 1 && fieldValueTypes.stream()
                        .map(ViewClassFieldType::getFullyQualifiedClassName)
                        .filter(fqcn -> fqcn.equals(this.classDef.getFullyQualifiedClassName()))
                        .findAny()
                        .isPresent()) {

                    implementedFieldDefs.add(fieldDef);
                }
            }
        }

        return implementedFieldDefs;
    }

    private String getInterfaceStaticStringVariableDeclaration(ViewClassFieldDefinition fieldDef, int indent, String suffix) {

        String varName = StringUtils.toUnderscored(fieldDef.getFieldName()).toUpperCase() + suffix;
        String varValue = fieldDef.getFieldName();

        String declaration = "";

        declaration += indent(indent) + "static final String " + varName + " = \"" + varValue + "\";";

        return declaration;
    }

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

        methodJavadocs.addSampleStringValuesList(fieldDef, 5);

        boolean isDefaulted = context.isGenerateDefaultMethods();

        // if it's a default interface method just make the body return null;
        String methodBody = !isDefaulted ? ";" : (" {\n"
                + indent(indent + 1) + "return null;\n"
                + indent(indent) + "}");

        return methodJavadocs.buildJavadocsSource(indent)
                + indent(indent) + (isDefaulted ? "default " : "") + getJavaFieldType(fieldDef) + " " + getJavaInterfaceMethodName(fieldDef) + "()" + methodBody;
    }

    private String getInterfaceBuilderFieldDeclarationSource(ViewClassFieldDefinition fieldDef, int indent) {
        return indent(indent) + "private " + getJavaFieldTypeForBuilder(fieldDef) + " " + fieldDef.getFieldName() + ";";
    }

    private String getJavaFieldType(ViewClassFieldDefinition fieldDef) {

        Class<? extends JsonValue> effectiveType = fieldDef.getEffectiveType();

        if (effectiveType == JsonBoolean.class) {
            return "Boolean";

        } else if (effectiveType == JsonNumber.class) {
            return "Number";

        } else if (effectiveType == JsonString.class) {
            return context.isGenerateStrictTypes() ? "String" : "Object";

        } else if (effectiveType == JsonMap.class) {
            importsBuilder.add(Map.class.getName());
            return "Map<String, Object>";

        } else if (effectiveType == JsonViewMap.class) {
            ViewClassFieldType fieldType = fieldDef.getEffectiveValueType();
            importsBuilder.add(fieldType);
            return fieldType.getLocalClassName();

        } else if (effectiveType == JsonList.class) {

            importsBuilder.add(Collection.class.getName());

            String genericArgument;

            ViewClassFieldType effectiveFieldValueType = fieldDef.getEffectiveValueType();

            if (effectiveFieldValueType == null || effectiveFieldValueType == ViewClassFieldNativeJavaType.OBJECT) {
                genericArgument = "?";

            } else {
                // if the effective type lives in a different package, then import it.
                if (!fieldDef.hasSamePackageAs(effectiveFieldValueType)) {
                    importsBuilder.add(effectiveFieldValueType);
                }

                genericArgument = "? extends " + effectiveFieldValueType.getLocalClassName();
            }

            return "Collection<" + genericArgument + ">";

        } else {
            throw new IllegalStateException("Field definitions must have a valid effective type!");
        }
    }

    private String getJavaFieldTypeForBuilder(ViewClassFieldDefinition fieldDef) {

        if (fieldDef.getEffectiveType() == JsonList.class) {
            importsBuilder.add(Collection.class.getName());

            // Collection<?> --> Collection<Object>
            // Collection<? extends Foo> --> Collection<Foo>
            return getJavaFieldType(fieldDef).replace("? extends ", "").replace("?", "Object");

        } else {
            return getJavaFieldType(fieldDef);
        }
    }

    private String getInterfaceBuilderMethodImplementationSource(ViewClassFieldDefinition fieldDef, int indent) {

        StringBuilder builder = new StringBuilder();

        Class<? extends JsonValue> effectiveType = fieldDef.getEffectiveType();

        String name = fieldDef.getFieldName();
        Set<String> notes = fieldDef.getNotes();

        // Lists have a specialized set of builder methods.
        if (effectiveType == JsonList.class) {

            importsBuilder.add(ArrayList.class.getName());

            ViewClassJavadocsBuilder method1Javadocs = new ViewClassJavadocsBuilder();
            method1Javadocs.addParagraph("Sets the " + name + " field.");
            notes.forEach(method1Javadocs::addParagraph);
            method1Javadocs.addFieldOccurrencesList(fieldDef);
            method1Javadocs.addSampleStringValuesList(fieldDef, 5);
            method1Javadocs.newLine();
            method1Javadocs.addParameter(name).addCollectionFieldValueTypesSnippet(fieldDef).newLine();
            method1Javadocs.addReturn().add("this builder.");

            String[] method1 = {
                    method1Javadocs.buildJavadocsSource(indent),
                    indent(indent) + "public Builder " + name + "(" + getJavaFieldType(fieldDef) + " " + name + ") {\n",
                    indent(indent + 1) + "this." + name + " = " + name + " != null ? new ArrayList<>(" + name + ") : null;\n",
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
            method2Javadocs.addParagraph("Adds a single item to the " + name + " field.");
            notes.forEach(method2Javadocs::addParagraph);
            method2Javadocs.addFieldOccurrencesList(fieldDef);
            method2Javadocs.addSampleStringValuesList(fieldDef, 5);
            method2Javadocs.newLine();
            method2Javadocs.addParameter(name).add("the item to add. ").addFieldValueTypesSnippet(fieldDef).newLine();
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
                    indent(indent) + "public Builder addTo" + ViewClassStringUtils.toJavaMethodCase(name) + "(" + fieldDef.getEffectiveValueType().getLocalClassName() + " " + name + ") {\n",
                    indent(indent + 1) + "if (this." + name + " == null) {\n",
                    indent(indent + 2) + "this." + name + " = new ArrayList<>();\n",
                    indent(indent + 1) + "}\n",
                    indent(indent + 1) + "this." + name + ".add(" + name + ");\n",
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
            method3Javadocs.addParagraph("Adds a Collection of items to the " + name + " field.");
            notes.forEach(method3Javadocs::addParagraph);
            method3Javadocs.addFieldOccurrencesList(fieldDef);
            method3Javadocs.addSampleStringValuesList(fieldDef, 5);
            method3Javadocs.newLine();
            method3Javadocs.addParameter(name).add("the items to add. ").addCollectionFieldValueTypesSnippet(fieldDef).newLine();
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
                    indent(indent) + "public Builder addAllTo" + ViewClassStringUtils.toJavaMethodCase(name) + "(" + getJavaFieldType(fieldDef) + " " + name + ") {\n",
                    indent(indent + 1) + "if (this." + name + " == null) {\n",
                    indent(indent + 2) + "this." + name + " = new ArrayList<>();\n",
                    indent(indent + 1) + "}\n",
                    indent(indent + 1) + "this." + name + ".addAll(" + name + ");\n",
                    indent(indent + 1) + "return this;\n",
                    indent(indent) + "}"
            };

            builder.append(Arrays.stream(method1).collect(Collectors.joining(""))).append("\n\n");

            builder.append(Arrays.stream(method2).collect(Collectors.joining(""))).append("\n\n");

            builder.append(Arrays.stream(method3).collect(Collectors.joining("")));

        } else {
            // All other types follow a similar pattern

            ViewClassJavadocsBuilder methodJavadocs = new ViewClassJavadocsBuilder();

            methodJavadocs.addParagraph("Sets the " + name + " field.");
            notes.forEach(methodJavadocs::addParagraph);
            methodJavadocs.addFieldOccurrencesList(fieldDef);
            methodJavadocs.addSampleStringValuesList(fieldDef, 5);
            methodJavadocs.newLine();
            methodJavadocs.addParameter(name).addFieldAwareValueTypesSnippet(fieldDef).newLine();
            methodJavadocs.addReturn().add("this builder.");

            methodJavadocs.buildJavadocsSource(indent);

            String[] method = {
                    methodJavadocs.buildJavadocsSource(indent),
                    indent(indent) + "public Builder " + name + "(" + getJavaFieldType(fieldDef) + " " + name + ") {\n",
                    indent(indent + 1) + "this." + name + " = " + name + ";\n",
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
                javadocsBuilder.add("Adds an entry to the " + name + " field .").addLink("java.util.Map").addLine(".");
                notes.forEach(javadocsBuilder::addParagraph);
                javadocsBuilder.addFieldOccurrencesList(fieldDef);
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
                        indent(indent) + "public Builder add" + ViewClassStringUtils.toJavaMethodCase(name) + "(String key, Object value) {\n",
                        indent(indent + 1) + "if (this." + name + " == null) {\n",
                        indent(indent + 2) + "this." + name + " = new LinkedHashMap<>();\n",
                        indent(indent + 1) + "}\n",
                        indent(indent + 1) + "if (key != null) {\n",
                        indent(indent + 2) + "this." + name + ".put(key, value);\n",
                        indent(indent + 1) + "}\n",
                        indent(indent + 1) + "return this;\n",
                        indent(indent) + "}"
                };

                builder.append(Arrays.stream(mapAddMethod).collect(Collectors.joining("")));
            }
        }

        return builder.toString();
    }

    private String getInterfaceBuilderBuildMethodSource(ViewClassFieldDefinition fieldDef, int indent) {
        String[] method = {
                indent(indent) + "@Override\n",
                indent(indent) + "public " + getJavaFieldType(fieldDef) + " " + getJavaInterfaceMethodName(fieldDef) + "() {\n",
                indent(indent + 1) + "return " + fieldDef.getFieldName() + ";\n",
                indent(indent) + "}"
        };

        return Arrays.stream(method).collect(Collectors.joining(""));
    }

    private String getJavaInterfaceMethodName(ViewClassFieldDefinition fieldDef) {
        return "get" + ViewClassStringUtils.toJavaMethodCase(fieldDef.getFieldName());
    }
}
