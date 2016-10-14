package com.psddev.styleguide;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.StringUtils;

abstract class TemplateFieldDefinition {

    protected TemplateDefinitions templateDefinitions;
    protected String parentTemplate;
    protected String name;
    protected List<JsonObject> values;
    protected Set<String> mapTemplates;
    protected Set<String> notes;
    protected String javaClassNamePrefix;
    protected boolean isDefaulted;
    protected boolean isStrictlyTyped;

    public static TemplateFieldDefinition createInstance(TemplateDefinitions templateDefinitions, String parentTemplate, String name, List<JsonObject> values, Set<String> mapTemplates, String javaClassNamePrefix) {

        JsonObjectType effectiveValueType;

        Set<JsonObjectType> valueTypes = new HashSet<>();

        values.forEach((value) -> valueTypes.add(value.getType()));

        if (valueTypes.size() == 1
                || valueTypes.size() == 2 && valueTypes.contains(JsonObjectType.TEMPLATE_OBJECT) && valueTypes.contains(JsonObjectType.STRING)) {

            if (valueTypes.size() == 2) {
                // We allow Strings and Objects to co-exist and just treat them as if it is Object
                effectiveValueType = JsonObjectType.TEMPLATE_OBJECT;

            } else {
                effectiveValueType = valueTypes.iterator().next();
            }

            if (effectiveValueType == JsonObjectType.BOOLEAN) {
                return new TemplateFieldDefinitionBoolean(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix);

            } else if (effectiveValueType == JsonObjectType.STRING) {
                return new TemplateFieldDefinitionString(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix);

            } else if (effectiveValueType == JsonObjectType.NUMBER) {
                return new TemplateFieldDefinitionNumber(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix);

            } else if (effectiveValueType == JsonObjectType.LIST) {
                return new TemplateFieldDefinitionList(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix);

            } else if (effectiveValueType == JsonObjectType.MAP) {
                return new TemplateFieldDefinitionMap(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix);

            } else if (effectiveValueType == JsonObjectType.TEMPLATE_OBJECT) {
                return new TemplateFieldDefinitionObject(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix);

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

    public TemplateFieldDefinition(TemplateDefinitions templateDefinitions, String parentTemplate, String name, List<JsonObject> values, Set<String> mapTemplates, String javaClassNamePrefix) {
        this.templateDefinitions = templateDefinitions;
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
        this.javaClassNamePrefix = javaClassNamePrefix;
        // TODO: Eventually pass these values as parameters to the constructor so the behavior can be configured.
        this.isDefaulted = false;
        this.isStrictlyTyped = true;
    }

    @Override
    public String toString() {
        return getEffectiveValueType().getFullyQualifiedClassName()
                + " " + name + " : ["
                + getFieldValueTypes().stream()
                        .map(TemplateFieldType::getFullyQualifiedClassName)
                        .collect(Collectors.joining(", "))
                + "]";
    }

    public TemplateDefinition getParentTemplate() {
        return templateDefinitions.getByName(parentTemplate);
    }

    public String getName() {
        return name;
    }

    public Set<String> getNotes() {
        return notes;
    }

    public String getJavaFieldType() {
        return getJavaFieldType(new HashSet<>());
    }

    /** Never null. */
    public abstract Set<TemplateFieldType> getFieldValueTypes();

    /** Never null. */
    protected TemplateFieldType getEffectiveValueType() {

        Set<TemplateFieldType> fieldValueTypes = getFieldValueTypes();

        if (fieldValueTypes.size() == 1) {
            return isStrictlyTyped ? fieldValueTypes.iterator().next() : NativeJavaTemplateFieldType.OBJECT;

        } else if (fieldValueTypes.size() > 1) {

            if (isStrictlyTyped && this instanceof TemplateFieldType) {
                return (TemplateFieldType) this;

            } else {
                return NativeJavaTemplateFieldType.OBJECT;
            }

        } else {
            return NativeJavaTemplateFieldType.OBJECT;
        }
    }

    public abstract String getJavaFieldType(Set<String> imports);

    public String getJavaFieldTypeForBuilder(Set<String> imports) {
        return getJavaFieldType();
    }

    String getJavaInterfaceMethodName() {

        String methodNamePrefix = "get";

        if ("boolean".equals(getJavaFieldType())) {
            methodNamePrefix = "is";
        }

        return methodNamePrefix + StyleguideStringUtils.toJavaMethodCase(name);
    }

    String getInterfaceMethodDeclarationSource(int indent, Set<String> imports) {

        // collect the methods' javadocs
        TemplateJavadocsBuilder methodJavadocs = new TemplateJavadocsBuilder();

        notes.forEach(methodJavadocs::addParagraph);

        methodJavadocs.startParagraph();
        if (this instanceof TemplateFieldDefinitionList) {
            methodJavadocs.addCollectionFieldValueTypesSnippet(this);
        } else {
            methodJavadocs.addFieldValueTypesSnippet(this);
        }
        methodJavadocs.endParagraph();

        // if it's a default interface method just make the body return null;
        String methodBody = !isDefaulted ? ";" : (" {\n"
                + indent(indent + 1) + "return null;\n"
                + indent(indent) + "}");

        return methodJavadocs.buildJavadocsSource(indent)
                + indent(indent) + (isDefaulted ? "default " : "") + getJavaFieldType(imports) + " " + getJavaInterfaceMethodName() + "()" + methodBody;
    }

    public String getInterfaceStaticStringVariableDeclaration(int indent, String suffix) {
        return getInterfaceStaticStringVariableDeclarationHelper(indent, suffix, false, null);
    }

    public String getInterfaceStaticStringVariableDeclarationDeprecated(int indent, String suffix, String alternateSuffix) {
        return getInterfaceStaticStringVariableDeclarationHelper(indent, suffix, true, alternateSuffix);
    }

    private String getInterfaceStaticStringVariableDeclarationHelper(int indent, String suffix, boolean isDeprecated, String alternateSuffix) {
        String varName = StringUtils.toUnderscored(name).toUpperCase() + suffix;
        String varValue = name;

        String declaration = "";

        if (isDeprecated) {
            String deprecatedVarName = StringUtils.toUnderscored(name).toUpperCase() + alternateSuffix;
            declaration += indent(indent) + "/**\n";
            declaration += indent(indent) + " * @deprecated Use {@link #" + deprecatedVarName + "} instead.\n";
            declaration += indent(indent) + " */\n";
            declaration += indent(indent) + "@Deprecated\n";
        }
        declaration += indent(indent) + "static final String " + varName + " = \"" + varValue + "\";";

        return declaration;
    }

    public String getInterfaceBuilderFieldDeclarationSource(int indent, Set<String> imports) {
        return indent(indent) + "private " + getJavaFieldTypeForBuilder(imports) + " " + name + ";";
    }

    public String getInterfaceBuilderMethodImplementationSource(int indent, Set<String> imports) {

        TemplateJavadocsBuilder methodJavadocs = new TemplateJavadocsBuilder();

        methodJavadocs.addParagraph("Sets the " + name + " field.");
        notes.forEach(methodJavadocs::addParagraph);
        methodJavadocs.newLine();
        methodJavadocs.addParameter(name).addFieldAwareValueTypesSnippet(this).newLine();
        methodJavadocs.addReturn().add("this builder.");

        methodJavadocs.buildJavadocsSource(indent);

        String[] method = {
                methodJavadocs.buildJavadocsSource(indent),
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
