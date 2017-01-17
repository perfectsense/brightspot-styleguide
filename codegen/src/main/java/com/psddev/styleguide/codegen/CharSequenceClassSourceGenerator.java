package com.psddev.styleguide.codegen;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.psddev.styleguide.codegen.ViewClassStringUtils.NEW_LINE;
import static com.psddev.styleguide.codegen.ViewClassStringUtils.indent;

/**
 * Generates classes with APIs for returning CharSequences as either plain text
 * or raw HTML.
 */
class CharSequenceClassSourceGenerator {

    static final String PACKAGE_NAME = "styleguide.core";

    static final String PLAIN_TEXT_CLASS_NAME = "PlainText";
    static final String RAW_HTML_CLASS_NAME = "RawHtml";

    private List<ViewClassDefinition> classDefinitions;

    CharSequenceClassSourceGenerator(List<ViewClassDefinition> classDefinitions) {
        this.classDefinitions = classDefinitions;
    }

    /**
     * Generates the source files for the RawHtml and PlainText CharSequence APIs.
     *
     * @return the sources for the RawHtml and PlainText APIs.
     */
    List<ViewClassSource> generateSources() {

        List<ViewClassFieldDefinition> mixedTypeFields = classDefinitions.stream()
                .map(ViewClassDefinition::getFieldDefinitions)
                .flatMap(Collection::stream)
                .filter(ViewClassFieldDefinition::hasMixedValueTypes)
                .collect(Collectors.toList());

        return Arrays.asList(
                getRawHtmlSource(mixedTypeFields),
                getPlainTextSource(mixedTypeFields));
    }

    private ViewClassSource getRawHtmlSource(List<ViewClassFieldDefinition> mixedTypeFields) {
        return getSource(RAW_HTML_CLASS_NAME, true, mixedTypeFields);
    }

    private ViewClassSource getPlainTextSource(List<ViewClassFieldDefinition> mixedTypeFields) {
        return getSource(PLAIN_TEXT_CLASS_NAME, false, mixedTypeFields);
    }

