package com.psddev.styleguide.viewgenerator;

import java.util.Set;
import java.util.TreeSet;

/**
 * Manages all of the imports that need to be included in the generated view
 * class. Takes care of removing duplicates and discarding redundant or implicit
 * imports.
 */
class ViewClassImportsBuilder {

    /**
     * A placeholder String for where in the generated file the import
     * definitions will be placed since the imports are declared first, but the
     * entire list isn't known until the entire file has been processed.
     */
    public static final String PLACEHOLDER = "${importsPlaceholder}";

    private ViewClassDefinition viewDefinition;

    private Set<String> imports = new TreeSet<>();

    /**
     * Creates a new import statement builder for the given view class definition.
     *
     * @param viewDefinition the view class definition that imports
     */
    public ViewClassImportsBuilder(ViewClassDefinition viewDefinition) {
        this.viewDefinition = viewDefinition;
    }

    /**
     * Adds a fully qualified class name to the list.
     *
     * @param fullyQualifiedClassName the fully qualified class name to add.
     */
    public void add(String fullyQualifiedClassName) {
        if (!isSamePackage(fullyQualifiedClassName) && !fullyQualifiedClassName.startsWith("java.lang.")) {
            imports.add(fullyQualifiedClassName);
        }
    }

    /**
     * Adds a view class field type to the list.
     *
     * @param fieldType the view class field type to add.
     */
    public void add(ViewClassFieldType fieldType) {
        add(fieldType.getFullyQualifiedClassName());
    }

    /**
     * Gets the list of import statements as they would appear in a Java class
     * file.
     *
     * @return the import statements source code.
     */
    public String getImportStatements() {
        StringBuilder builder = new StringBuilder();

        for (String importClass : imports) {
            builder.append("import ").append(importClass).append(";\n");
        }

        return builder.toString();
    }

    /*
     * Returns true if the specified {@code fieldType} is in the same package
     * as the view class definition that this builder is managing to determine
     * if an import statement is needed or not.
     */
    private boolean isSamePackage(ViewClassFieldType fieldType) {
        return viewDefinition.getPackageName().equals(fieldType.getPackageName());
    }

    /*
     * Returns true if the specified {@code fullyQualifiedClassName} is in the
     * same package as the view class definition that this builder is managing
     * to determine if an import statement is needed or not.
     */
    private boolean isSamePackage(String fullyQualifiedClassName) {
        return isSamePackage(() -> fullyQualifiedClassName);
    }
}
