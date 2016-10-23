package com.psddev.styleguide.viewgenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import com.psddev.dari.util.CollectionUtils;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;

/**
 * A configuration for a view/template that specifies how a particular view
 * interface should be generated.
 */
class TemplateViewConfiguration {

    public static final String JAVA_PACKAGE_KEY = "javaPackage";
    public static final String TEMPLATE_TYPE_KEY = "templateEngine";

    private Map<String, Object> rawData;

    private String javaPackage;

    private TemplateType templateType;

    public TemplateViewConfiguration(Path configFilePath) throws IOException {

        String configFileData = IoUtils.toString(configFilePath.toFile(), StandardCharsets.UTF_8);

        Object configFileObject;
        try {
            configFileObject = ObjectUtils.fromJson(configFileData);

        } catch (RuntimeException e) {
            throw new IOException("Invalid configuration file format. Must be a valid JSON file.", e);
        }

        if (configFileObject instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> configFileMap = (Map<String, Object>) configFileObject;
            rawData = configFileMap;

        } else {
            throw new IllegalArgumentException("Invalid configuration file format. Must be a valid JSON map.");
        }
    }

    /**
     * Gets the java package that the view should be created in.
     *
     * @return the java package.
     */
    public String getJavaPackage() {
        if (javaPackage == null) {
            Object value = CollectionUtils.getByPath(rawData, JAVA_PACKAGE_KEY);
            if (value instanceof String) {
                javaPackage = (String) value;
            }
        }
        return javaPackage;
    }

    /**
     * Gets the type of template that the view is associated with, which in turn
     * determines which view renderer annotation gets placed on the resulting
     * view interface.
     *
     * @return the template type.
     */
    public TemplateType getTemplateType() {
        if (templateType == null) {

            Object value = CollectionUtils.getByPath(rawData, TEMPLATE_TYPE_KEY);
            if (value instanceof String) {
                templateType = TemplateType.findByExtension((String) value);
            }
        }
        return templateType;
    }

    @Override
    public String toString() {
        return "TemplateViewConfiguration{"
                + "javaPackage='" + javaPackage + '\''
                + ", templateType=" + templateType
                + '}';
    }
}
