package com.psddev.styleguide.viewgenerator;

import java.nio.file.Path;

/**
 * A specialized ViewKey that also has a template component.
 */
class TemplateViewKey extends ViewKey {

    private Path templatePath;

    private TemplateType templateType;

    /**
     * Creates a new template based view key.
     *
     * @param name the name of the view key.
     * @param templatePath the path to the template.
     * @param templateType the type of template referenced by the templatePath.
     */
    public TemplateViewKey(String name, Path templatePath, TemplateType templateType) {
        super(name);
        this.templatePath = templatePath;
        this.templateType = templateType;
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

    @Override
    public String toString() {
        return templateType + ": " + templatePath.toString();
    }
}
