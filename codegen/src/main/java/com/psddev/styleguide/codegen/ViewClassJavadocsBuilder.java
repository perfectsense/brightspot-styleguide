package com.psddev.styleguide.codegen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.psddev.dari.util.StringUtils;

import static com.psddev.styleguide.codegen.ViewClassStringUtils.indent;
import static com.psddev.styleguide.codegen.ViewClassStringUtils.NEW_LINE;

/**
 * Utility class for generating Javadocs source.
 */
class ViewClassJavadocsBuilder {

    private StringBuilder javadocsBuilder = new StringBuilder();

    private boolean paragraphStarted = false;

    private StringBuilder paragraphBuilder = new StringBuilder();

    /*
     * Appends some text to the underlying builder. If the caller has initiated
     * the start of a paragraph via {@link #startParagraph} the text will be
     * appended to the paragraph buffer instead of the main javadocs builder,
     * until {@link #endParagraph} has been called.
     */
    private void appendToBuilder(String text) {
        if (paragraphStarted) {
            paragraphBuilder.append(text);
        } else {
            javadocsBuilder.append(text);
        }
    }

    /**
     * Adds plain text to the builder.
     *
     * @param text the text to ad.
     * @return this builder.
     */
    public ViewClassJavadocsBuilder add(String text) {
        appendToBuilder(text);
        return this;
    }

    /**
     * Adds plain text to the builder with a trailing new line.
     *
     * @param line the line of text to add.
     * @return this builder.
     */
    public ViewClassJavadocsBuilder addLine(String line) {
        add(line).add(NEW_LINE);
        return this;
    }

    /**
     * Adds a new line to the builder.
     *
     * @return this builder.
     */
    public ViewClassJavadocsBuilder newLine() {
        addLine("");
        return this;
    }

    /**
     * Adds a paragraph to the builder. The contents of the {@code paragraph}
     * argument will go inside of &lt;p&gt; tags.
     *
     * @param paragraph the paragraph text to add.
     * @return this builder.
     */
    public ViewClassJavadocsBuilder addParagraph(String paragraph) {
        addLine("<p>" + paragraph + "</p>");
        return this;
    }

    /**
     * Starts a new paragraph. All subsequent calls to this builder that add
     * text will go inside of the same paragraph until {@link #endParagraph()}
     * is called.
     *
     * @return this builder.
     */
    public ViewClassJavadocsBuilder startParagraph() {
        paragraphStarted = true;
        return this;
    }

    /**
     * Ends a previously {@link #startParagraph() started paragraph}.
     *
     * @return this builder.
     */
    public ViewClassJavadocsBuilder endParagraph() {
        paragraphStarted = false;
        if (paragraphBuilder.length() > 0) {
            addParagraph(paragraphBuilder.toString());
            paragraphBuilder.setLength(0);
        }
        return this;
    }

    /**
     * Adds a {@code parameter} declaration to the javadocs.
     *
     * @param parameterName the name of the parameter.
     * @return this builder.
     */
    public ViewClassJavadocsBuilder addParameter(String parameterName) {
        add("@param " + parameterName + " ");
        return this;
    }

    /**
     * Adds a {@code return} declaration to the javadocs.
     *
     * @return this builder.
     */
    public ViewClassJavadocsBuilder addReturn() {
        add("@return ");
        return this;
    }

    /**
     * Adds a Javadocs link.
     *
     * @param link the link URI.
     * @return this builder.
     */
    public ViewClassJavadocsBuilder addLink(String link) {
        addLink(link, null);
        return this;
    }

    /**
     * Adds a Javadocs link with optional label text.
     *
     * @param link the link URI
     * @param label the text that should be linked.
     * @return this builder.
     */
    public ViewClassJavadocsBuilder addLink(String link, String label) {
        add("{@link ");
        add(link);
        if (label != null) {
            add(" ");
            add(label);
        }
        add("}");
        return this;
    }

    /**
     * Adds a list of valid field types for a given view class field definition
     * with links to the javadocs of each of those types written as if a single
     * value is to be returned or passed as an argument.
     *
     * @param fieldDef the field definition in reference.
     * @return this builder.
     */
    public ViewClassJavadocsBuilder addFieldValueTypesSnippet(ViewClassFieldDefinition fieldDef) {
        fieldValueTypesSnippetHelper(fieldDef, false);
        return this;
    }

    /**
     * Adds a list of valid field types for a given view class field definition
     * with links to the javadocs of each of those types written as if a
     * collection of these value is to be returned or passed as an argument.
     *
     * @param fieldDef the field definition in reference.
     * @return this builder.
     */
    public ViewClassJavadocsBuilder addCollectionFieldValueTypesSnippet(ViewClassFieldDefinition fieldDef) {
        fieldValueTypesSnippetHelper(fieldDef, true);
        return this;
    }

    /**
     * Adds a list of valid field types for a given view class field definition
     * with links to the javadocs of each of those types written in context of
     * the type of field definition that is passed in. i.e. if it's a collection
     * or not.
     *
     * @param fieldDef the field definition in reference.
     * @return this builder.
     */
    public ViewClassJavadocsBuilder addFieldAwareValueTypesSnippet(ViewClassFieldDefinition fieldDef) {
        fieldValueTypesSnippetHelper(fieldDef, JsonList.class == fieldDef.getEffectiveType());
        return this;
    }

