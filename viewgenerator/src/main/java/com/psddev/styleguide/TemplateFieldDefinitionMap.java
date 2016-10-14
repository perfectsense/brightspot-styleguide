package com.psddev.styleguide;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class TemplateFieldDefinitionMap extends TemplateFieldDefinition {

    public TemplateFieldDefinitionMap(TemplateDefinitions templateDefinitions, String parentTemplate, String name, List<JsonObject> values, Set<String> mapTemplates, String javaClassNamePrefix) {
        super(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix);
    }

    @Override
    public String getJavaFieldType(TemplateImportsBuilder importsBuilder) {
        importsBuilder.add(Map.class.getName());
        return "Map<String, Object>";
    }

    @Override
    public Set<TemplateFieldType> getFieldValueTypes() {
        return Collections.singleton(NativeJavaTemplateFieldType.MAP);
    }

    @Override
    public String getInterfaceBuilderMethodImplementationSource(int indent, TemplateImportsBuilder importsBuilder) {

        StringBuilder builder = new StringBuilder();

        builder.append(super.getInterfaceBuilderMethodImplementationSource(indent, importsBuilder));
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
        String[] method = {
                indent(indent) + "/**\n",
                indent(indent) + " * <p>Adds an entry to the " + name + " field {@link java.util.Map}.</p>\n",
                notesJavaDoc.toString(),
                indent(indent) + " *\n",
                indent(indent) + " * @param key the key to add.\n",
                indent(indent) + " * @param value the value at the associated key.\n",
                indent(indent) + " * @return this builder.\n",
                indent(indent) + " */\n",
                indent(indent) + "public Builder add" + StyleguideStringUtils.toJavaMethodCase(name) + "(String key, Object value) {\n",
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

        return builder.toString();
    }
}
