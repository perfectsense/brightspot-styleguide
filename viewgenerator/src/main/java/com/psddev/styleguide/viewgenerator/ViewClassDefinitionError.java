package com.psddev.styleguide.viewgenerator;

/**
 * An error with a view class definition or one of its field level definitions
 * that would prevent a valid Java class from being generated from it.
 */
class ViewClassDefinitionError {

    private ViewClassDefinition classDef;

    private ViewClassFieldDefinition fieldDef;

    private String message;

    /**
     * Creates a new error for the specified {@code fieldDef} and {@code message}.
     *
     * @param fieldDef the field definition causing the error.
     * @param message the error message.
     */
    public ViewClassDefinitionError(ViewClassFieldDefinition fieldDef, String message) {
        this.classDef = fieldDef.getClassDefinition();
        this.fieldDef = fieldDef;
        this.message = message;
    }

    /**
     * Creates a new error for the specified {@code classDef} and {@code message}.
     *
     * @param classDef the class definition causing the error.
     * @param message the error message.
     */
    public ViewClassDefinitionError(ViewClassDefinition classDef, String message) {
        this.classDef = classDef;
        this.message = message;
    }

    /**
     * Gets the class definition causing the error.
     *
     * @return the class definition causing the error.
     */
    public ViewClassDefinition getClassDefinition() {
        return classDef;
    }

    /**
     * Gets the field definition causing the error (if the error is specific to a field).
     *
     * @return the field definition causing the error.
     */
    public ViewClassFieldDefinition getFieldDefinition() {
        return fieldDef;
    }

    /**
     * Gets the error message.
     *
     * @return the error message.
     */
    public String getMessage() {
        return message;
    }
}
