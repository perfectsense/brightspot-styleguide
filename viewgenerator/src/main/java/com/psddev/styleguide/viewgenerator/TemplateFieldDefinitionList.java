package com.psddev.styleguide.viewgenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.styleguide.JsonObject;
import com.psddev.styleguide.JsonObjectType;
import com.psddev.styleguide.JsonTemplateObject;

class TemplateFieldDefinitionList extends TemplateFieldDefinition implements TemplateFieldType {

    private Set<String> listItemTypes;
    private JsonObjectType effectiveListValueType;

    public TemplateFieldDefinitionList(TemplateDefinitions templateDefinitions, String parentTemplate, String name, List<JsonObject> values, Set<String> mapTemplates, String javaClassNamePrefix, boolean isDefaulted, boolean isStrictlyTyped) {
        super(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix, isDefaulted, isStrictlyTyped);

        Set<JsonObjectType> listValueTypes = new HashSet<>();

        values.forEach((value) -> {
            if (value instanceof com.psddev.styleguide.JsonList) {

                com.psddev.styleguide.JsonList list = (com.psddev.styleguide.JsonList) value;

                JsonObjectType listValueType = list.getValuesType();
                if (listValueType != null) {
                    listValueTypes.add(listValueType);
                }
            }
        });

        listItemTypes = new LinkedHashSet<>();

        if (!listValueTypes.isEmpty()) {

            if (listValueTypes.size() > 1) {

                if (!isStrictlyTyped && listValueTypes.size() == 2
                        && listValueTypes.containsAll(Arrays.asList(JsonObjectType.TEMPLATE_OBJECT, JsonObjectType.STRING))) {

                    // If not strictly typed, we allow Strings and Objects to co-exist and just treat them as if it is Object
                    effectiveListValueType = JsonObjectType.TEMPLATE_OBJECT;

                } else {
                    throw new IllegalArgumentException("ERROR: (" + this.parentTemplate + " - " + this.name
                            + ") List can only have one item value type but found [" + listValueTypes + "]!");
                }

            } else {
                effectiveListValueType = listValueTypes.iterator().next();
            }

            if (effectiveListValueType == JsonObjectType.BOOLEAN) {
                listItemTypes.add("java.lang.Boolean");

            } else if (effectiveListValueType == JsonObjectType.STRING) {
                listItemTypes.add("java.lang.String");

            } else if (effectiveListValueType == JsonObjectType.NUMBER) {
                listItemTypes.add("java.lang.Number");

            } else if (effectiveListValueType == JsonObjectType.LIST) {
                // TODO: Should we allow nested JSON arrays at all?
                listItemTypes.add("java.util.Collection");

            } else if (effectiveListValueType == JsonObjectType.TEMPLATE_OBJECT) {

                values.forEach((value) -> {
                    if (value instanceof com.psddev.styleguide.JsonList) {

                        com.psddev.styleguide.JsonList list = (com.psddev.styleguide.JsonList) value;

                        list.getValues().forEach((itemValue) -> {
                            if (itemValue instanceof JsonTemplateObject) {
                                listItemTypes.add(((JsonTemplateObject) itemValue).getTemplateName());
                            }
                        });
                    }
                });
            }
        }
    }

    public Set<String> getListItemTypes() {
        return listItemTypes;
    }

    @Override
    public String getFullyQualifiedClassName() {
        TemplateDefinition parentTemplateDef = templateDefinitions.getByName(parentTemplate);
        return parentTemplateDef.getFullyQualifiedClassName() + StyleguideStringUtils.toJavaClassCase(name) + "Field";
    }

    @Override
    public String getJavaFieldType(ViewClassImportsBuilder importsBuilder) {
        importsBuilder.add(Collection.class.getName());

        String genericArgument;

        TemplateFieldType effectiveFieldValueType = getEffectiveValueType();

        if (effectiveFieldValueType == null || effectiveFieldValueType == NativeJavaTemplateFieldType.OBJECT) {
            genericArgument = "?";

        } else {
            // if the effective type lives in a different package, then import it.
            if (!this.hasSamePackageAs(effectiveFieldValueType)) {
                importsBuilder.add(effectiveFieldValueType);
            }

            genericArgument = "? extends " + effectiveFieldValueType.getLocalClassName();
        }

        return "Collection<" + genericArgument + ">";
    }

