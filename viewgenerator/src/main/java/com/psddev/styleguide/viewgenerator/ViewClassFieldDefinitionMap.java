package com.psddev.styleguide.viewgenerator;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.styleguide.JsonObject;

class ViewClassFieldDefinitionMap extends ViewClassFieldDefinition {

    public ViewClassFieldDefinitionMap(ViewClassDefinitions templateDefinitions, String parentTemplate, String name, List<JsonObject> values, Set<String> mapTemplates, String javaClassNamePrefix, boolean isDefaulted, boolean isStrictlyTyped) {
        super(templateDefinitions, parentTemplate, name, values, mapTemplates, javaClassNamePrefix, isDefaulted, isStrictlyTyped);
    }

    @Override
    public String getJavaFieldType(ViewClassImportsBuilder importsBuilder) {
        importsBuilder.add(Map.class.getName());
        return "Map<String, Object>";
    }

    @Override
    public Set<ViewClassFieldType> getFieldValueTypes() {
        return Collections.singleton(ViewClassFieldNativeJavaType.MAP);
    }

    @Override
    public String getInterfaceBuilderMethodImplementationSource(int indent, ViewClassImportsBuilder importsBuilder) {

        StringBuilder builder = new StringBuilder();

        builder.append(super.getInterfaceBuilderMethodImplementationSource(indent, importsBuilder));
        builder.append("\n\n");

        importsBuilder.add(LinkedHashMap.class.getName());

        ViewClassJavadocsBuilder javadocsBuilder = new ViewClassJavadocsBuilder();
        javadocsBuilder.add("Adds an entry to the " + name + " field .").addLink("java.util.Map").addLine(".");
        notes.forEach(javadocsBuilder::addParagraph);
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
        String[] method = {
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

        builder.append(Arrays.stream(method).collect(Collectors.joining("")));

        return builder.toString();
    }
}
