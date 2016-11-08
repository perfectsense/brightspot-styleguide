package com.psddev.styleguide.viewgenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.psddev.dari.util.StringUtils;

/**
 * A unique key for a particular view, whose name will ultimately drive what
 * the Java class name and package as well as the view renderer annotation will
 * be for the resulting view interface.
 */
class ViewKey implements ViewClassFieldType {

    protected ViewClassGeneratorContext context;

    protected String name;

    protected List<String> errors = new ArrayList<>();

    private boolean validated;

    /**
     * Creates a new view key with the given name.
     *
     * @param name the view key name.
     */
    public ViewKey(ViewClassGeneratorContext context, String name) {
        this.context = context;
        this.name = name;
    }

    /**
     * Gets the view key name.
     *
     * @return the name of the view key.
     */
    public final String getName() {
        return name;
    }

    /**
     * Validates that this view key contains no errors. If there are any, they
     * can be retrieved by called {@link #getErrors()}.
     */
    public final void validate() {

        if (validated) {
            return;
        }

        doValidate();

        validated = true;
    }

    /**
     * Validates that this view key contains no errors. If there are any, they
     * can be retrieved by called {@link #getErrors()}. Sub-classes may override
     * this method to provide different validation.
     */
    protected void doValidate() {

        int lastDotAt = name.lastIndexOf('.');
        if (lastDotAt >= 0) {

            String packageName = name.substring(0, lastDotAt);
            String className = name.substring(lastDotAt + 1);

            if (!ViewClassStringUtils.isValidJavaPackageName(packageName)) {
                errors.add("ViewKey [" + name + "] has an invalid java package name.");
            }

            if (!ViewClassStringUtils.isValidJavaClassName(className)) {
                errors.add("ViewKey [" + name + "] has an invalid java class name.");
            }

        } else {

            if (!ViewClassStringUtils.isValidJavaClassName(name)) {
                errors.add("ViewKey [" + name + "] has an invalid java class name.");
            }
        }
    }

    /**
     * Gets the list of errors (if any) for this view key.
     *
     * @return the list of errors.
     */
    public List<String> getErrors() {
        validate();
        return errors;
    }

    /**
     * Checks if this view key contains any errors.
     *
     * @return true if there are errors, false otherwise.
     */
    public boolean hasAnyErrors() {
        validate();
        return !errors.isEmpty();
    }

    /**
     * The name of the view renderer annotation class.
     *
     * @return the class name for the view renderer annotation.
     */
    public String getAnnotationClass() {
        return "com.psddev.cms.view.JsonView";
    }

    /**
     * This is a simplification of what is actually possible with annotation
     * declarations, but since we control the types of templates that we support
     * we can expand this logic to support more complex structures as needed.
     *
     * @return the map of annotation arguments needed to write out the annotation declaration.
     */
    public Map<String, String> getAnnotationArguments() {
        return Collections.emptyMap();
    }

    @Override
    public String getFullyQualifiedClassName() {

        String packagePrefix = context.getDefaultJavaPackagePrefix();
        if (packagePrefix == null) {
            packagePrefix = "";
        } else {
            packagePrefix = StringUtils.ensureEnd(packagePrefix, ".");
        }

        return StringUtils.ensureEnd(packagePrefix + StringUtils.removeSurrounding(name, "."), "View");
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public final boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ViewKey viewKey = (ViewKey) o;

        return name.equals(viewKey.name);
    }

    @Override
    public final int hashCode() {
        return name.hashCode();
    }
}
