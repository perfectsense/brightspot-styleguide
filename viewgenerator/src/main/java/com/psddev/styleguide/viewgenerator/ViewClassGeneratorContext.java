package com.psddev.styleguide.viewgenerator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ViewClassGeneratorContext {

    private Path jsonDirectory;
    private Path javaSourceDirectory;

    private Set<String> excludedPaths;

    private boolean generateDefaultMethods = false;
    private boolean generateStrictTypes = true;

    private List<ViewClassDefinition> classDefinitions = new ArrayList<>();

    public ViewClassGeneratorContext(Path jsonDirectory, Path javaSourceDirectory) {
        this.jsonDirectory = jsonDirectory;
        this.javaSourceDirectory = javaSourceDirectory;
    }

    public ViewClassGeneratorContext() {
    }

    public Path getJsonDirectory() {
        return jsonDirectory;
    }

    public void setJsonDirectory(Path jsonDirectory) {
        this.jsonDirectory = jsonDirectory;
    }

    public Path getJavaSourceDirectory() {
        return javaSourceDirectory;
    }

    public void setJavaSourceDirectory(Path javaSourceDirectory) {
        this.javaSourceDirectory = javaSourceDirectory;
    }

    public Set<String> getExcludedPaths() {
        if (excludedPaths == null) {
            excludedPaths = new HashSet<>();
        }
        return excludedPaths;
    }

    public void setExcludedPaths(Set<String> excludedPaths) {
        this.excludedPaths = excludedPaths;
    }

    public boolean isGenerateDefaultMethods() {
        return generateDefaultMethods;
    }

    public void setGenerateDefaultMethods(boolean generateDefaultMethods) {
        this.generateDefaultMethods = generateDefaultMethods;
    }

    public boolean isGenerateStrictTypes() {
        return generateStrictTypes;
    }

    public void setGenerateStrictTypes(boolean generateStrictTypes) {
        this.generateStrictTypes = generateStrictTypes;
    }

    public ViewClassDefinition createViewClassDefinition(ViewKey viewKey, Set<JsonViewMap> jsonViewMaps) {
        ViewClassDefinition classDef = new ViewClassDefinition(this, viewKey, jsonViewMaps);
        classDef.validate();
        classDefinitions.add(classDef);
        return classDef;
    }

    public List<ViewClassDefinition> getClassDefinitions() {
        return Collections.unmodifiableList(classDefinitions);
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

    @Deprecated
    public boolean isRelativePaths() {
        return relativePaths;
    }

    @Deprecated
    public void setRelativePaths(boolean relativePaths) {
        this.relativePaths = relativePaths;
    }

    @Deprecated
    public String getDefaultJavaPackagePrefix() {
        return defaultJavaPackagePrefix;
    }

    @Deprecated
    public void setDefaultJavaPackagePrefix(String defaultJavaPackagePrefix) {
        this.defaultJavaPackagePrefix = defaultJavaPackagePrefix;
    }

    @Deprecated
    public String getDefaultTemplateExtension() {
        return defaultTemplateExtension;
    }

    @Deprecated
    public void setDefaultTemplateExtension(String defaultTemplateExtension) {
        this.defaultTemplateExtension = defaultTemplateExtension;
    }
}
