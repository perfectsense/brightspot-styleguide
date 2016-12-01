package com.psddev.styleguide.codegen;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.psddev.dari.util.StringUtils;

/**
 * A specialized ViewKey that also has a template component.
 */
class TemplateViewKey extends ViewKey {

    private JsonDirectory jsonDirectory;

    private Path templatePath;

    private TemplateType templateType;

    private List<ViewConfiguration> viewConfigs;

    /**
     * Creates a new template based view key.
     *
     * @param jsonDirectory the JSON directory where the template lives.
     * @param name the name of the view key.
     * @param templatePath the path to the template.
     * @param templateType the type of template referenced by the templatePath.
     * @param viewConfigs the configurations for this template.
     */
    public TemplateViewKey(JsonDirectory jsonDirectory, String name, Path templatePath, TemplateType templateType, List<ViewConfiguration> viewConfigs) {
        super(jsonDirectory.getContext(), name != null ? name : templatePath.toString());
        this.jsonDirectory = jsonDirectory;
        this.templatePath = templatePath;
        this.templateType = templateType;
        this.viewConfigs = viewConfigs;
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
    public List<ViewConfiguration> getViewConfigs() {
        return viewConfigs;
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

        // Find the closest config that defines a java package prefix.
        ViewConfiguration packageConfig = viewConfigs.stream()
                .filter(c -> c.getJavaPackage() != null)
                .findFirst()
                .orElse(null);

        // Get the java package prefix value, or fallback to the context default.
        String packagePrefix = packageConfig != null ? packageConfig.getJavaPackage() : context.getDefaultJavaPackagePrefix();
        if (packagePrefix == null) {
            packagePrefix = "";
        }

        // Gets the directory path of the closest config that defines a java package prefix, or default to the JSON directory if none defined.
        Path packageConfigPath = packageConfig != null ? packageConfig.getPath().getParent() : jsonDirectory.getPath();

        // Gets the relativized template path that will be used to construct the final java package name.
        Path packageConfigRelativeTemplatePath = packageConfigPath.relativize(jsonDirectory.getPath().resolve(templatePath));

        String packageSuffix = ViewClassStringUtils.toJavaPackageName(packageConfigRelativeTemplatePath);
        String className = ViewClassStringUtils.toJavaClassName(packageConfigRelativeTemplatePath);

        String fqcn = packagePrefix + "." + packageSuffix + "." + className + "View";

        // clean up any extraneous dots before returning
        return StringUtils.removeStart(fqcn.replaceAll("\\.+", "."), ".");
    }

    /*
     * Helper method to get the effective package prefix by looking first at
     * the template's config files, and then falling back to the legacy view
     * generator context property that allows it to be specified globally.
     */
    private String getPackagePrefix() {

        String packagePrefix = viewConfigs.stream()
                .map(ViewConfiguration::getJavaPackage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (packagePrefix == null) {
            packagePrefix = context.getDefaultJavaPackagePrefix();
        }

        return packagePrefix;
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
