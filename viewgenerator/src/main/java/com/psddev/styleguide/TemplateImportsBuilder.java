package com.psddev.styleguide;

import java.util.Set;
import java.util.TreeSet;

/**
 * Manages all of the imports that need to be included in the generated view class.
 */
class TemplateImportsBuilder {

    public static final String PLACEHOLDER = "${importsPlaceholder}";

    private TemplateDefinition viewDefinition;

    private Set<String> imports = new TreeSet<>();

    public TemplateImportsBuilder(TemplateDefinition viewDefinition) {
        this.viewDefinition = viewDefinition;
    }

    public void add(String fullyQualifiedClassName) {
        if (!isSamePackage(fullyQualifiedClassName) && !fullyQualifiedClassName.startsWith("java.lang.")) {
            imports.add(fullyQualifiedClassName);
        }
    }

    public void add(TemplateFieldType fieldType) {
        add(fieldType.getFullyQualifiedClassName());
    }

    public boolean addIfExists(String fullyQualifiedClassName) {
        if (classExists(fullyQualifiedClassName)) {
            add(fullyQualifiedClassName);
            return true;
        } else {
            return false;
        }
    }

    public String getImportStatements() {
        StringBuilder builder = new StringBuilder();

        // TODO: Sort!
        for (String importClass : imports) {
            builder.append("import ").append(importClass).append(";\n");
        }

        return builder.toString();
    }

    private boolean isSamePackage(TemplateFieldType fieldType) {
        return viewDefinition.getPackageName().equals(fieldType.getPackageName());
    }

    private boolean isSamePackage(String fullyQualifiedClassName) {
        return isSamePackage(() -> fullyQualifiedClassName);
    }

    // Checks if a class exists for the given className.
    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
