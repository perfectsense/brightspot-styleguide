package com.psddev.styleguide;

import com.psddev.dari.util.ObjectUtils;

/**
 * Represents a snippet of Javadoc that links to another class.
 */
class JavadocsClassLink implements Comparable<JavadocsClassLink> {

    private TemplateDefinition templateDef;
    private TemplateFieldType fieldType;

    public JavadocsClassLink(TemplateDefinition templateDef, TemplateFieldType fieldType) {
        this.templateDef = templateDef;
        this.fieldType = fieldType;
    }

    public String toJavadocLinkSnippet() {

        StringBuilder javadocLinkBuilder = new StringBuilder();

        javadocLinkBuilder.append("{@link ");

        if (fieldType instanceof NativeJavaTemplateFieldType) {
            javadocLinkBuilder.append(fieldType.getFullyQualifiedClassName());

        } else {
            if (templateDef != null && fieldType.hasSamePackageAs(templateDef)) {
                javadocLinkBuilder.append(fieldType.getClassName());

            } else {
                javadocLinkBuilder.append(fieldType.getFullyQualifiedClassName());
                javadocLinkBuilder.append(" ");
                javadocLinkBuilder.append(fieldType.getClassName());
            }
        }

        javadocLinkBuilder.append("}");

        return javadocLinkBuilder.toString();
    }

    @Override
    public int compareTo(JavadocsClassLink other) {
        return ObjectUtils.compare(
                this.fieldType.getClassName(),
                other != null ? other.fieldType.getClassName() : null,
                true);
    }
}