    /*
     * Helper method to produce the value types snippet of Javadocs for a
     * particular view class field definition.
     */
    private void fieldValueTypesSnippetHelper(ViewClassFieldDefinition fieldDef, boolean isCollection) {

        Set<ViewClassFieldType> fieldValueTypes = fieldDef.getFieldValueTypes();
        ViewClassDefinition parentTemplateDef = fieldDef.getClassDefinition();

        List<String> javadocLinks = new ArrayList<>();

        fieldValueTypes.stream().sorted(Comparator.comparing(ViewClassFieldType::getClassName)).forEach(fieldType -> {

            // TODO: Add logic to detect whether it really needs to be fully qualified or not
            StringBuilder javadocLinkBuilder = new StringBuilder();

            javadocLinkBuilder.append("{@link ");

            if (fieldType instanceof ViewClassFieldNativeJavaType) {
                javadocLinkBuilder.append(fieldType.getFullyQualifiedClassName());

            } else {
                if (fieldType.hasSamePackageAs(parentTemplateDef)) {
                    javadocLinkBuilder.append(fieldType.getClassName());

                } else {
                    javadocLinkBuilder.append(fieldType.getFullyQualifiedClassName());
                    javadocLinkBuilder.append(" ");
                    javadocLinkBuilder.append(fieldType.getClassName());
                }
            }

            javadocLinkBuilder.append("}");

            javadocLinks.add(javadocLinkBuilder.toString());
        });

        if (fieldDef.hasMixedValueTypes()) {
            javadocLinks.add("{@link "
                    + CharSequenceClassSourceGenerator.PACKAGE_NAME
                    + "."
                    + CharSequenceClassSourceGenerator.PLAIN_TEXT_CLASS_NAME
                    + " "
                    + CharSequenceClassSourceGenerator.PLAIN_TEXT_CLASS_NAME);
        }

        StringBuilder builder = new StringBuilder();

        if (fieldDef.getClassDefinition().getContext().isGenerateStrictTypes()) {
            if (fieldDef.isAbstract()) {
                builder.append("An <b>abstract</b> ");
            } else {
                builder.append("An ");
            }
        } else {
            builder.append("Typically a ");
        }

        if (isCollection) {
            builder.append("Iterable of ");
        }

        String operator = isCollection ? "and" : "or";

        builder.append(javadocLinks
                .stream()
                // join them with commas
                .collect(Collectors.joining("," + NEW_LINE))
                // isolate the last comma in the String and replace it with the operator " and" or " or"
                .replaceFirst("(.*),([^,]+$)", "$1 " + operator + "$2"));

        builder.append(".");

        add(builder.toString());
    }

    /**
     * Adds a snippet of javadocs that lists all of the JSON files that the
     * given class definition is referenced in.
     *
     * @param classDef the view class definition in question.
     * @return this builder.
     */
    public ViewClassJavadocsBuilder addClassOccurrencesList(ViewClassDefinition classDef) {

        List<JsonDataLocation> locations = classDef.getJsonViewMaps().stream()
                .map(JsonViewMap::getLocation)
                .collect(Collectors.toList());

        if (!locations.isEmpty()) {
            StringBuilder builder = new StringBuilder();

            builder.append("This View is referenced in the following files:");
            builder.append(NEW_LINE);
            builder.append(getOccurrencesListHtml(locations));

            addParagraph(builder.toString());
        }

        return this;
    }

    /**
     * Adds a snippet of javadocs that lists all of the JSON files that the
     * given field definition is referenced in.
     *
     * @param fieldDef the view class field definition in question.
     * @return this builder.
     */
    public ViewClassJavadocsBuilder addFieldOccurrencesList(ViewClassFieldDefinition fieldDef) {

        List<JsonDataLocation> locations = fieldDef.getFieldKeyValues().stream()
                .map(entry -> entry.getKey().getLocation())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!locations.isEmpty()) {
            StringBuilder builder = new StringBuilder();

            builder.append("This field is referenced in the following files:");
            builder.append(NEW_LINE);
            builder.append(getOccurrencesListHtml(locations));

            addParagraph(builder.toString());
        }

