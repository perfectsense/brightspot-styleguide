package com.psddev.styleguide;

public class ViewClassSource {

    private String packageName;
    private String className;
    private String sourceCode;

    public ViewClassSource(String packageName, String className, String sourceCode) {
        this.packageName = packageName;
        this.className = className;
        this.sourceCode = sourceCode;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public String getSourceCode() {
        return sourceCode;
    }
}
