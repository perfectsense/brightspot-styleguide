package com.psddev.styleguide.codegen;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Contains all of the metadata related to a single field of a
 * {@link ViewClassDefinition}.
 */
class ViewClassFieldDefinition implements ViewClassFieldType {

    private ViewClassDefinition viewClassDef;

    private String fieldName;

    private Set<Map.Entry<JsonKey, JsonValue>> fieldKeyValues;

    // The type of the field
    private Class<? extends JsonValue> effectiveType;

    private Boolean isDelegate;
    private Boolean isAbstract;

    private boolean validated = false;
    private List<ViewClassDefinitionError> errors = new ArrayList<>();

    /**
     * Creates a new view class field definition.
     *
     * @param viewClassDef the parent class definition.
     * @param fieldName the name of the field.
     * @param fieldKeyValues the key/value JSON pairs for all instance of this
     *                       field in the JSON directory.
     */
    public ViewClassFieldDefinition(ViewClassDefinition viewClassDef,
                                    String fieldName,
                                    Set<Map.Entry<JsonKey, JsonValue>> fieldKeyValues) {

        this.viewClassDef = viewClassDef;
        this.fieldName = fieldName;
        this.fieldKeyValues = fieldKeyValues;
    }

    /**
     * Gets the parent class definition.
     *
     * @return the parent class definition.
     */
    public ViewClassDefinition getClassDefinition() {
        return viewClassDef;
    }

    /**
     * Gets the name of the field.
     *
     * @return the field name.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Gets all the key/value JSON pairs for all instance of this field in the
     * JSON directory.
     *
     * @return the JSON key/value pairs for this field.
     */
    public Set<Map.Entry<JsonKey, JsonValue>> getFieldKeyValues() {
        return fieldKeyValues;
    }

    /**
     * Gets the documentation notes for this field.
     *
     * @return the set of notes for this field.
     */
    public Set<String> getNotes() {
        return fieldKeyValues.stream()
                .map(entry -> entry.getKey().getNotes())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Performs validation on this field, and adds any errors to a list.
     */
    public void validate() {

        if (validated) {
            return;
        }

        // validate the field name
        validateFieldName();

        // validate the value types
        getEffectiveType(fieldKeyValues.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toCollection(ArrayList::new)), true);

        // Gets the field value types with the validate flag set to true.
        getFieldValueTypes(fieldKeyValues.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toCollection(ArrayList::new)), true);

