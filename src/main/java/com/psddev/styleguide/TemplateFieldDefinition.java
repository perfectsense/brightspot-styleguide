package com.psddev.styleguide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.StringUtils;

abstract class TemplateFieldDefinition {

    protected String parentTemplate;
    protected String name;
    protected List<JsonObject> values;
    protected List<String> mapTemplates;
    protected Set<String> notes;

    public static TemplateFieldDefinition createInstance(String parentTemplate, String name, List<JsonObject> values, List<String> mapTemplates) {

        JsonObjectType effectiveValueType;

        Set<JsonObjectType> valueTypes = new HashSet<>();

        values.forEach((value) -> valueTypes.add(value.getType()));

        if (valueTypes.size() == 1
                || valueTypes.size() == 2 && valueTypes.contains(JsonObjectType.MAP) && valueTypes.contains(JsonObjectType.STRING)) {

            if (valueTypes.size() == 2) {

                // special case that we allow, but just log it as a warning.
                System.out.println("WARN: (" + parentTemplate + " - " + name
                        + ") has multiple value types " + valueTypes + ", but this is a common use case so we allow it.");

                effectiveValueType = JsonObjectType.MAP;

            } else {
                effectiveValueType = valueTypes.iterator().next();
            }

            if (effectiveValueType == JsonObjectType.BOOLEAN) {
                return new TemplateFieldDefinitionBoolean(parentTemplate, name, values, mapTemplates);

            } else if (effectiveValueType == JsonObjectType.STRING) {
                return new TemplateFieldDefinitionString(parentTemplate, name, values, mapTemplates);

            } else if (effectiveValueType == JsonObjectType.NUMBER) {
                return new TemplateFieldDefinitionNumber(parentTemplate, name, values, mapTemplates);

            } else if (effectiveValueType == JsonObjectType.LIST) {
                return new TemplateFieldDefinitionList(parentTemplate, name, values, mapTemplates);

            } else if (effectiveValueType == JsonObjectType.MAP) {
                return new TemplateFieldDefinitionObject(parentTemplate, name, values, mapTemplates);

            } else {
                throw new IllegalArgumentException("ERROR: (" + parentTemplate + " - " + name
                        + ") Unknown field value type [" + effectiveValueType + "]!");
            }

        } else if (valueTypes.size() == 0) {
            throw new IllegalArgumentException("ERROR: (" + parentTemplate + " - " + name + ") A field must have at least 1 value type!");

        } else {
            throw new IllegalArgumentException("ERROR: (" + parentTemplate + " - " + name
                    + ") A field can only have a single value type but has " + valueTypes + " instead!");
        }
    }

    public TemplateFieldDefinition(String parentTemplate, String name, List<JsonObject> values, List<String> mapTemplates) {
        this.parentTemplate = parentTemplate;
        this.name = name;
        this.values = values;
        this.mapTemplates = mapTemplates;
        this.notes = new LinkedHashSet<>();
        values.forEach((value) -> {
            String valueNotes = value.getNotes();
            if (valueNotes != null) {
                notes.add(valueNotes);
            }
        });
    }

    @Override
    public String toString() {
        return getJavaFieldType() + " " + name + " : " + getValueTypes();
    }

    public Set<String> getNotes() {
        return notes;
    }

    public String getJavaFieldType() {
        return getJavaFieldType(new HashSet<>());
    }

    public abstract Set<String> getValueTypes();

    public abstract String getJavaFieldType(Set<String> imports);

    public String getJavaInterfaceMethodName() {

        String methodNamePrefix = "get";

        if ("Boolean".equals(getJavaFieldType())) {
            methodNamePrefix = "is";
        }

        return methodNamePrefix + StringUtils.toPascalCase(name);
    }

