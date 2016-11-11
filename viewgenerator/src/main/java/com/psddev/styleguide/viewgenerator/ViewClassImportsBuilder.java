package com.psddev.styleguide.viewgenerator;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Manages all of the imports that need to be included in the generated view
 * class. Takes care of removing duplicates and discarding redundant or implicit
 * imports.
 *
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
     * Adds a fully qualified class name to the list of imports. If the class
     * is a native java class, or the class is in the same package as the
     * underlying view class definition, then it won't be added to the list
     * but it will just safely be ignored, and the method will still return
     * true. The method returns false if the class being added would cause a
     * conflict with the existing imports in that there already exists a class
     * with the same simple name.
     *
     * @param fullyQualifiedClassName the fully qualified class name to add.
     * @return true if adding the import wouldn't , false otherwise.
     */
    public boolean add(String fullyQualifiedClassName) {
        return add(() -> fullyQualifiedClassName);
    }

    /**
     * Adds a fieldType to the list of imports. If the class
     * is a native java class, or the class is in the same package as the
     * underlying view class definition, then it won't be added to the list
     * but it will just safely be ignored, and the method will still return
     * true. The method returns false if the class being added would cause a
     * conflict with the existing imports in that there already exists a class
     * with the same simple name.
     *
     * @param fieldType the view class field type to add.
     * @return true if adding the import wouldn't , false otherwise.
     */
    public boolean add(ViewClassFieldType fieldType) {

        if (!isSamePackage(fieldType) && !fieldType.getPackageName().equals("java.lang")) {

            if (imports.contains(fieldType.getFullyQualifiedClassName())) {
                return true;

            } else {
                if (!imports.stream()
                        .map(ViewClassFieldType::from)
                        .map(ViewClassFieldType::getLocalClassName)
                        .collect(Collectors.toSet())
                        .contains(fieldType.getLocalClassName())) {

                    imports.add(fieldType.getFullyQualifiedClassName());
                    return true;

                } else {
                    return false;
                }
            }
        } else {
            return true;
        }
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