        validated = true;
    }

    private void validateFieldName() {

        // Make sure the field name is valid java identifier...
        if (!ViewClassStringUtils.isValidJavaIdentifier(fieldName)) {

            addError("Field name [" + fieldName + "] is not a valid Java identifier.");

        // ... and not a Java reserved keyword...
        } else if (ViewClassStringUtils.isJavaKeyword(fieldName)) {

            addError("Field name [" + fieldName + "] cannot be a Java keyword.");

        // ...and that it can be converted to a bean spec compatible getter method name.
        } else if (Character.isUpperCase(fieldName.charAt(0))
                && fieldName.length() > 1
                && (Character.isLowerCase(fieldName.charAt(1)) || !Character.isLetter(fieldName.charAt(1)))) {

            addError("Field name [" + fieldName + "] cannot start with an uppercase letter and be followed by a non-uppercase letter.");
        }
    }

    /*
     * Validates that the values have a consistent type, and returns it. If
     * there is more than one, an error is added to this field definition and
     * null is returned.
     */
    private Class<? extends JsonValue> getEffectiveType(Collection<JsonValue> values, boolean validate) {

        // The only time values will be empty is if we are in a recursive call on a List.
        if (values.isEmpty()) {
            addErrorConditionally("List cannot be empty, they must have at least one value.", validate);
        }

        Set<Class<? extends JsonValue>> valueTypes = values.stream()
                .map(JsonValue::getClass)
                .collect(Collectors.toCollection(HashSet::new));

        // ignore nulls
        valueTypes.remove(JsonNull.class);

        // If we find a delegate or abstract map type, replace it with a view map type since they are placeholders for them.
        if (valueTypes.remove(JsonDelegateMap.class)) {
            valueTypes.add(JsonViewMap.class);
        }

        if (valueTypes.remove(JsonAbstractMap.class)) {
            valueTypes.add(JsonViewMap.class);
        }

        Class<? extends JsonValue> effectiveValueType;

        if (valueTypes.size() == 1
                || (!viewClassDef.getContext().isGenerateStrictTypes() && valueTypes.size() == 2
                && valueTypes.containsAll(Arrays.asList(JsonViewMap.class, JsonString.class)))) {

            if (valueTypes.size() == 2) {
                // If not strictly typed, we allow Strings and Objects to co-exist and just treat them as if it is an Object.
                effectiveValueType = JsonViewMap.class;
            } else {
                effectiveValueType = valueTypes.iterator().next();
            }

            // Recurse on list values, since they have to all be the same as well.
            // And we don't have to worry about List of Lists since that is caught during the JSON parse phase.
            if (effectiveValueType == JsonList.class) {
                if (validate) {
                    getEffectiveType(values.stream()
                            // ignore nulls
                            .filter(value -> !(value instanceof JsonNull))
                            // the rest should be Lists
                            .map(value -> (JsonList) value)
                            // get the values in list
                            .map(JsonList::getValues)
                            // flatten it out
                            .flatMap(Collection::stream)
                            // add them to a set
                            .collect(Collectors.toList()), true);
                }
            }

            return effectiveValueType;

        } else if (valueTypes.size() > 1) {
            addErrorConditionally("A field can only have a single value type but has "
                    + valueTypes.stream()
                            .map(JsonValueType::forClass)
                            .map(JsonValueType::getLabel)
                            .collect(Collectors.joining(" and "))
                    + " instead!", validate);
        }

        return null;
    }

    /**
     * Gets all the types of values that can be returned for this field.
     *
     * @return the set of value types for this field.
     */
    public Set<ViewClassFieldType> getFieldValueTypes() {
        return getFieldValueTypes(fieldKeyValues.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toCollection(ArrayList::new)), false);
    }

    private Set<ViewClassFieldType> getFieldValueTypes(Collection<JsonValue> values, boolean validate) {

        Set<Class<? extends JsonValue>> valueTypes = values.stream()
                .map(JsonValue::getClass)
                .collect(Collectors.toCollection(HashSet::new));

        // ignore nulls
        valueTypes.remove(JsonNull.class);

        // If we find a delegate map type, replace it with a view map type since they are placeholders for them.
        if (valueTypes.remove(JsonDelegateMap.class)) {
            valueTypes.add(JsonViewMap.class);
        }

        if (valueTypes.remove(JsonAbstractMap.class)) {
            valueTypes.add(JsonViewMap.class);
        }

        Class<? extends JsonValue> effectiveValueType;

        isDelegate = false;
        isAbstract = false;

        if (valueTypes.size() == 1
                || (!viewClassDef.getContext().isGenerateStrictTypes() && valueTypes.size() == 2
                && valueTypes.containsAll(Arrays.asList(JsonViewMap.class, JsonString.class)))) {

            if (valueTypes.size() == 2) {
                // If not strictly typed, we allow Strings and Objects to co-exist and just treat them as if it is an Object.
                effectiveValueType = JsonViewMap.class;
            } else {
                effectiveValueType = valueTypes.iterator().next();
            }

            // Recurse on list values, since they have to all be the same as well.
            // And we don't have to worry about List of Lists since that is caught during the JSON parse phase.
            if (effectiveValueType == JsonList.class) {
                return getFieldValueTypes(values.stream()
                        // ignore nulls
                        .filter(value -> !(value instanceof JsonNull))
                        // the rest should be Lists
                        .map(value -> (JsonList) value)
                        // get the values in list
                        .map(JsonList::getValues)
                        // flatten it out
                        .flatMap(Collection::stream)
                        // add them to a set
                        .collect(Collectors.toList()), validate);

            } else if (effectiveValueType == JsonViewMap.class) {

                Set<ViewClassFieldType> fieldValueTypes = values.stream()
                        .filter(value -> (value instanceof JsonViewMap))
                        .map(value -> (JsonViewMap) value)
                        .map(JsonViewMap::getViewKey)
                        .collect(Collectors.toCollection(HashSet::new));

                Set<JsonDelegateMap> delegateMaps = values.stream()
                        .filter(value -> (value instanceof JsonDelegateMap))
                        .map(value -> (JsonDelegateMap) value)
                        .collect(Collectors.toSet());

                Set<JsonAbstractMap> abstractMaps = values.stream()
                        .filter(value -> (value instanceof JsonAbstractMap))
                        .map(value -> (JsonAbstractMap) value)
                        .collect(Collectors.toSet());

                /*
                 * for each delegate key search for all view class definitions
                 * that have a JSON view map whose wrapper JSON file matches
                 * the file that the delegate map is declared in.
                 */
                Set<Path> delegateFilePaths = delegateMaps.stream()
                        .map(JsonDelegateMap::getDeclaringJsonFile)
                        .map(JsonFile::getRelativePath)
                        .collect(Collectors.toCollection(TreeSet::new));

                if (!delegateFilePaths.isEmpty()) {

                    for (ViewClassDefinition classDef : getContext().getClassDefinitions()) {

                        for (JsonViewMap jsonViewMap : classDef.getJsonViewMaps()) {

                            JsonFile wrapper = jsonViewMap.getWrapper();
                            if (wrapper != null) {
                                if (delegateFilePaths.contains(wrapper.getRelativePath())) {
                                    fieldValueTypes.add(classDef);
                                }
                            }
                        }
                    }
                }

                if (!abstractMaps.isEmpty() && !delegateMaps.isEmpty()) {
                    addErrorConditionally("A field cannot be declared as both delegate and abstract.", validate);
                }

                if (!delegateMaps.isEmpty()) {
                    isDelegate = true;

                    /*
                     * This means that this field definition is a delegate, but
                     * no corresponding class definitions that declared it as
                     * its wrapper.
                     */
                    if (fieldValueTypes.isEmpty()) {
                        addErrorConditionally("Can't infer the type of this delegate field"
                                + " because there are no views that implicitly nor"
                                + " explicitly declared the field's file(s) as a"
                                + " wrapper. Unreferenced wrapper files: "
                                + delegateFilePaths, validate);
                    }
                }

                if (!abstractMaps.isEmpty() && fieldValueTypes.isEmpty()) {
                    isAbstract = true;

                    return Collections.singleton(this);
                }

                return fieldValueTypes;

            } else if (effectiveValueType == JsonBoolean.class) {
                return Collections.singleton(ViewClassFieldNativeJavaType.BOOLEAN);

            } else if (effectiveValueType == JsonNumber.class) {
                return Collections.singleton(ViewClassFieldNativeJavaType.NUMBER);

            } else if (effectiveValueType == JsonString.class) {
                return Collections.singleton(ViewClassFieldNativeJavaType.STRING);

            } else if (effectiveValueType == JsonMap.class) {
                return Collections.singleton(ViewClassFieldNativeJavaType.MAP);
            }
        }

        return Collections.emptySet();
    }

    /**
     * Gets the effective value type for this field. If the field can be
     * multiple different value types the effective value type is the field
     * itself which will be converted to the corresponding field level
     * interface. The value type of a list field is the items that go in the
     * list and the list type itself.
     *
     * @return the effective value type for of this field.
     */
    public ViewClassFieldType getEffectiveValueType() {

        boolean isStrictlyTyped = getClassDefinition().getContext().isGenerateStrictTypes();

        Set<ViewClassFieldType> fieldValueTypes = getFieldValueTypes();

        if (fieldValueTypes.size() == 1) {

            if (isStrictlyTyped) {

                ViewClassFieldType fieldValueType = fieldValueTypes.iterator().next();
                if (fieldValueType instanceof ViewClassFieldNativeJavaType) {
                    return fieldValueType;
                } else {
                    return this;
                }

            } else {
                return ViewClassFieldNativeJavaType.OBJECT;
            }

            //return isStrictlyTyped ? fieldValueTypes.iterator().next() : ViewClassFieldNativeJavaType.OBJECT;

        } else if (fieldValueTypes.size() > 1) {

            if (isStrictlyTyped) {
                return this;

            } else {
                return ViewClassFieldNativeJavaType.OBJECT;
            }

        } else {
            return null;
        }
    }

    /**
     * Gets the effective type of this field. A {@link JsonBoolean},
     * {@link JsonNumber}, {@link JsonString}, {@link JsonMap},
     * {@link JsonList}, or {@link JsonViewMap}.
     *
     * @return the effective type of this field.
     */
    public Class<? extends JsonValue> getEffectiveType() {
        if (effectiveType == null) {
            effectiveType = getEffectiveType(fieldKeyValues.stream()
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toCollection(ArrayList::new)), false);
        }

        return effectiveType;
    }

    public boolean isDelegate() {
        if (isDelegate == null) {
            // initializes the isDelegate flag if it hasn't been set.
            getFieldValueTypes();
        }
        return Boolean.TRUE.equals(isDelegate);
    }

    public boolean isAbstract() {
        if (isAbstract == null) {
            // initializes the isAbstract flag if it hasn't been set.
            getFieldValueTypes();
        }
        return Boolean.TRUE.equals(isAbstract);
    }

    @Override
    public String getFullyQualifiedClassName() {
        return getClassDefinition().getFullyQualifiedClassName() + ViewClassStringUtils.toJavaClassCase(fieldName) + "Field";
    }

    private void addError(String message) {
        errors.add(new ViewClassDefinitionError(this, message));
    }

    private void addErrorConditionally(String message, boolean addError) {
        if (addError) {
            addError(message);
        }
    }

    private ViewClassGeneratorContext getContext() {
        return getClassDefinition().getContext();
    }

    /**
     * Gets the list of errors with this field.
     *
     * @return the list of errors.
     */
    public List<ViewClassDefinitionError> getErrors() {
        return errors;
    }

    /**
     * Returns whether or not this field has any errors.
     *
     * @return true if this field has any errors, false otherwise.
     */
    public boolean hasAnyErrors() {
        return !errors.isEmpty();
    }
}
