package com.psddev.styleguide.viewgenerator;

import java.util.Arrays;

import com.psddev.dari.util.StringUtils;

/**
 * A Java template engine type.
 */
enum TemplateType {

    FREEMARKER("ftl", /* Not yet implemented */ null),
    HANDLEBARS("hbs", "com.psddev.handlebars.HandlebarsTemplate"),
    JSP("jsp", /* Not yet implemented */ null);

    private String extension;
    private String annotationClass;

    TemplateType(String extension, String annotationClass) {
        this.extension = extension;
        this.annotationClass = annotationClass;
    }

    /**
     * Gets the file extension for this type of template.
     *
     * @return the template file extension.
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Gets the view renderer annotation class name for this template type.
     *
     * @return the view renderer annotation class name for this template type.
     */
    public String getAnnotationClass() {
        return annotationClass;
    }

    /**
     * Finds a template type based on the provided file extension.
     *
     * @param extension the extension to lookup.
     * @return the template type for the given extension.
     */
    public static TemplateType findByExtension(String extension) {
        return Arrays.stream(TemplateType.values())
                .filter(type -> type.getExtension().equalsIgnoreCase(StringUtils.removeStart(extension, ".")))
                .findAny()
                .orElse(null);
    }

    @Override
    public String toString() {
        return name();
    }
}