    private ViewClassSource getSource(String className, boolean isRaw, List<ViewClassFieldDefinition> mixedTypeFieldDefs) {

        ViewClassImportsBuilder importsBuilder = new ViewClassImportsBuilder(PACKAGE_NAME);

        importsBuilder.add("java.util.stream.IntStream");
        if (isRaw) {
            importsBuilder.add("com.psddev.cms.view.Raw");
        }

        StringBuilder sourceBuilder = new StringBuilder();

        // File header
        sourceBuilder.append(getSourceCodeHeaderComment());

        // Package declaration
        sourceBuilder.append("package ").append(PACKAGE_NAME).append(";").append(NEW_LINE);
        sourceBuilder.append(NEW_LINE);

        // Imports - we collect them as we process, so this just a placeholder which we'll replace at the end.
        sourceBuilder.append(ViewClassImportsBuilder.PLACEHOLDER);
        sourceBuilder.append(NEW_LINE);

        // sources.add(getRawHtmlSource(mixedTypeFields));
        sourceBuilder.append("public final class ").append(className).append(" implements ").append(isRaw ? "Raw" : "CharSequence");
        if (!mixedTypeFieldDefs.isEmpty()) {
            sourceBuilder.append(",");
        } else {
            sourceBuilder.append(" {");
        }
        sourceBuilder.append(NEW_LINE);
        {
            for (Iterator<ViewClassFieldDefinition> it = mixedTypeFieldDefs.iterator(); it.hasNext(); ) {

                ViewClassFieldDefinition fieldDef = it.next();

                sourceBuilder.append(indent(2));

                if (importsBuilder.add(fieldDef)) {
                    sourceBuilder.append(fieldDef.getClassName());
                } else {
                    sourceBuilder.append(fieldDef.getFullyQualifiedClassName());
                }

                if (it.hasNext()) {
                    sourceBuilder.append(",");
                } else {
                    sourceBuilder.append(" {");
                }

                sourceBuilder.append(NEW_LINE);
            }

            sourceBuilder.append(NEW_LINE);

            // private String value;
            sourceBuilder.append(indent(1)).append("private String value;").append(NEW_LINE);

            sourceBuilder.append(NEW_LINE);

            // // RawHtml(String value);
            sourceBuilder.append(indent(1)).append("private ").append(className).append("(String value) {").append(NEW_LINE);
            {
                sourceBuilder.append(indent(2)).append("this.value = value;").append(NEW_LINE);
            }
            sourceBuilder.append(indent(1)).append("}").append(NEW_LINE);

            sourceBuilder.append(NEW_LINE);

            // static RawHtml of(String value);
            sourceBuilder.append(indent(1)).append("public static ").append(className).append(" of(String value) {").append(NEW_LINE);
            {
                sourceBuilder.append(indent(2)).append("return new ").append(className).append("(value);").append(NEW_LINE);
            }
            sourceBuilder.append(indent(1)).append("}").append(NEW_LINE);

            sourceBuilder.append(NEW_LINE);

            // int length();
            sourceBuilder.append(indent(1)).append("@Override").append(NEW_LINE);
            sourceBuilder.append(indent(1)).append("public int length() {").append(NEW_LINE);
            {
                sourceBuilder.append(indent(2)).append("return value.length();").append(NEW_LINE);
            }
            sourceBuilder.append(indent(1)).append("}").append(NEW_LINE);

            sourceBuilder.append(NEW_LINE);

            // char charAt(int index);
            sourceBuilder.append(indent(1)).append("@Override").append(NEW_LINE);
            sourceBuilder.append(indent(1)).append("public char charAt(int index) {").append(NEW_LINE);
            {
                sourceBuilder.append(indent(2)).append("return value.charAt(index);").append(NEW_LINE);
            }
            sourceBuilder.append(indent(1)).append("}").append(NEW_LINE);

            sourceBuilder.append(NEW_LINE);

            // CharSequence subSequence(int start, int end);
            sourceBuilder.append(indent(1)).append("@Override").append(NEW_LINE);
            sourceBuilder.append(indent(1)).append("public CharSequence subSequence(int start, int end) {").append(NEW_LINE);
            {
                sourceBuilder.append(indent(2)).append("return value.subSequence(start, end);").append(NEW_LINE);
            }
            sourceBuilder.append(indent(1)).append("}").append(NEW_LINE);

            sourceBuilder.append(NEW_LINE);

            // IntStream chars();
            sourceBuilder.append(indent(1)).append("@Override").append(NEW_LINE);
            sourceBuilder.append(indent(1)).append("public IntStream chars() {").append(NEW_LINE);
            {
                sourceBuilder.append(indent(2)).append("return value.chars();").append(NEW_LINE);
            }
            sourceBuilder.append(indent(1)).append("}").append(NEW_LINE);

            sourceBuilder.append(NEW_LINE);

            // IntStream codePoints();
            sourceBuilder.append(indent(1)).append("@Override").append(NEW_LINE);
            sourceBuilder.append(indent(1)).append("public IntStream codePoints() {").append(NEW_LINE);
            {
                sourceBuilder.append(indent(2)).append("return value.codePoints();").append(NEW_LINE);
            }
            sourceBuilder.append(indent(1)).append("}").append(NEW_LINE);

            sourceBuilder.append(NEW_LINE);

            // String toString();
            sourceBuilder.append(indent(1)).append("@Override").append(NEW_LINE);
            sourceBuilder.append(indent(1)).append("public String toString() {").append(NEW_LINE);
            {
                sourceBuilder.append(indent(2)).append("return value;").append(NEW_LINE);
            }
            sourceBuilder.append(indent(1)).append("}").append(NEW_LINE);

            sourceBuilder.append(NEW_LINE);

            // boolean equals(Object other);
            sourceBuilder.append(indent(1)).append("@Override").append(NEW_LINE);
            sourceBuilder.append(indent(1)).append("public boolean equals(Object other) {").append(NEW_LINE);
            {
                sourceBuilder.append(indent(2)).append("if (this == other) {").append(NEW_LINE);
                {
                    sourceBuilder.append(indent(3)).append("return true;").append(NEW_LINE);
                }
                sourceBuilder.append(indent(2)).append("}").append(NEW_LINE);

                sourceBuilder.append(NEW_LINE);

                sourceBuilder.append(indent(2)).append("if (other == null || getClass() != other.getClass()) {").append(NEW_LINE);
                {
                    sourceBuilder.append(indent(3)).append("return false;").append(NEW_LINE);
                }
                sourceBuilder.append(indent(2)).append("}").append(NEW_LINE);

                sourceBuilder.append(NEW_LINE);

                sourceBuilder.append(indent(2)).append(className).append(" otherInstance = (").append(className).append(") other;").append(NEW_LINE);
                sourceBuilder.append(indent(2)).append("return value.equals(otherInstance.value);").append(NEW_LINE);
            }
            sourceBuilder.append(indent(1)).append("}").append(NEW_LINE);

            sourceBuilder.append(NEW_LINE);

            // int hashCode();
            sourceBuilder.append(indent(1)).append("@Override").append(NEW_LINE);
            sourceBuilder.append(indent(1)).append("public int hashCode() {").append(NEW_LINE);
            {
                sourceBuilder.append(indent(2)).append("return value.hashCode();").append(NEW_LINE);
            }
            sourceBuilder.append(indent(1)).append("}").append(NEW_LINE);
        }
        sourceBuilder.append("}").append(NEW_LINE);

        String javaSource = sourceBuilder.toString();

        String importsSource = importsBuilder.getImportStatements();

        javaSource = javaSource.replace("${importsPlaceholder}", importsSource);

        return new ViewClassSource(PACKAGE_NAME, className, javaSource);
    }

    /*
     * Standard messaging for auto-generated file header.
     */
    private String getSourceCodeHeaderComment() {
        return new ViewClassJavadocsBuilder()
                .addLine("AUTO-GENERATED FILE.  DO NOT MODIFY.")
                .newLine()
                .addLine("This class was automatically generated by the Maven build tool based on")
                .addLine("discovered JSON data files. It should NOT be modified by hand nor checked")
                .addLine("into source control.")
                .buildCommentsSource(0);
    }
}
