package com.psddev.styleguide.viewgenerator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The overall context (combination of state and settings) for a view class
 * generation operation.
 */
class ViewClassGeneratorContext {

    private Path jsonDirectory;
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
     * Gets the directory path where all of the JSON data files and templates live.
     *
     * @return the JSON directory path.
     */
    public Path getJsonDirectory() {
        return jsonDirectory;
    }

    /**
     * Sets the directory path where all of the JSON data files and templates live.
     *
     * @param jsonDirectory the JSON directory path.
     */
    public void setJsonDirectory(Path jsonDirectory) {
        this.jsonDirectory = jsonDirectory;
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
     * searching for JSON files in the {@link #getJsonDirectory() JSON directory}.
     *
     * @return the set of path names to exclude.
     */
    public Set<String> getExcludedPaths() {
        if (excludedPaths == null) {
            excludedPaths = new HashSet<>();
        }
        return excludedPaths;
    }

    /**
     * Sets the path names that should be excluded by default when searching
     * for JSON files in the {@link #getJsonDirectory() JSON directory}.
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
     * Creates a new view class definition object, validates it, and keeps a
     * reference to it so that all of the created definitions can be analyzed
     * holistically.
     *
     * @param viewKey the desired view key
     * @param jsonViewMaps the set of JSON view maps that represent the class
     *                     definition.
     * @return a newly created and validated view class definition.
     */
    public ViewClassDefinition createViewClassDefinition(ViewKey viewKey, Set<JsonViewMap> jsonViewMaps) {
        ViewClassDefinition classDef = new ViewClassDefinition(this, viewKey, jsonViewMaps);
        classDef.validate();
        classDefinitions.add(classDef);
        return classDef;
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

    /*
     * To support backward compatibility
     */
    @Deprecated
    private boolean relativePaths = true;

    @Deprecated
    private String defaultJavaPackagePrefix;

    @Deprecated
    private String defaultTemplateExtension;

    /**
     * Returns true if all paths (i.e. those not starting with a slash) should
     * be treated as relative to the file the path was declared in. If set to
     * false, then path values of "_template" keys are considered absolute even
     * when they don't start with a slash. The default is true.
     *
     * @return true if relative paths is enabled.
     */
    @Deprecated
    public boolean isRelativePaths() {
        return relativePaths;
    }

    /**
     * Sets whether relative paths is enabled or not.
     *
     * @param relativePaths true if relative paths is enabled.
     */
    @Deprecated
    public void setRelativePaths(boolean relativePaths) {
        this.relativePaths = relativePaths;
    }

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

    /**
     * Gets the default template file extension that should be used for template
     * paths that don't have one specified.
     *
     * @return the default template file extension.
     */
    @Deprecated
    public String getDefaultTemplateExtension() {
        return defaultTemplateExtension;
    }

    /**
     * Sets the default template file extension that should be used for template
     * paths that don't have one specified.
     *
     * @param defaultTemplateExtension the default template file extension.
     */
    @Deprecated
    public void setDefaultTemplateExtension(String defaultTemplateExtension) {
        this.defaultTemplateExtension = defaultTemplateExtension;
    }
}
