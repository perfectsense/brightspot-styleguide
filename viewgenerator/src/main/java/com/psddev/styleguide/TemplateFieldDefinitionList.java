package com.psddev.styleguide;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class TemplateFieldDefinitionList extends TemplateFieldDefinition {

    private String listItemJavaType;
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

        listItemJavaType = "Object";
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
                listItemJavaType = "Boolean";

            } else if (effectiveListValueType == JsonObjectType.STRING) {
                listItemJavaType = "?";
                listItemTypes.add("java.lang.String");

            } else if (effectiveListValueType == JsonObjectType.NUMBER) {
                listItemJavaType = "? extends Number";

            } else if (effectiveListValueType == JsonObjectType.LIST) {
                listItemJavaType = "?";
                listItemTypes.add("java.util.Collection");

            } else if (effectiveListValueType == JsonObjectType.TEMPLATE_OBJECT) {
                listItemJavaType = "?";

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

    @Override
    public String getJavaFieldType(Set<String> imports) {
        imports.add(Collection.class.getName());
        return "Collection<" + listItemJavaType + ">";
    }

    public String getJavaFieldTypeForBuilder(Set<String> imports) {
        imports.add(Collection.class.getName());
        return "Collection<" + ("?".equals(listItemJavaType) ? "Object" : listItemJavaType) + ">";
    }

    @Override
    public Set<String> getValueTypes() {

        Set<String> viewClassNames = new LinkedHashSet<>();

        if (!listItemTypes.isEmpty()) {

            if (effectiveListValueType == JsonObjectType.STRING) {
                return Collections.singleton("java.lang.String");

            } else if (effectiveListValueType == JsonObjectType.LIST) {
                return Collections.singleton("java.util.Collection");

            } else {
                for (String templateType : listItemTypes) {

                    String className = templateDefinitions.getTemplateDefinitionRelativeClassName(templateType, parentTemplate);
                    if (className != null) {
                        viewClassNames.add(className);
                    }
                }
            }
        }

        return viewClassNames;
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
        String valueTypesJavaDocList = getValueTypesJavaDocList();

        StringBuilder notesJavaDoc = new StringBuilder();
        for (String note : notes) {
            notesJavaDoc.append(indent(indent)).append(" * <p>").append(note).append("</p>\n");
        }

        String[] method1 = {
                indent(indent) + "/**\n",
                indent(indent) + " * <p>Sets the " + name + " field.</p>\n",
                notesJavaDoc.toString(),
                indent(indent) + " *\n",
                indent(indent) + " * @param " + name + " Typically a Collection of " + valueTypesJavaDocList + ".\n",
                indent(indent) + " * @return this builder.\n",
                indent(indent) + " */\n",
                indent(indent) + "public Builder " + name + "(" + getJavaFieldType(imports) + " " + name + ") {\n",
                indent(indent + 1) + "this." + name + " = " + name + " != null ? new ArrayList<>(" + name + ") : null;\n",
                indent(indent + 1) + "return this;\n",
                indent(indent) + "}"
        };

        /**
         * Sets the articleBody field.
         *
         * @param articleBodyViewClass the articleBody views class, typically a Class of {@link FigureView}.
         * @param articleBodyModels the models powering the articleBody views.
         * @return this builder.
         */
        /*
        public Builder authors(Class<?> authorsViewClass, List<?> authorsModels) {
            this.authors = authorsModels.stream()
                    .map((authorsModel) -> request.createView(authorsViewClass, authorModel))
                    .filter((authorsView) -> authorsView != null)
                    .collect(Collectors.toList());
            return this;
        }
         */
        String[] method2 = {
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
         * Adds an item to the articleBody field.
         *
         * @param articleBody the view to set, typically a {@link FigureView}.
         * @return this builder.
         */
        /*
        public Builder addAuthors(Object authors) {
            if (this.authors == null) {
                this.authors = new ArrayList<>();
            }
            this.authors.add(authors);
            return this;
        }
         */
        String[] method3 = {
                indent(indent) + "/**\n",
                indent(indent) + " * <p>Adds an item to the " + name + " field.</p>\n",
                notesJavaDoc.toString(),
                indent(indent) + " *\n",
                indent(indent) + " * @param " + name + " the view to set, typically a " + valueTypesJavaDocList + ".\n",
                indent(indent) + " * @return this builder.\n",
                indent(indent) + " */\n",
                indent(indent) + "public Builder add" + StyleguideStringUtils.toPascalCase(name) + "(Object " + name + ") {\n",
                indent(indent + 1) + "if (this." + name + " == null) {\n",
                indent(indent + 2) + "this." + name + " = new ArrayList<>();\n",
                indent(indent + 1) + "}\n",
                indent(indent + 1) + "this." + name + ".add(" + name + ");\n",
                indent(indent + 1) + "return this;\n",
                indent(indent) + "}"
        };

        /**
         * Adds an item to the articleBody field.
         *
         * @param articleBodyViewClass the articleBody view class, typically a Class of {@link FigureView}.
         * @param articleBodyModel the model powering the articleBody view.
         * @return this builder.
         */
        /*
        public Builder addAuthors(Class<?> authorsViewClass, Object authorsModel) {
            Object authors = request.createView(authorsViewClass, authorsModel);
            if (authors != null) {
                addAuthors(authors);
            }
            return this;
        }
         */
        String[] method4 = {
                indent(indent) + "/**\n",
                indent(indent) + " * @deprecated no replacement\n",
                indent(indent) + " */\n",
                indent(indent) + "@Deprecated\n",
                indent(indent) + "public Builder add" + StyleguideStringUtils.toPascalCase(name) + "(Class<?> " + nameViewClass + ", Object " + nameModel + ") {\n",
                indent(indent + 1) + "Object " + name + " = request.createView(" + nameViewClass + ", " + nameModel + ");\n",
                indent(indent + 1) + "if (" + name + " != null) {\n",
                indent(indent + 2) + "add" + StyleguideStringUtils.toPascalCase(name) + "(" + name + ");\n",
                indent(indent + 1) + "}\n",
                indent(indent + 1) + "return this;\n",
                indent(indent) + "}"
        };

        builder.append(Arrays.stream(method1).collect(Collectors.joining(""))).append("\n\n");

        if (!removeDeprecations) {
            builder.append(Arrays.stream(method2).collect(Collectors.joining(""))).append("\n\n");
        }

        builder.append(Arrays.stream(method3).collect(Collectors.joining(""))).append(removeDeprecations ? "" : "\n\n");

        if (!removeDeprecations) {
            builder.append(Arrays.stream(method4).collect(Collectors.joining("")));
        }

        return builder.toString();
    }
}
