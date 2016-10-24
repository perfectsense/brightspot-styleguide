package com.psddev.styleguide.viewgenerator;

import java.util.Set;
import java.util.TreeSet;

/**
 * Manages all of the imports that need to be included in the generated view class.
 */
class ViewClassImportsBuilder {

    public static final String PLACEHOLDER = "${importsPlaceholder}";

    private ViewClassDefinition viewDefinition;

    private Set<String> imports = new TreeSet<>();

    public ViewClassImportsBuilder(ViewClassDefinition viewDefinition) {
        this.viewDefinition = viewDefinition;
    }

    public void add(String fullyQualifiedClassName) {
        if (!isSamePackage(fullyQualifiedClassName) && !fullyQualifiedClassName.startsWith("java.lang.")) {
            imports.add(fullyQualifiedClassName);
        }
    }

    public void add(ViewClassFieldType fieldType) {
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

        for (String importClass : imports) {
            builder.append("import ").append(importClass).append(";\n");
        }

        return builder.toString();
    }

    private boolean isSamePackage(ViewClassFieldType fieldType) {
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