    public final String getValueTypesJavaDocList() {

        List<String> types = new ArrayList<>(getValueTypes());
        if (types.size() > 1) {

            List<String> firstTypes = types.subList(0, types.size() - 1);
            String lastType = types.get(types.size() - 1);

            return firstTypes.stream()
                    .map((viewClass) -> "{@link " + viewClass + "}")
                    .collect(Collectors.joining(", ")) + " or {@link " + lastType + "}";

        } else if (types.size() == 1) {
            return "{@link " + types.get(0) + "}";

        } else {
            return null;
        }
    }

    public String getInterfaceMethodDeclarationSource(int indent, Set<String> imports) {

        String methodJavaDoc = "";

        String valueTypesJavaDocList = getValueTypesJavaDocList();
        if (valueTypesJavaDocList != null) {
            String valueTypesJavaDocListPrefix;

            if (this instanceof TemplateFieldDefinitionList) {
                valueTypesJavaDocListPrefix = "Typically a List of ";
            } else {
                valueTypesJavaDocListPrefix = "Typically a ";
            }

            StringBuilder notesSource = new StringBuilder();
            for (String note : notes) {
                notesSource.append(indent(indent)).append(" * <p>").append(note).append("</p>\n");
            }

            methodJavaDoc = Arrays.stream(new String[] {
                    indent(indent) + "/**\n",
                    notesSource.toString(),
                    indent(indent) + " * <p>" + valueTypesJavaDocListPrefix + valueTypesJavaDocList + "</p>.\n",
                    indent(indent) + " */\n"
            }).collect(Collectors.joining(""));
        }

        return methodJavaDoc + indent(indent) + getJavaFieldType(imports) + " " + getJavaInterfaceMethodName() + "();";
    }

    public String getInterfaceBuilderFieldDeclarationSource(int indent, Set<String> imports) {
        return indent(indent) + "private " + getJavaFieldType(imports) + " " + name + ";";
    }

    public String getInterfaceBuilderMethodImplementationSource(int indent, Set<String> imports) {

        String methodJavaDoc = "";

        StringBuilder notesJavaDoc = new StringBuilder();
        for (String note : notes) {
            notesJavaDoc.append(indent(indent)).append(" * <p>").append(note).append("</p>\n");
        }

        String valueTypesJavaDocList = getValueTypesJavaDocList();
        if (valueTypesJavaDocList != null) {
            String valueTypesJavaDocListPrefix;

            if (this instanceof TemplateFieldDefinitionList) {
                valueTypesJavaDocListPrefix = "Typically a List of ";
            } else {
                valueTypesJavaDocListPrefix = "Typically a ";
            }

            methodJavaDoc = Arrays.stream(new String[] {
                    indent(indent) + "/**\n",
                    indent(indent) + " * <p>Sets the " + name + " field.</p>\n",
                    notesJavaDoc.toString(),
                    indent(indent) + " *\n",
                    indent(indent) + " * @param " + name + " " + valueTypesJavaDocListPrefix + valueTypesJavaDocList + ".\n",
                    indent(indent) + " * @return this builder.\n",
                    indent(indent) + " */\n"
            }).collect(Collectors.joining(""));
        }

        String[] method = {
                methodJavaDoc,
                indent(indent) + "public Builder " + name + "(" + getJavaFieldType(imports) + " " + name + ") {\n",
                indent(indent + 1) + "this." + name + " = " + name + ";\n",
                indent(indent + 1) + "return this;\n",
                indent(indent) + "}"
        };

        return Arrays.stream(method).collect(Collectors.joining(""));
    }

    public String getInterfaceBuilderBuildMethodSource(int indent, Set<String> imports) {
        String[] method = {
                indent(indent) + "@Override\n",
                indent(indent) + "public " + getJavaFieldType(imports) + " " + getJavaInterfaceMethodName() + "() {\n",
                indent(indent + 1) + "return " + name + ";\n",
                indent(indent) + "}"
        };

        return Arrays.stream(method).collect(Collectors.joining(""));
    }

    protected String indent(int indent) {
        char[] spaces = new char[indent * 4];
        Arrays.fill(spaces, ' ');
        return new String(spaces);
    }
}
