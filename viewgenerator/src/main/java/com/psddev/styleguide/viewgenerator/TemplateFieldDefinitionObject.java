package com.psddev.styleguide.viewgenerator;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.styleguide.JsonObject;
import com.psddev.styleguide.JsonTemplateObject;

public class TemplateFieldDefinitionObject extends TemplateFieldDefinition implements TemplateFieldType {

    private Set<String> templateTypes = new LinkedHashSet<>();

    public TemplateFieldDefinitionObject(TemplateDefinitions templateDefinitions, String parentTemplate, String name, List<JsonObject> values, Set<String> mapTemplates, String javaClassNamePrefix, boolean isDefaulted, boolean isStrictlyTyped) {
        super(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix, isDefaulted, isStrictlyTyped);

        values.forEach((value) -> {
            if (value instanceof JsonTemplateObject) {

                String templateName = ((JsonTemplateObject) value).getTemplateName();
                // the only time this would be null is when the object contains the DELEGATE_TEMPLATE_OBJECT_KEY key
                if (templateName != null) {
                    templateTypes.add(templateName);
                }
            }
        });
    }

    @Override
    public String getFullyQualifiedClassName() {
        TemplateDefinition parentTemplateDef = templateDefinitions.getByName(parentTemplate);
        return parentTemplateDef.getFullyQualifiedClassName() + StyleguideStringUtils.toJavaClassCase(name) + "Field";
    }

    @Override
    public String getJavaFieldType(TemplateImportsBuilder importsBuilder) {
        if (isStringMap()) {
            importsBuilder.add(Map.class.getName());
            return "Map<String, String>";

        } else {
            TemplateFieldType fieldType = getEffectiveValueType();
            importsBuilder.add(fieldType);
            return fieldType.getLocalClassName();
        }
    }

    @Override
    public Set<TemplateFieldType> getFieldValueTypes() {

        Set<TemplateFieldType> fieldTypes = new LinkedHashSet<>();

        if (!isStringMap()) {
            for (String templateType : templateTypes) {

                TemplateDefinition templateDef = templateDefinitions.getByName(templateType);
                if (templateDef != null) {
                    fieldTypes.add(templateDef);
                }
            }
        }

        return fieldTypes;
    }

    @Override
    public String getInterfaceBuilderMethodImplementationSource(int indent, TemplateImportsBuilder importsBuilder) {

        StringBuilder builder = new StringBuilder();

        builder.append(super.getInterfaceBuilderMethodImplementationSource(indent, importsBuilder));

        if (isStringMap()) {
            builder.append("\n\n");
            importsBuilder.add(LinkedHashMap.class.getName());

            StringBuilder notesJavaDoc = new StringBuilder();
            for (String note : notes) {
                notesJavaDoc.append(indent(indent)).append(" * <p>").append(note).append("</p>\n");
            }

            /*
             * <p>Adds an entry to the attributes field {@link java.util.Map}.</p>
             *
             * @param key the key to set.
             * @param value the value at the given key.
             * @return this builder.
             */
            /*
            public Builder addAttributes(String key, String value) {
                if (this.attributes == null) {
                    this.attributes = new LinkedHashMap<>();
                }
                if (key != null) {
                    this.attributes.put(key, value);
                }
                return this;
            }
            */
            String[] method = {
                    indent(indent) + "/**\n",
                    indent(indent) + " * <p>Adds an entry to the " + name + " field {@link java.util.Map}.</p>\n",
                    notesJavaDoc.toString(),
                    indent(indent) + " *\n",
                    indent(indent) + " * @param key the key to add.\n",
                    indent(indent) + " * @param value the value at the associated key.\n",
                    indent(indent) + " * @return this builder.\n",
                    indent(indent) + " */\n",
                    indent(indent) + "public Builder add" + StyleguideStringUtils.toJavaMethodCase(name) + "(String key, String value) {\n",
                    indent(indent + 1) + "if (this." + name + " == null) {\n",
                    indent(indent + 2) + "this." + name + " = new LinkedHashMap<>();\n",
                    indent(indent + 1) + "}\n",
                    indent(indent + 1) + "if (key != null) {\n",
                    indent(indent + 2) + "this." + name + ".put(key, value);\n",
                    indent(indent + 1) + "}\n",
                    indent(indent + 1) + "return this;\n",
                    indent(indent) + "}"
            };

            builder.append(Arrays.stream(method).collect(Collectors.joining("")));
        }

        return builder.toString();
    }

    private String getEffectiveTemplateType() {
        if (templateTypes.size() == 1) {
            return templateTypes.iterator().next();
        } else {
            return null;
        }
    }

    private boolean isStringMap() {
        String effectiveTemplateType = getEffectiveTemplateType();
        return effectiveTemplateType != null && mapTemplates.contains(effectiveTemplateType);
    }
}
