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

import com.psddev.dari.util.ObjectUtils;

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

    // True if this field's value types are a mix of String and Views
    private boolean hasMixedValueTypes;

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

    //
    // Validates that the values have a consistent type, and returns it. If
    // there is more than one, an error is added to this field definition and
    // null is returned.
    //
    // There are some exceptions to this rule:
    //
    // 1. Having a mix of Strings and Views is allowed, and just considered to be a View.
    // 2. Views are always considered to be Lists, and thus a mix of Views and Lists are also allowed.
    //
    private Class<? extends JsonValue> getEffectiveType(Collection<JsonValue> values, boolean validate) {

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

        // treat it as a list and validate its items.
        if (valueTypes.contains(JsonViewMap.class) || valueTypes.contains(JsonList.class)) {

            // if there are Strings, then it's considered mixed...
            if (valueTypes.contains(JsonString.class)) {

                hasMixedValueTypes = true;

                // but if there are not also Views, then it's an error.
                if (!valueTypes.contains(JsonViewMap.class)) {
                    addMultipleEffectiveTypesErrorConditionally(valueTypes, validate);
                    return null;
                }
            }

            // temp set used to test if there is some combination of only list, string, and view value types.
            Set<Class<? extends JsonValue>> tempValueTypes = new HashSet<>(valueTypes);
            tempValueTypes.removeAll(Arrays.asList(JsonList.class, JsonViewMap.class, JsonString.class));

            // If there is anything other than List, String or View, then it's also an error.
            if (!tempValueTypes.isEmpty()) {
                addMultipleEffectiveTypesErrorConditionally(valueTypes, validate);
                return null;
            }

            // now conditionally validate each list item if there are any lists
            if (validate && valueTypes.contains(JsonList.class)) {

                // Recurse on list item values, since they have to all be the same as well.
                // And we don't have to worry about List of Lists since that is caught during the JSON parse phase.
                getEffectiveListItemType(values.stream()
                        // ignore nulls
                        .filter(value -> !(value instanceof JsonNull))
                        // convert each value to a list
                        .map(value -> value instanceof JsonList ? ((JsonList) value).getValues() : Collections.singleton(value))
                        // flatten them out
                        .flatMap(Collection::stream)
                        // add them to a set
                        .collect(Collectors.toList()), true);
            }

            return JsonList.class;

        } else if (valueTypes.size() == 1) {
            return valueTypes.iterator().next();

        } else if (valueTypes.size() > 1) {
            addMultipleEffectiveTypesErrorConditionally(valueTypes, validate);
        }

        return null;
    }

    //
    // Validates that the list item values have a consistent type, and returns
    // it. If there is more than one, an error is added to this field definition
    // and null is returned
    //
    private Class<? extends JsonValue> getEffectiveListItemType(Collection<JsonValue> values, boolean validate) {

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

        Class<? extends JsonValue> effectiveListItemValueType;

        if (valueTypes.size() == 1
                || (valueTypes.size() == 2
                && valueTypes.containsAll(Arrays.asList(JsonViewMap.class, JsonString.class)))) {

            if (valueTypes.size() == 2) {
                // We allow Strings and Objects to co-exist and just treat them as if it is an Object.
                effectiveListItemValueType = JsonViewMap.class;
                hasMixedValueTypes = true;

            } else {
                effectiveListItemValueType = valueTypes.iterator().next();
            }

            return effectiveListItemValueType;

        } else if (valueTypes.size() > 1) {
            addMultipleEffectiveTypesErrorConditionally(valueTypes, validate);
        }

        return null;
    }

    private void addMultipleEffectiveTypesErrorConditionally(Set<Class<? extends JsonValue>> valueTypes, boolean validate) {
        addErrorConditionally("A field can only have a single value type but has "
                + valueTypes.stream()
                        .map(JsonValueType::forClass)
                        .map(JsonValueType::getLabel)
                        .collect(Collectors.joining(" and "))
                + " instead!", validate);
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

        isDelegate = false;
        isAbstract = false;

        Class<? extends JsonValue> effectiveItemType;
        Collection<JsonValue> itemValues;

        Class<? extends JsonValue> effectiveType = getEffectiveType(values, false);

        if (effectiveType == JsonList.class) {

            itemValues = values.stream()
                    // ignore nulls
                    .filter(value -> !(value instanceof JsonNull))
                    // convert each value to a list
                    .map(value -> value instanceof JsonList ? ((JsonList) value).getValues() : Collections.singleton(value))
                    // flatten them out
                    .flatMap(Collection::stream)
                    // add them to a set
                    .collect(Collectors.toList());

            effectiveItemType = getEffectiveListItemType(itemValues, false);

        } else {
            itemValues = values;
            effectiveItemType = effectiveType;
        }

        if (effectiveItemType == JsonViewMap.class) {

            Set<ViewClassFieldType> fieldValueTypes = new TreeSet<>((o1, o2) -> ObjectUtils.compare(
                    o1.getFullyQualifiedClassName(),
                    o2.getFullyQualifiedClassName(),
                    true));

            itemValues.stream()
                    .filter(value -> (value instanceof JsonViewMap))
                    .map(value -> (JsonViewMap) value)
                    .map(JsonViewMap::getViewKey)
                    .collect(Collectors.toCollection(() -> fieldValueTypes));

            Set<JsonDelegateMap> delegateMaps = itemValues.stream()
                    .filter(value -> (value instanceof JsonDelegateMap))
                    .map(value -> (JsonDelegateMap) value)
                    .collect(Collectors.toSet());

            Set<JsonAbstractMap> abstractMaps = itemValues.stream()
                    .filter(value -> (value instanceof JsonAbstractMap))
                    .map(value -> (JsonAbstractMap) value)
                    .collect(Collectors.toSet());

            // for each delegate key search for all view class definitions
            // that have a JSON view map whose wrapper JSON file matches
            // the file that the delegate map is declared in.
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

        } else if (effectiveItemType == JsonBoolean.class) {
            return Collections.singleton(ViewClassFieldNativeJavaType.BOOLEAN);

        } else if (effectiveItemType == JsonNumber.class) {
            return Collections.singleton(ViewClassFieldNativeJavaType.NUMBER);

        } else if (effectiveItemType == JsonString.class) {
            return Collections.singleton(ViewClassFieldNativeJavaType.CHAR_SEQUENCE);

        } else if (effectiveItemType == JsonMap.class) {
            return Collections.singleton(ViewClassFieldNativeJavaType.MAP);

        } else {
            return Collections.emptySet();
        }
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

    public boolean hasMixedValueTypes() {
        return hasMixedValueTypes;
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