        return this;
    }

    /*
     * Converts a list of JSON data location objects into javadocs HTML.
     */
    private String getOccurrencesListHtml(List<JsonDataLocation> locations) {

        Map<String, Set<JsonDataLocation>> locationsByPath = new TreeMap<>();

        // sort, then de-dupe preserving sort order
        for (JsonDataLocation location : locations.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new))) {

            String locationPath = location.getFile().getRelativePath().toString();

            Set<JsonDataLocation> locationsForPath = locationsByPath.get(locationPath);

            if (locationsForPath == null) {
                locationsForPath = new TreeSet<>();
                locationsByPath.put(locationPath, locationsForPath);
            }

            locationsForPath.add(location);
        }

        StringBuilder builder = new StringBuilder();

        builder.append("<ol>");
        builder.append(NEW_LINE);

        for (Map.Entry<String, Set<JsonDataLocation>> entry : locationsByPath.entrySet()) {

            String path = entry.getKey();
            Set<JsonDataLocation> locationsAtPath = entry.getValue();

            builder.append("<li>");
            builder.append(path);
            builder.append(" at:");
            builder.append(NEW_LINE);

            builder.append("<ol>");
            builder.append(NEW_LINE);

            for (JsonDataLocation location : locationsAtPath) {
                builder.append("    <li>");
                builder.append(String.format("(line=%s, col=%s, offset=%s)",
                        location.getLineNumber(),
                        location.getColumnNumber(),
                        location.getStreamOffset()));
                builder.append("</li>");
                builder.append(NEW_LINE);
            }

            builder.append("</ol>");
            builder.append(NEW_LINE);

            builder.append("</li>");
            builder.append(NEW_LINE);
        }

        builder.append("</ol>");
        builder.append(NEW_LINE);
        return builder.toString();
    }

    /**
     * Finds example String values or Map values for a given field definition
     * and appends them in an HTML list to serve as an example for what the
     * view model implementation should return.
     *
     * @param fieldDef the field definition to analyze.
     * @param numberOfSamples the maximum number of unique String values to
     *                        extract from the field definition.
     * @return this builder.
     */
    public ViewClassJavadocsBuilder addSampleValuesList(ViewClassFieldDefinition fieldDef, int numberOfSamples) {

        if (numberOfSamples > 0) {

            Set<String> sampleValues = new LinkedHashSet<>();

            List<JsonValue> values = null;

            Class<? extends JsonValue> effectiveType = fieldDef.getEffectiveType();

            if (effectiveType == JsonList.class
                    && ViewClassFieldNativeJavaType.CHAR_SEQUENCE.equals(fieldDef.getEffectiveValueType())) {

                values = fieldDef.getFieldKeyValues().stream()
                        .map(Map.Entry::getValue)
                        .filter(value -> value instanceof JsonList)
                        .map(value -> (JsonList) value)
                        .map(JsonList::getValues)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

            } else if (effectiveType == JsonString.class) {

                values = fieldDef.getFieldKeyValues().stream()
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList());

            } else if (effectiveType == JsonMap.class) {

                values = fieldDef.getFieldKeyValues().stream()
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList());
            }

            if (values != null) {
                for (JsonValue value : values) {

                    if (value instanceof JsonString) {
                        sampleValues.add(((JsonString) value).toRawValue());

                        // make sure it's not a sub-class of JsonMap
                    } else if (value instanceof JsonMap && value.getClass() == JsonMap.class) {

                        for (Map.Entry<String, Object> entry : ((JsonMap) value).toRawValue().entrySet()) {

                            sampleValues.add(entry.getKey() + " = " + entry.getValue());

                            if (sampleValues.size() == numberOfSamples) {
                                break;
                            }
                        }
                    }

                    if (sampleValues.size() == numberOfSamples) {
                        break;
                    }
                }
            }

            if (!sampleValues.isEmpty()) {

                StringBuilder builder = new StringBuilder();

                builder.append("Example values for this field include:");
                builder.append(NEW_LINE);
                builder.append("<ul>");
                builder.append(NEW_LINE);

                for (String sampleValue : sampleValues) {
                    builder.append("<li>");
                    builder.append(StringUtils.escapeHtml(sampleValue).replace("*/", "&#x2A;&#x2F;"));
                    builder.append("</li>");
                    builder.append(NEW_LINE);
                }

                builder.append("</ul>");
                builder.append(NEW_LINE);

                addParagraph(builder.toString());
            }
        }

        return this;
    }

    /**
     * Builds the Javadocs into source code using the standard Javadocs style
     * commenting indented with the specified {@code indent}.
     *
     * @param indent the number of 4-spaced indents that should be prefixed to
     *               each line.
     * @return the javadocs source code.
     */
    public String buildJavadocsSource(int indent) {
        return buildSourceHelper(indent, true);
    }

    /**
     * Builds the Javadocs into source code using the standard multi-line style
     * commenting indented with the specified {@code indent}.
     *
     * @param indent the number of 4-spaced indents that should be prefixed to
     *               each line.
     * @return the multi-line comment source code.
     */
    public String buildCommentsSource(int indent) {
        return buildSourceHelper(indent, true);
    }

    /*
     * Helper method to build the final javadocs source code.
     */
    private String buildSourceHelper(int indent, boolean isJavadoc) {
        StringBuilder builder = new StringBuilder();

        String[] lines = javadocsBuilder.toString().split(NEW_LINE);

        if (lines.length > 0) {

            builder.append(indent(indent));
            builder.append("/*");
            if (isJavadoc) {
                builder.append("*");
            }
            builder.append(NEW_LINE);

            for (String line : lines) {

                builder.append(indent(indent)).append(" *");

                if (line.length() > 0) {
                    builder.append(" ");
                }

                builder.append(line);
                builder.append(NEW_LINE);
            }

            builder.append(indent(indent)).append(" */").append(NEW_LINE);
        }

        return builder.toString();
    }
}
