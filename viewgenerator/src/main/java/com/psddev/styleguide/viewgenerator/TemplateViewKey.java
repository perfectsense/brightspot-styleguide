package com.psddev.styleguide.viewgenerator;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.psddev.dari.util.StringUtils;

/**
 * A specialized ViewKey that also has a template component.
 */
class TemplateViewKey extends ViewKey {

    private Path templatePath;

    private TemplateType templateType;

    private TemplateViewConfiguration templateConfig;

    /**
     * Creates a new template based view key.
     *
     * @param context the view class generator context.
     * @param name the name of the view key.
     * @param templatePath the path to the template.
     * @param templateType the type of template referenced by the templatePath.
     * @param templateConfig the configuration for this template.
     */
    public TemplateViewKey(ViewClassGeneratorContext context, String name, Path templatePath, TemplateType templateType, TemplateViewConfiguration templateConfig) {
        super(context, name != null ? name : templatePath.toString());
        this.templatePath = templatePath;
        this.templateType = templateType;
        this.templateConfig = templateConfig;
    }

    /**
     * Gets the template path.
     *
     * @return the template path.
     */
    public Path getTemplatePath() {
        return templatePath;
    }

    /**
     * Gets the template type.
     *
     * @return the template type.
     */
    public TemplateType getTemplateType() {
        return templateType;
    }

    /**
     * Gets the template configuration. Can be null.
     *
     * @return the template configuration. Can be null.
     */
    public TemplateViewConfiguration getTemplateConfig() {
        return templateConfig;
    }

    @Override
    protected void doValidate() {

        if (ViewClassStringUtils.toJavaPackageName(templatePath) == null) {
            errors.add("Template [" + name + "] produces an invalid Java package name.");
        }

        if (ViewClassStringUtils.toJavaClassName(templatePath) == null) {
            errors.add("Template [" + name + "] produces an invalid Java class name.");
        }
    }

    @Override
    public String toString() {
        return templateType + ": " + templatePath.toString();
    }

    @Override
    public String getFullyQualifiedClassName() {

        String packagePrefix = getPackagePrefix();
        if (packagePrefix == null) {
            packagePrefix = "";
        }

        String packageSuffix = ViewClassStringUtils.toJavaPackageName(templatePath);
        String className = ViewClassStringUtils.toJavaClassName(templatePath);

        String fqcn = packagePrefix + "." + packageSuffix + "." + className + "View";

        // clean up any extraneous dots before returning
        return StringUtils.removeStart(fqcn.replaceAll("\\.+", "."), ".");
    }

    /*
     * Helper method to get the effective package prefix by looking first at
     * the template's config file, and then falling back to the legacy view
     * generator context property that allows it to be specified globally.
     */
    private String getPackagePrefix() {

        if (templateConfig != null) {
            String javaPackage = templateConfig.getJavaPackage();
            if (javaPackage != null) {
                return javaPackage;
            }
        }

        return context.getDefaultJavaPackagePrefix();
    }

    @Override
    public String getAnnotationClass() {
        return templateType.getAnnotationClass();
    }

    @Override
    public Map<String, String> getAnnotationArguments() {

        if (templateType == TemplateType.HANDLEBARS) {
            Map<String, String> args = new LinkedHashMap<>();
            args.put("value", StringUtils.removeEnd(templatePath.toString(), "." + templateType.getExtension()));
            return args;

        } else {
            // Other template types are not currently supported...
            return Collections.emptyMap();
        }
    }
}