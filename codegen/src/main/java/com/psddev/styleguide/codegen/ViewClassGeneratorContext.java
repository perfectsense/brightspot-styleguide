package com.psddev.styleguide.codegen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The overall context (combination of state and settings) for a view class
 * generation operation.
 */
class ViewClassGeneratorContext {

    private Set<Path> jsonDirectories;
    private Path javaSourceDirectory;

    private Set<String> excludedPaths;

    private boolean generateDefaultMethods = false;
    private boolean generateStrictTypes = true;

    private List<ViewClassDefinition> classDefinitions = new ArrayList<>();

    /**
     * Creates a new instance with the default settings.
     */
    public ViewClassGeneratorContext() {
    }

    /**
     * Gets the directory paths where all of the JSON data files and
     * templates live.
     *
     * @return the JSON directory path.
     */
    public Set<Path> getJsonDirectories() {
        if (jsonDirectories == null) {
            jsonDirectories = new LinkedHashSet<>();
        }
        return jsonDirectories;
    }

    /**
     * Sets the directory paths where all of the JSON data files and templates
     * live.
     *
     * @param jsonDirectories the JSON directory paths to set.
     */
    public void setJsonDirectories(Set<Path> jsonDirectories) {
        this.jsonDirectories = jsonDirectories;
    }

    /**
     * Gets the directory path where the generated source files should be placed.
     *
     * @return the Java source file directory.
     */
    public Path getJavaSourceDirectory() {
        return javaSourceDirectory;
    }

    /**
     * Sets the directory path where the generated source files should be placed.
     *
     * @param javaSourceDirectory the Java source file directory.
     */
    public void setJavaSourceDirectory(Path javaSourceDirectory) {
        this.javaSourceDirectory = javaSourceDirectory;
    }

    /**
     * Gets the set of path names that should be excluded by default when
     * searching for JSON files in the {@link #getJsonDirectories() JSON directories}.
     *
     * @return the set of path names to exclude.
     */
    public Set<String> getExcludedPaths() {
        if (excludedPaths == null) {
            excludedPaths = new LinkedHashSet<>();
        }
        return excludedPaths;
    }

    /**
     * Sets the path names that should be excluded by default when searching
     * for JSON files in the {@link #getJsonDirectories() JSON directories}.
     *
     * @param excludedPaths the set of path names to exclude.
     */
    public void setExcludedPaths(Set<String> excludedPaths) {
        this.excludedPaths = excludedPaths;
    }

    /**
     * Returns true if the generated view interface classes should contain
     * default interface methods or not. The default is {@code false}.
     *
     * @return true if the view interface should contain default methods, false otherwise.
     */
    public boolean isGenerateDefaultMethods() {
        return generateDefaultMethods;
    }

    /**
     * Sets whether the generated view interface classes should contain default
     * interface methods or not.
     *
     * @param generateDefaultMethods true if the view interface should contain
     *                               default methods, false otherwise.
     */
    public void setGenerateDefaultMethods(boolean generateDefaultMethods) {
        this.generateDefaultMethods = generateDefaultMethods;
    }

    /**
     * Returns true if the generated view interface class methods should have
     * strict return types or not. A non-strict return type is one that just
     * returns {@code Object}. The default is {@code true}.
     *
     * @return true if the view interface methods should have strict return
     *         types, false otherwise.
     */
    public boolean isGenerateStrictTypes() {
        return generateStrictTypes;
    }

    /**
     * Sets whether the generated view interface class methods should have
     * strict return types or not.
     *
     * @param generateStrictTypes true if the view interface methods should
     *                            have strict return types, false otherwise.
     */
    public void setGenerateStrictTypes(boolean generateStrictTypes) {
        this.generateStrictTypes = generateStrictTypes;
    }

    /**
     * Returns the list of view class definitions that have been created thus
     * far in a view class generation operation.
     *
     * @return the list of view class definitions.
     */
    public List<ViewClassDefinition> getClassDefinitions() {
        return new ArrayList<>(classDefinitions);
    }

    /**
     * Sets the list of view class definitions that have been created.
     *
     * @param classDefinitions the class definitions to set.
     */
    public void setClassDefinitions(List<ViewClassDefinition> classDefinitions) {
        this.classDefinitions = classDefinitions;
    }

    @Deprecated
    private String defaultJavaPackagePrefix;

    /**
     * Gets the default java package prefix that should be used for view
     * classes that don't have one defined otherwise.
     *
     * @return the default java package prefix name.
     */
    @Deprecated
    public String getDefaultJavaPackagePrefix() {
        return defaultJavaPackagePrefix;
    }

    /**
     * Sets the default java package prefix that should be used for view
     * classes that don't have one defined otherwise.
     *
     * @param defaultJavaPackagePrefix the default java package prefix name.
     */
    @Deprecated
    public void setDefaultJavaPackagePrefix(String defaultJavaPackagePrefix) {
        this.defaultJavaPackagePrefix = defaultJavaPackagePrefix;
    }
}
