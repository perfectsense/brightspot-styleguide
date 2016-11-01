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

    private Set<Path> excludedPaths;
    private Set<String> excludedPathNames;

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

    public Set<Path> getExcludedPaths() {
        if (excludedPaths == null) {
            excludedPaths = new HashSet<>();
        }
        return excludedPaths;
    }

    public void setExcludedPaths(Set<Path> excludedPaths) {
        this.excludedPaths = excludedPaths;
    }

    public Set<String> getExcludedPathNames() {
        if (excludedPathNames == null) {
            excludedPathNames = new HashSet<>();
        }
        return excludedPathNames;
    }

    public void setExcludedPathNames(Set<String> excludedPathNames) {
        this.excludedPathNames = excludedPathNames;
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

    // To support backward compatibility
    @Deprecated
    private String defaultJavaPackagePrefix;

    @Deprecated
    public String getDefaultJavaPackagePrefix() {
        return defaultJavaPackagePrefix;
    }

    @Deprecated
    public void setDefaultJavaPackagePrefix(String defaultJavaPackagePrefix) {
        this.defaultJavaPackagePrefix = defaultJavaPackagePrefix;
    }
}
