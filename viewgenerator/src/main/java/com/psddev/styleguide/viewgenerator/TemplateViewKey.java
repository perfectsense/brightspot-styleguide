package com.psddev.styleguide.viewgenerator;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
    public String toString() {
        return templateType + ": " + templatePath.toString();
    }

    @Override
    public String getFullyQualifiedClassName() {

        StringBuilder builder = new StringBuilder();

        String packagePrefix = getPackagePrefix();
        if (packagePrefix == null) {
            packagePrefix = "";
        }

        String packageSuffix;
        if (templatePath.getNameCount() > 1) {

            String[] templatePathParts = new String[templatePath.getNameCount() - 1];
            for (int i = 0; i < templatePathParts.length; i++) {
                templatePathParts[i] = templatePath.getName(i).toString();
            }
            packageSuffix = Arrays.stream(templatePathParts).collect(Collectors.joining("."));

        } else {
            packageSuffix = "";
        }

        for (char c : (packagePrefix + "." + packageSuffix).toCharArray()) {
            // excludes characters that would be invalid in a java package name
            if (Character.isJavaIdentifierPart(c) || c == '.') {
                builder.append(c);
            }
        }

        builder.append('.');

        // get just the template file name
        String templateName = templatePath.getName(templatePath.getNameCount() - 1).toString();

        // remove the template extension
        templateName = StringUtils.removeEnd(templateName, "." + templateType.getExtension());

        // convert the name to proper Java class name casing
        builder.append(ViewClassStringUtils.toJavaClassCase(templateName));

        // append "View"
        builder.append("View");

        // clean up any extraneous dots before returning
        return StringUtils.removeStart(builder.toString().replaceAll("\\.+", "."), ".");
    }

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
            args.put("value", templatePath.toString());
            return args;

        } else {
            // Other template types are not currently supported...
            return Collections.emptyMap();
        }
    }
}
