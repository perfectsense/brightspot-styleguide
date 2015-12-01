package com.psddev.styleguide;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class TemplateFieldDefinitionMap extends TemplateFieldDefinition {

    public TemplateFieldDefinitionMap(String parentTemplate, String name, List<JsonObject> values, List<String> mapTemplates) {
        super(parentTemplate, name, values, mapTemplates);
    }

    @Override
    public String getJavaFieldType(Set<String> imports) {
        imports.add(Map.class.getName());
        return "Map<String, Object>";
    }

    @Override
    public Set<String> getValueTypes() {
        return Collections.singleton("java.util.Map");
    }

    @Override
    public String getInterfaceBuilderMethodImplementationSource(int indent, Set<String> imports) {

        StringBuilder builder = new StringBuilder();

        builder.append(super.getInterfaceBuilderMethodImplementationSource(indent, imports));
        builder.append("\n\n");

        imports.add("java.util.LinkedHashMap");

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
                indent(indent) + "public Builder add" + StyleguideStringUtils.toPascalCase(name) + "(String key, Object value) {\n",
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
