package com.psddev.styleguide.viewgenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains all of the metadata necessary to generate a Java class source file
 * representing a View interface along with a static inner Builder class. Its
 * metadata can and should be validated first to ensure that the generated
 * class will compile.
 */
class ViewClassDefinition implements ViewClassFieldType {

    private ViewClassGeneratorContext context;

    private ViewKey viewKey;

    private Set<JsonViewMap> jsonViewMaps;

    private Map<String, ViewClassFieldDefinition> fieldDefsByName;

    private List<ViewClassDefinitionError> errors;

    private boolean validated = false;

    /**
     * Creates a new view class definition identified by the given
     * {@code viewKey} and defined by the set of {@code jsonViewMaps} governed
     * by the given view class generator {@code context}.
     *
     * @param context The context/settings for the overall view class generator operation.
     * @param viewKey The key that uniquely identifies this view class definition.
     * @param jsonViewMaps Set of all the JSON based definitions and usages of this view
     *                     found in the styleguide that when combined create a unified
     *                     definition of all the fields and types for this view.
     */
    public ViewClassDefinition(ViewClassGeneratorContext context,
                               ViewKey viewKey,
                               Set<JsonViewMap> jsonViewMaps) {

        this.context = context;
        this.viewKey = viewKey;
        this.jsonViewMaps = jsonViewMaps;
    }

    /**
     * Gets the context/settings for the overall view class generator operation.
     *
     * @return the view class generation context.
     */
    public ViewClassGeneratorContext getContext() {
        return context;
    }

    /**
     * The key that uniquely identifies this view class definition.
     *
     * @return the view key for this view definition.
     */
    public ViewKey getViewKey() {
        return viewKey;
    }

    /**
     * Gets the Set of all the JSON based definitions and usages of this view
     * found in the styleguide that when combined create a unified definition
     * of all the fields and types for this view.
     *
     * @return the set of JSON view maps for this view definition.
     */
    public Set<JsonViewMap> getJsonViewMaps() {
        return jsonViewMaps;
    }

    /**
     * Gets all the documentation notes about the view class represented by
     * this view class definition.
     *
     * @return the set of documentation notes.
     */
    public Set<String> getNotes() {
        return jsonViewMaps.stream()
                .map(JsonViewMap::getNotes)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public String getFullyQualifiedClassName() {
        return viewKey.getFullyQualifiedClassName();
    }

    /**
     * Validates the metadata contained within this view class definition. If
     * any of the metadata present in this definition would result in an invalid
     * Java class being generated further down stream then an error is added to
     * this definition's errors list which can be checked prior to view class
     * source code generation.
     */
    public void validate() {

        if (validated) {
            return;
        }

        viewKey.validate();

        if (viewKey.hasAnyErrors()) {
            errors.addAll(viewKey.getErrors());
        }

        for (ViewClassFieldDefinition fieldDef : getFieldDefinitions()) {

            fieldDef.validate();

            if (fieldDef.hasAnyErrors()) {
                errors.addAll(fieldDef.getErrors());
            }
        }

        validated = true;
    }

    /**
     * Checks if there are any errors with this view class definition such that
     * generating a Java class from it would cause it to not compile or
     * otherwise cause some downstream error.
     *
     * @return true if there are nay errors with this view class definition.
     */
    public boolean hasAnyErrors() {
        validate();
        return !getErrors().isEmpty();
    }

    /**
     * Gets the list of errors with this view class definition.
     *
     * @return the list errors, or and empty list if there are no errors.
     */
    public List<ViewClassDefinitionError> getErrors() {
        validate();
        if (errors == null) {
            errors = new ArrayList<>();
        }
        return new ArrayList<>(errors);
    }

    /**
     * Gets the full list view class field definitions including those that
     * have an effective value of null.
     *
     * @return the list of field definitions.
     */
    public List<ViewClassFieldDefinition> getFieldDefinitions() {

        if (fieldDefsByName == null) {

            Map<String, Set<Map.Entry<JsonKey, JsonValue>>> fieldValuesMap = new HashMap<>();

            for (JsonViewMap jsonViewMap : jsonViewMaps) {

                for (Map.Entry<JsonKey, JsonValue> entry : jsonViewMap.getValues().entrySet()) {

                    String fieldName = entry.getKey().getName();

                    Set<Map.Entry<JsonKey, JsonValue>> fieldValues = fieldValuesMap.get(fieldName);
                    if (fieldValues == null) {
                        fieldValues = new HashSet<>();
                        fieldValuesMap.put(fieldName, fieldValues);
                    }

                    fieldValues.add(entry);
                }
            }

            fieldDefsByName = fieldValuesMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> new ViewClassFieldDefinition(this, entry.getKey(), entry.getValue())));
        }

        return new ArrayList<>(fieldDefsByName.values());
    }

    /**
     * Gets only the field definitions that are not effectively null.
     *
     * @return the non-null field definitions only.
     */
    public List<ViewClassFieldDefinition> getNonNullFieldDefinitions() {
        return getFieldDefinitions().stream()
                .filter(fieldDef -> fieldDef.getEffectiveType() != null)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