    public String getJavaFieldTypeForBuilder(ViewClassImportsBuilder importsBuilder) {
        importsBuilder.add(Collection.class.getName());

        // Collection<?> --> Collection<Object>
        // Collection<? extends Foo> --> Collection<Foo>
        return getJavaFieldType(importsBuilder).replace("? extends ", "").replace("?", "Object");
    }

    @Override
    public Set<TemplateFieldType> getFieldValueTypes() {

        Set<TemplateFieldType> fieldTypes = new LinkedHashSet<>();

        if (!listItemTypes.isEmpty()) {

            if (effectiveListValueType == JsonObjectType.STRING) {
                fieldTypes.add(NativeJavaTemplateFieldType.STRING);

            } else if (effectiveListValueType == JsonObjectType.BOOLEAN) {
                fieldTypes.add(NativeJavaTemplateFieldType.BOOLEAN);

            } else if (effectiveListValueType == JsonObjectType.NUMBER) {
                fieldTypes.add(NativeJavaTemplateFieldType.NUMBER);

            } else if (effectiveListValueType == JsonObjectType.LIST) {
                fieldTypes.add(NativeJavaTemplateFieldType.COLLECTION);

            } else {
                for (String templateType : listItemTypes) {

                    TemplateDefinition templateDef = templateDefinitions.getByName(templateType);
                    if (templateDef != null) {
                        fieldTypes.add(templateDef);
                    }
                }
            }
        }

        return fieldTypes;
    }

    @Override
    public String getInterfaceBuilderMethodImplementationSource(int indent, ViewClassImportsBuilder importsBuilder) {

        StringBuilder builder = new StringBuilder();

        importsBuilder.add(ArrayList.class.getName());

        ViewClassJavadocsBuilder method1Javadocs = new ViewClassJavadocsBuilder();
        method1Javadocs.addParagraph("Sets the " + name + " field.");
        notes.forEach(method1Javadocs::addParagraph);
        method1Javadocs.newLine();
        method1Javadocs.addParameter(name).addCollectionFieldValueTypesSnippet(this).newLine();
        method1Javadocs.addReturn().add("this builder.");

        String[] method1 = {
                method1Javadocs.buildJavadocsSource(indent),
                indent(indent) + "public Builder " + name + "(" + getJavaFieldType(importsBuilder) + " " + name + ") {\n",
                indent(indent + 1) + "this." + name + " = " + name + " != null ? new ArrayList<>(" + name + ") : null;\n",
                indent(indent + 1) + "return this;\n",
                indent(indent) + "}"
        };

        /**
         * Adds a single item to the articleBody field.
         *
         * @param articleBody the item to add, typically a {@link FigureView}.
         * @return this builder.
         */
        ViewClassJavadocsBuilder method2Javadocs = new ViewClassJavadocsBuilder();
        method2Javadocs.addParagraph("Adds a single item to the " + name + " field.");
        notes.forEach(method2Javadocs::addParagraph);
        method2Javadocs.newLine();
        method2Javadocs.addParameter(name).add("the item to add. ").addFieldValueTypesSnippet(this).newLine();
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
                indent(indent) + "public Builder addTo" + StyleguideStringUtils.toJavaMethodCase(name) + "(" + getEffectiveValueType().getLocalClassName() + " " + name + ") {\n",
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
        method3Javadocs.newLine();
        method3Javadocs.addParameter(name).add("the items to add. ").addCollectionFieldValueTypesSnippet(this).newLine();
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
                indent(indent) + "public Builder addAllTo" + StyleguideStringUtils.toJavaMethodCase(name) + "(" + getJavaFieldType(importsBuilder) + " " + name + ") {\n",
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

        return builder.toString();
    }
}
