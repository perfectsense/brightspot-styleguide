package com.psddev.styleguide.viewgenerator;

/**
 * All the metadata necessary to produce a Java class source file.
 */
class ViewClassSource {

    private String packageName;

    private String className;

    private String sourceCode;

    /**
     * Creates a new view class source object containing all the information
     * need to produce a Java source file.
     *
     * @param packageName the Java package name.
     * @param className the Java class name.
     * @param sourceCode the source code.
     */
    public ViewClassSource(String packageName, String className, String sourceCode) {
        this.packageName = packageName;
        this.className = className;
        this.sourceCode = sourceCode;
    }

    /**
     * Gets the package name for this view class. This determines which
     * directory the file is written to.
     *
     * @return the package name.
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Gets the class name for this view class. This determines the name of
     * file.
     *
     * @return the class name.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gets the source code for this view class. This determines the contents
     * of the file.
     *
     * @return the source code.
     */
    public String getSourceCode() {
        return sourceCode;
    }
}
