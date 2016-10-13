package com.psddev.styleguide;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.StringUtils;

class TemplateFieldDefinitionList extends TemplateFieldDefinition implements TemplateFieldType {

    private Set<String> listItemTypes;
    private JsonObjectType effectiveListValueType;

    public TemplateFieldDefinitionList(TemplateDefinitions templateDefinitions, String parentTemplate, String name, List<JsonObject> values, Set<String> mapTemplates, String javaClassNamePrefix) {
        super(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix);

        Set<JsonObjectType> listValueTypes = new HashSet<>();

        values.forEach((value) -> {
            if (value instanceof JsonList) {

                JsonList list = (JsonList) value;

                JsonObjectType listValueType = list.getValuesType();
                if (listValueType != null) {
                    listValueTypes.add(listValueType);
                }
            }
        });

        listItemTypes = new LinkedHashSet<>();

        if (!listValueTypes.isEmpty()) {

            if (listValueTypes.size() > 1) {

                if (listValueTypes.size() == 2
                        && listValueTypes.contains(JsonObjectType.TEMPLATE_OBJECT) && listValueTypes.contains(JsonObjectType.STRING)) {

                    // We allow Strings and Objects to co-exist and just treat them as if it is Object
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
                    if (value instanceof JsonList) {

                        JsonList list = (JsonList) value;

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

    Set<String> getListItemTypes() {
        return listItemTypes;
    }

    @Override
    public String getFullyQualifiedClassName() {
        TemplateDefinition parentTemplateDef = templateDefinitions.getByName(parentTemplate);
        return parentTemplateDef.getFullyQualifiedClassName() + "." + StyleguideStringUtils.toJavaClassCase(name);
    }

    @Override
    public String getJavaFieldType(Set<String> imports) {
        imports.add(Collection.class.getName());

        String genericArgument;

        TemplateFieldType effectiveFieldValueType = getEffectiveValueType();

        if (effectiveFieldValueType == null || effectiveFieldValueType == NativeJavaTemplateFieldType.OBJECT) {
            genericArgument = "?";

        } else {
            // if the effective type lives in a different package, then import it.
            if (!this.hasSamePackageAs(effectiveFieldValueType)) {
                imports.add(effectiveFieldValueType.getFullyQualifiedClassName());
            }

            genericArgument = "? extends " + effectiveFieldValueType.getLocalClassName();
        }

        return "Collection<" + genericArgument + ">";
    }

    public String getJavaFieldTypeForBuilder(Set<String> imports) {
        imports.add(Collection.class.getName());

        // Collection<?> --> Collection<Object>
        // Collection<? extends Foo> --> Collection<Foo>
        return getJavaFieldType(imports).replace("? extends ", "").replace("?", "Object");
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
    public String getInterfaceBuilderMethodImplementationSource(int indent, Set<String> imports, boolean removeDeprecations) {

        StringBuilder builder = new StringBuilder();

        imports.add("java.util.ArrayList");
        if (!removeDeprecations) {
            imports.add("java.util.stream.Collectors");
        }

        String nameViewClass = name + "ViewClass";
        String nameView = name + "View";
        String nameModel = name + "Model";
        String nameModels = name + "Models";

        String valueTypesJavaDocList = getValueTypesJavadocsClassLinksJavadocSnippet();
        if (valueTypesJavaDocList == null) {
            valueTypesJavaDocList = "";
        } else {
            valueTypesJavaDocList = StringUtils.ensureStart(valueTypesJavaDocList, " ");
        }

        StringBuilder notesJavaDoc = new StringBuilder();
        for (String note : notes) {
            notesJavaDoc.append(indent(indent)).append(" * <p>").append(note).append("</p>\n");
        }

        String[] method1 = {
                indent(indent) + "/**\n",
                indent(indent) + " * <p>Sets the " + name + " field.</p>\n",
                notesJavaDoc.toString(),
                indent(indent) + " *\n",
                indent(indent) + " * @param " + name + valueTypesJavaDocList + "\n",
                indent(indent) + " * @return this builder.\n",
                indent(indent) + " */\n",
                indent(indent) + "public Builder " + name + "(" + getJavaFieldType(imports) + " " + name + ") {\n",
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
                indent(indent) + "/**\n",
                indent(indent) + " * <p>Adds a single item to the " + name + " field.</p>\n",
                notesJavaDoc.toString(),
                indent(indent) + " *\n",
                indent(indent) + " * @param " + name + " the item to add." + valueTypesJavaDocList + "\n",
                indent(indent) + " * @return this builder.\n",
                indent(indent) + " */\n",
                indent(indent) + "public Builder addTo" + StyleguideStringUtils.toJavaMethodCase(name) + "(" + getEffectiveValueType().getLocalClassName() + " " + name + ") {\n",
                indent(indent + 1) + "if (this." + name + " == null) {\n",
                indent(indent + 2) + "this." + name + " = new ArrayList<>();\n",
                indent(indent + 1) + "}\n",
                indent(indent + 1) + "this." + name + ".add(" + name + ");\n",
                indent(indent + 1) + "return this;\n",
                indent(indent) + "}"
        };

        /**
         * Adds a Collection of items to the articleBody field.
         *
         * @param articleBody the items to add, typically a {@link FigureView}.
         * @return this builder.
         */
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
                indent(indent) + "/**\n",
                indent(indent) + " * <p>Adds a Collection of items to the " + name + " field.</p>\n",
                notesJavaDoc.toString(),
                indent(indent) + " *\n",
                indent(indent) + " * @param " + name + " the items to add." + valueTypesJavaDocList + "\n",
                indent(indent) + " * @return this builder.\n",
                indent(indent) + " */\n",
                indent(indent) + "public Builder addAllTo" + StyleguideStringUtils.toJavaMethodCase(name) + "(" + getJavaFieldType(imports) + " " + name + ") {\n",
                indent(indent + 1) + "if (this." + name + " == null) {\n",
                indent(indent + 2) + "this." + name + " = new ArrayList<>();\n",
                indent(indent + 1) + "}\n",
                indent(indent + 1) + "this." + name + ".addAll(" + name + ");\n",
                indent(indent + 1) + "return this;\n",
                indent(indent) + "}"
        };

        /**
         * @deprecated Use {@link #addToArticleBody(Object)} instead.
         */
        /*
        @Deprecated
        public Builder addAuthors(Object authors) {
            if (this.authors == null) {
                this.authors = new ArrayList<>();
            }
            this.authors.add(authors);
            return this;
        }
         */
        String[] method4 = {
                indent(indent) + "/**\n",
                indent(indent) + " * @deprecated Use {@link #addTo" + StyleguideStringUtils.toJavaMethodCase(name) + "(Object)} instead.\n",
                indent(indent) + " */\n",
                indent(indent) + "@Deprecated\n",
                indent(indent) + "public Builder add" + StyleguideStringUtils.toJavaMethodCase(name) + "(Object " + name + ") {\n",
                indent(indent + 1) + "if (this." + name + " == null) {\n",
                indent(indent + 2) + "this." + name + " = new ArrayList<>();\n",
                indent(indent + 1) + "}\n",
                indent(indent + 1) + "this." + name + ".add(" + name + ");\n",
                indent(indent + 1) + "return this;\n",
                indent(indent) + "}"
        };

        /**
         * @deprecated no replacement.
         */
        /*
        @Deprecated
        public Builder authors(Class<?> authorsViewClass, List<?> authorsModels) {
            this.authors = authorsModels.stream()
                    .map((authorsModel) -> request.createView(authorsViewClass, authorModel))
                    .filter((authorsView) -> authorsView != null)
                    .collect(Collectors.toList());
            return this;
        }
         */
        String[] method5 = {
                indent(indent) + "/**\n",
                indent(indent) + " * @deprecated no replacement\n",
                indent(indent) + " */\n",
                indent(indent) + "@Deprecated\n",
                indent(indent) + "public Builder " + name + "(Class<?> " + nameViewClass + ", Collection<?> " + nameModels + ") {\n",
                indent(indent + 1) + "this." + name + " = " + nameModels + ".stream()\n",
                indent(indent + 3) + ".map((" + nameModel + ") -> request.createView(" + nameViewClass + ", " + nameModel + "))\n",
                indent(indent + 3) + ".filter((" + nameView + ") -> " + nameView + " != null)\n",
                indent(indent + 3) + ".collect(Collectors.toList());\n",
                indent(indent + 1) + "return this;\n",
                indent(indent) + "}"
        };

        /**
         * @deprecated no replacement.
         */
        /*
        @Deprecated
        public Builder addAuthors(Class<?> authorsViewClass, Object authorsModel) {
            Object authors = request.createView(authorsViewClass, authorsModel);
            if (authors != null) {
                addAuthors(authors);
            }
            return this;
        }
         */
        String[] method6 = {
                indent(indent) + "/**\n",
                indent(indent) + " * @deprecated no replacement\n",
                indent(indent) + " */\n",
                indent(indent) + "@Deprecated\n",
                indent(indent) + "public Builder add" + StyleguideStringUtils.toJavaMethodCase(name) + "(Class<?> " + nameViewClass + ", Object " + nameModel + ") {\n",
                indent(indent + 1) + "Object " + name + " = request.createView(" + nameViewClass + ", " + nameModel + ");\n",
                indent(indent + 1) + "if (" + name + " != null) {\n",
                indent(indent + 2) + "add" + StyleguideStringUtils.toJavaMethodCase(name) + "(" + name + ");\n",
                indent(indent + 1) + "}\n",
                indent(indent + 1) + "return this;\n",
                indent(indent) + "}"
        };

        builder.append(Arrays.stream(method1).collect(Collectors.joining(""))).append("\n\n");

        builder.append(Arrays.stream(method2).collect(Collectors.joining(""))).append("\n\n");

        builder.append(Arrays.stream(method3).collect(Collectors.joining(""))).append(removeDeprecations ? "" : "\n\n");

        if (!removeDeprecations) {
            builder.append(Arrays.stream(method4).collect(Collectors.joining(""))).append("\n\n");

            builder.append(Arrays.stream(method5).collect(Collectors.joining(""))).append("\n\n");

            builder.append(Arrays.stream(method6).collect(Collectors.joining("")));
        }

        return builder.toString();
    }
}
