package com.psddev.styleguide.viewgenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class ViewClassDefinition implements ViewClassFieldType {

    private ViewClassGeneratorContext context;

    private ViewKey viewKey;

    private Set<JsonViewMap> jsonViewMaps;

    private Map<String, ViewClassFieldDefinition> fieldDefsByName;

    private List<ViewClassDefinitionError> errors = new ArrayList<>();

    public ViewClassDefinition(ViewClassGeneratorContext context,
                                ViewKey viewKey,
                                Set<JsonViewMap> jsonViewMaps) {

        this.context = context;
        this.viewKey = viewKey;
        this.jsonViewMaps = jsonViewMaps;
    }

    public ViewClassGeneratorContext getContext() {
        return context;
    }

    public ViewKey getViewKey() {
        return viewKey;
    }

    public Set<JsonViewMap> getJsonViewMaps() {
        return jsonViewMaps;
    }

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

    public void validate() {

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
    }

    public boolean hasAnyErrors() {
        return !errors.isEmpty();
    }

    public List<ViewClassDefinitionError> getErrors() {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        return errors;
    }

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
     * @return the non-null field definitions only
     */
    public List<ViewClassFieldDefinition> getNonNullFieldDefinitions() {
        return getFieldDefinitions().stream()
                .filter(fieldDef -> fieldDef.getEffectiveType() != null)
                .collect(Collectors.toList());
    }
}
