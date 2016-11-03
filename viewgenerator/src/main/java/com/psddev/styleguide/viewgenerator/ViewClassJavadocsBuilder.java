package com.psddev.styleguide.viewgenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.StringUtils;

import static com.psddev.styleguide.viewgenerator.ViewClassStringUtils.indent;
import static com.psddev.styleguide.viewgenerator.ViewClassStringUtils.NEW_LINE;

/**
 * Utility class for generating Javadocs source.
 */
class ViewClassJavadocsBuilder {

    private StringBuilder javadocsBuilder = new StringBuilder();

    private boolean paragraphStarted = false;
    private StringBuilder paragraphBuilder = new StringBuilder();

    private void appendToBuilder(String text) {
        if (paragraphStarted) {
            paragraphBuilder.append(text);
        } else {
            javadocsBuilder.append(text);
        }
    }

    public ViewClassJavadocsBuilder add(String text) {
        appendToBuilder(text);
        return this;
    }

    public ViewClassJavadocsBuilder addLine(String line) {
        add(line).add(NEW_LINE);
        return this;
    }

    public ViewClassJavadocsBuilder newLine() {
        addLine("");
        return this;
    }

    public ViewClassJavadocsBuilder addParagraph(String paragraph) {
        addLine("<p>" + paragraph + "</p>");
        return this;
    }

    public ViewClassJavadocsBuilder startParagraph() {
        paragraphStarted = true;
        return this;
    }

    public ViewClassJavadocsBuilder endParagraph() {
        paragraphStarted = false;
        if (paragraphBuilder.length() > 0) {
            addParagraph(paragraphBuilder.toString());
            paragraphBuilder.setLength(0);
        }
        return this;
    }

    public ViewClassJavadocsBuilder addParameter(String parameterName) {
        add("@param " + parameterName + " ");
        return this;
    }

    public ViewClassJavadocsBuilder addReturn() {
        add("@return ");
        return this;
    }

    public ViewClassJavadocsBuilder addLink(String link) {
        addLink(link, null);
        return this;
    }

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

    public ViewClassJavadocsBuilder addFieldValueTypesSnippet(ViewClassFieldDefinition fieldDef) {
        fieldValueTypesSnippetHelper(fieldDef, false);
        return this;
    }

    public ViewClassJavadocsBuilder addCollectionFieldValueTypesSnippet(ViewClassFieldDefinition fieldDef) {
        fieldValueTypesSnippetHelper(fieldDef, true);
        return this;
    }

    public ViewClassJavadocsBuilder addFieldAwareValueTypesSnippet(ViewClassFieldDefinition fieldDef) {
        fieldValueTypesSnippetHelper(fieldDef, JsonList.class == fieldDef.getEffectiveType());
        return this;
    }

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

        StringBuilder builder = new StringBuilder();

        if (fieldDef.getClassDefinition().getContext().isGenerateStrictTypes()) {
            builder.append("A ");
        } else {
            builder.append("Typically a ");
        }

        if (isCollection) {
            builder.append("Collection of ");
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

    private String getOccurrencesListHtml(List<JsonDataLocation> locations) {

        StringBuilder builder = new StringBuilder();

        builder.append("<ul>");
        builder.append(NEW_LINE);

        // sort, then de-dupe preserving sort order
        for (JsonDataLocation location : locations.stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new))) {
            builder.append("<li>");
            builder.append(location);
            builder.append("</li>");
            builder.append(NEW_LINE);
        }

        builder.append("</ul>");
        builder.append(NEW_LINE);
        return builder.toString();
    }

    public ViewClassJavadocsBuilder addSampleStringValuesList(ViewClassFieldDefinition fieldDef, int numberOfSamples) {

        if (numberOfSamples > 0) {

            Set<String> sampleValues = new LinkedHashSet<>();

            List<JsonValue> values = null;

            Class<? extends JsonValue> effectiveType = fieldDef.getEffectiveType();

            if (effectiveType == JsonList.class
                    && ViewClassFieldNativeJavaType.STRING.equals(fieldDef.getEffectiveValueType())) {

                values = fieldDef.getFieldKeyValues().stream()
                        .map(Map.Entry::getValue)
                        .filter(value -> value instanceof JsonList)
                        .map(value -> (JsonList) value)
                        .map(JsonList::getValues)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

            } else  if (effectiveType == JsonString.class) {

                values = fieldDef.getFieldKeyValues().stream()
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList());
            }

            if (values != null) {
                for (JsonValue value : values) {

                    if (value instanceof JsonString) {
                        sampleValues.add(((JsonString) value).toRawValue());
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
                    builder.append(StringUtils.escapeHtml(sampleValue));
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

    public String buildJavadocsSource(int indent) {
        return buildSourceHelper(indent, true);
    }

    public String buildCommentsSource(int indent) {
        return buildSourceHelper(indent, true);
    }

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
                line = line.trim();

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
