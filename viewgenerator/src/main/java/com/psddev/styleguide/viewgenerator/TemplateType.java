package com.psddev.styleguide.viewgenerator;

import java.util.Arrays;

import com.psddev.dari.util.StringUtils;

/**
 * A Java template engine type.
 */
enum TemplateType {

    FREEMARKER("ftl"),
    HANDLEBARS("hbs"),
    JSP("jsp");

    private String extension;

    TemplateType(String extension) {
        this.extension = extension;
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
