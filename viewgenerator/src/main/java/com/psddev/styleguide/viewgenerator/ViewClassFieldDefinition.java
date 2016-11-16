package com.psddev.styleguide.viewgenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.util.StringUtils;

class ViewClassFieldDefinition implements ViewClassFieldType {

    private ViewClassDefinition viewClassDef;

    private String fieldName;

    private Set<Map.Entry<JsonKey, JsonValue>> fieldKeyValues;

    // The type of the field
    private Class<? extends JsonValue> effectiveType;

    private boolean validated = false;
    private List<ViewClassDefinitionError> errors = new ArrayList<>();

    public ViewClassFieldDefinition(ViewClassDefinition viewClassDef,
                                    String fieldName,
                                    Set<Map.Entry<JsonKey, JsonValue>> fieldKeyValues) {

        this.viewClassDef = viewClassDef;
        this.fieldName = fieldName;
        this.fieldKeyValues = fieldKeyValues;
    }

    public ViewClassDefinition getClassDefinition() {
        return viewClassDef;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Set<Map.Entry<JsonKey, JsonValue>> getFieldKeyValues() {
        return fieldKeyValues;
    }

    public Set<String> getNotes() {
        return fieldKeyValues.stream()
                .map(entry -> entry.getKey().getNotes())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public void validate() {

        if (validated) {
            return;
        }

        // validate the field name
        validateFieldName();

        // validate the value types
        effectiveType = validateValueTypes(fieldKeyValues.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toCollection(ArrayList::new)));

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
    private Class<? extends JsonValue> validateValueTypes(Collection<JsonValue> values) {

        // The only time values will be empty is if we are in a recursive call on a List.
        if (values.isEmpty()) {
            addError("List cannot be empty, they must have at least one value.");
        }

        Set<Class<? extends JsonValue>> valueTypes = values.stream()
                .map(JsonValue::getClass)
                .collect(Collectors.toCollection(HashSet::new));

        // ignore nulls
        valueTypes.remove(JsonNull.class);

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
                validateValueTypes(values.stream()
                        // ignore nulls
                        .filter(value -> !(value instanceof JsonNull))
                        // the rest should be Lists
                        .map(value -> (JsonList) value)
                        // get the values in list
                        .map(JsonList::getValues)
                        // flatten it out
                        .flatMap(Collection::stream)
                        // add them to a set
                        .collect(Collectors.toList()));
            }

            return effectiveValueType;

        } else if (valueTypes.size() > 1) {
            addError("A field can only have a single value type but has "
                    + valueTypes.stream()
                            .map(Class::getSimpleName)
                            .map(s -> StringUtils.removeStart(s, "Json"))
                            .collect(Collectors.joining(" and "))
                    + " instead!");
        }

        return null;
    }

    public Set<ViewClassFieldType> getFieldValueTypes() {
        validate();
        return getFieldValueTypes(fieldKeyValues.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    private Set<ViewClassFieldType> getFieldValueTypes(Collection<JsonValue> values) {

        Set<Class<? extends JsonValue>> valueTypes = values.stream()
                // filter out nulls
                .filter(value -> !(value instanceof JsonNull))
                // get the class
                .map(JsonValue::getClass)
                // add it to the set
                .collect(Collectors.toCollection(HashSet::new));

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
                        .collect(Collectors.toList()));

            } else if (effectiveValueType == JsonViewMap.class) {

                Set<ViewClassFieldType> fieldValueTypes = values.stream()
                        .filter(value -> (value instanceof JsonViewMap))
                        .map(value -> (JsonViewMap) value)
                        .map(JsonViewMap::getViewKey)
                        .collect(Collectors.toSet());

                // if there's a delegate view key, then that becomes the effective type, aka Object.
                if (fieldValueTypes.contains(DelegateViewKey.INSTANCE)) {
                    return Collections.singleton(DelegateViewKey.INSTANCE);
                } else {
                    return fieldValueTypes;
                }

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

    protected ViewClassFieldType getEffectiveValueType() {

        boolean isStrictlyTyped = getClassDefinition().getContext().isGenerateStrictTypes();

        Set<ViewClassFieldType> fieldValueTypes = getFieldValueTypes();

        if (fieldValueTypes.size() == 1) {
            return isStrictlyTyped ? fieldValueTypes.iterator().next() : ViewClassFieldNativeJavaType.OBJECT;

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

    public Class<? extends JsonValue> getEffectiveType() {
        validate();
        return effectiveType;
    }

    @Override
    public String getFullyQualifiedClassName() {
        return getClassDefinition().getFullyQualifiedClassName() + ViewClassStringUtils.toJavaClassCase(fieldName) + "Field";
    }

    private void addError(String message) {
        errors.add(new ViewClassDefinitionError(this, message));
    }

    public List<ViewClassDefinitionError> getErrors() {
        return errors;
    }

    public boolean hasAnyErrors() {
        return !errors.isEmpty();
    }
}
